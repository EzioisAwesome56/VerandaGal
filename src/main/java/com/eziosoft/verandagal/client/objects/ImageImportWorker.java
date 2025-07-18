package com.eziosoft.verandagal.client.objects;

import com.eziosoft.verandagal.client.utils.ImageProcessor;
import com.eziosoft.verandagal.client.utils.ImageUtils;
import com.eziosoft.verandagal.database.ThumbnailStore;
import com.eziosoft.verandagal.database.objects.Thumbnail;
import com.eziosoft.verandagal.utils.ConfigFile;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * this is an instance of a thread that will be used for generating
 * webp previews and thumbnails of any imported images
 * this will greatly speed up bulk importing, in theory anyway
 */
public class ImageImportWorker implements Runnable{
    // we need a couple of things in here for our own use later
    private final CountDownLatch latch;
    private final List<ImageImportJob> jobs;
    private final ThumbnailStore thumbnails;
    private final ConfigFile config;
    private final Logger log = LogManager.getLogger("Image Worker Thread");

    public ImageImportWorker(CountDownLatch latch, List<ImageImportJob> jobs, ThumbnailStore store, ConfigFile file){
        this.latch = latch;
        this.jobs = jobs;
        this.config = file;
        this.thumbnails = store;
    }

    @Override
    public void run() {
        // start up the thread
        this.log.info("New image worker thread has started");
        // setup some basics
        File previewdir = new File(this.config.getImagePreviewDir());
        // then, the main loop for processing
        // the code for this was originally in the main bulkimageimport class
        int count = 0;
        for (ImageImportJob job : this.jobs){
            // we have to reread in the image, since it was not passed in eariler
            // TODO: this might memory leak, but it should be fine i think?
            BufferedImage temp;
            try {
                temp = ImageIO.read(job.getTargetfile());
            } catch (IOException e){
                // what happened?
                this.log.error("Something broke while trying to read in file {}, skipping", job.getFilename());
                this.log.error(e);
                continue;
            }
            // create a preview for it
            byte[] previewbytes = null;
            byte[] thumbnailbytes = null;
            try {
                // webp previews may be disabled, dont waste diskspace if they are
                if (!this.config.isDontUsePreviews() || ImageUtils.checkIfFormatRequiresPreview(job.getFilename())){
                    previewbytes = ImageUtils.generateImagePreview(temp);
                }
                thumbnailbytes = ImageProcessor.generateThumbnail(temp);
            } catch (IOException e){
                this.log.error("Something went wrong during thumbnail/preview gen, this may break things!");
                this.log.error(e);
            }
            // we can cheat by checking to make sure the array isnt null before writing
            if (previewbytes != null) {
                // create a new file, then write the webp to it
                File preview_file = new File(previewdir, job.getId() + ".webp");
                try {
                    FileUtils.writeByteArrayToFile(preview_file, previewbytes);
                } catch (IOException e) {
                    this.log.error("Error while trying to write preview webp file");
                    this.log.error(e);
                    this.log.error("Affected file is: {}", job.getFilename());
                }
            } else {
                // this is something i have encountered in further testing, so we will write an error about this
                if (!this.config.isDontUsePreviews() || ImageUtils.checkIfFormatRequiresPreview(job.getFilename())){
                    this.log.error("Image that needs preview failed to generate any bytes");
                    this.log.error("Effected file: {}", job.getFilename());
                }
            }
            // now we have to write the thumbnail into the database
            Thumbnail thumbnailent = new Thumbnail();
            // make sure its not null, if it is we have a problem
            if (thumbnailbytes == null){
                this.log.error("Thumbnail bytes are null, filling with dummy bytes. this will break shit!");
                thumbnailbytes = new byte[]{(byte) 255, (byte) 255};
            }
            thumbnailent.setImagedata(thumbnailbytes);
            // since we are doing this threaded and it may insert out of order, set an ID manually
            thumbnailent.setId(job.getId());
            // write to the database
            this.thumbnails.MergeThumbnail(thumbnailent);
            // TODO: i dont think this sanity check applies anymore? look into this
            if (thumbnailent.getId() != job.getId()){
                this.log.warn("Thumbnail db id does not match image id in database. something may be wrong");
            }
            // now we just need to move the file
            try {
                Files.move(job.getTargetfile().toPath(), new File(job.getTargetPackDir(), job.getFilename()).toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e){
                this.log.error("Something went wrong while trying to move a file!");
                this.log.error(e);
            }
            // done with this cycle
            this.log.info("Processed image {} of {}", count, this.jobs.size());
            count++;
        }
        // once we're done, annouce that fact and then hit up the latch
        this.log.info("Thread has completed!");
        this.latch.countDown();
    }
}
