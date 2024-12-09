package com.eziosoft.verandagal.client.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eziosoft.verandagal.client.dialogs.guiImageMetaEditor;
import com.eziosoft.verandagal.client.dialogs.guiImageProcessorSelector;
import com.eziosoft.verandagal.client.json.ImageEntry;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImageProcessor {

    // such luxaries to have its own logger!
    public static Logger log = LogManager.getLogger("Image Processor");

    public static byte[] generateThumbnail(BufferedImage buf) throws IOException{
        // get image of buffered image
        Image thumbnail = buf.getScaledInstance(128, 128, BufferedImage.SCALE_FAST);
        // do the conversion
        BufferedImage temp = new BufferedImage(128, 128, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D tempgfx = temp.createGraphics();
        tempgfx.drawImage(thumbnail, 0, 0, null);
        tempgfx.dispose();
        // write this data to an output stream
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(temp, "jpg", stream);
        // get byte[] from that
        byte[] data = stream.toByteArray();
        // clean up
        stream.close();
        return data;
    }

    /**
     * this will handle the dialog for queueing images to process
     * and then processing them, and writing the output to the folder
     * @param source
     * @return
     */
    public static ArrayList<ImageEntry> doImageAddition(ArrayList<ImageEntry> source, File outdir, ImportableArtistsFile artists){
        // so we have our sources. cool i guess.
        // we have to do a bunch of shit first
        // namely, we have to get the list of images to process
        guiImageProcessorSelector selector = new guiImageProcessorSelector();
        ArrayList<String> images = selector.openDialog();
        /*
        now comes the stupid af part
        we have to make image entries for each one of these, then also process them. fun times
         */
        // make a hashmap to store these
        HashMap<String, ImageEntry> newents = new HashMap<>();
        // here comes the loop
        boolean kill_loop = false;
        for (String file : images){
            // setup the editor
            guiImageMetaEditor editor = new guiImageMetaEditor(file, artists);
            ImageEntry temp = editor.OpenDialog();
            // get the exit code
            int code = editor.getExit_code();
            switch (code){
                case 0 -> System.exit(0);
                case 1 -> newents.put(file, temp);
                case 2 -> {
                    newents.put(file, temp);
                    kill_loop = true;
                }
                case 3 -> {}
                default -> log.debug("Somehow, we have hit the default path in the switch block!");
            }
            // check if we need to kill the loop
            if (kill_loop){
                break;
            }
        }
        // ok, we have escpated the loop.
        // how many images do we have even?
        log.debug("Number of images with metadata is {}", newents.size());
        // also, we need a new array of image entries for yes
        ArrayList<ImageEntry> outputlist = new ArrayList<>();
        // next up, we have to process them all, copy to output dir and also make thumbnails
        // these folders should exit already so we dont need to make them later
        File imagedir = new File(outdir, "images");
        File thumbdir = new File(outdir, "thumbs");
        // loop thru our hashmap of brand new image entries
        log.info("Image processing is now starting...");
        int count = 1;
        for (Map.Entry<String, ImageEntry> entry : newents.entrySet()){
            try {
                // make file of source image
                File src = new File(entry.getKey());
                // get file of output image
                File output = new File(imagedir, FilenameUtils.getName(entry.getKey()));
                // copy it to the output image folder
                FileUtils.copyFile(src, output);
                // make a file of the thumbnail output
                File thumbout = new File(thumbdir, FilenameUtils.getBaseName(entry.getKey()) + ".jpg");
                // read it in with imageio
                BufferedImage ourimage = ImageIO.read(src);
                // generate and save thumbnail to disk
                FileUtils.writeByteArrayToFile(thumbout, generateThumbnail(ourimage));
                // append our image to the output list
                outputlist.add(entry.getValue());
                // visual feedback
                log.info("Processed image {} of {}", count, newents.size());
                count++;
            } catch (IOException e){
                log.error("Something went wrong while trying to process an image", e);
            }
        }
        // finally, append everything from the source list to our new output list
        outputlist.addAll(source);
        // return our new list
        return outputlist;
    }
}
