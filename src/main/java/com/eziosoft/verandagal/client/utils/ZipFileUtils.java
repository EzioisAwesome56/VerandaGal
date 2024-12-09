package com.eziosoft.verandagal.client.utils;

import com.eziosoft.verandagal.database.objects.ImagePack;
import com.eziosoft.verandagal.utils.ConfigUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.client.VerandaClient;
import com.eziosoft.verandagal.client.json.ImageEntry;
import com.eziosoft.verandagal.client.json.PackMetaFile;
import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.ThumbnailStore;
import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.database.objects.Thumbnail;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.utils.ConfigFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipFileUtils {

    /**
     * does what it says on the tin. makes a zip file of an ImagePack
     * @param packfolder the folder the image pack is in
     */
    public static void CreateImagePackZip(File packfolder) {
        try {
            // check to see if file exists
            File packmetafile = new File(packfolder, "pack.json");
            if (!packmetafile.exists()) {
                VerandaClient.log.error("Error: no pack.json found in folder!");
                return;
            }
            // otherwise if its there, we can assume everything else is ready to go
            VerandaClient.log.info("Now preparing to zip pack file");
            // loosely from https://stackoverflow.com/a/1091817
            File packzip = new File(packfolder, "pack.zip");
            FileOutputStream fout = new FileOutputStream(packzip);
            ZipOutputStream zip = new ZipOutputStream(fout);
            // ok, we have our zip output stream now
            // make a new entry for pack.json
            ZipEntry entry = new ZipEntry("pack.json");
            zip.putNextEntry(entry);
            // we need to load the pack.json file into a byte[] now
            byte[] data = FileUtils.readFileToByteArray(packmetafile);
            // write to the zip
            zip.write(data, 0, data.length);
            zip.closeEntry();
            VerandaClient.log.info("added pack.json to zip file");
            // setup some prep for later
            File images = new File(packfolder, "images");
            File thumbs = new File(packfolder, "thumbs");
            // we also need to load pack.json's object
            // so let us do that really quickly
            PackMetaFile packmeta = Main.gson_pretty.fromJson(FileUtils.readFileToString(packmetafile, StandardCharsets.UTF_8), PackMetaFile.class);
            // now we have to loop thru every image and write it to the pack folder
            for (ImageEntry imgent : packmeta.getImages()){
                // check to see if the file exists
                File tempfile = new File(images, imgent.getFilename());
                if (!tempfile.exists()){
                    VerandaClient.log.error("ERROR: image file listed in pack does not exist!");
                    VerandaClient.log.error("Your pack is probably broken or corrupted!");
                    throw new RuntimeException("Corrupted Uncompressed Image Pack");
                }
                // if it DOES exist, make a new zip entry for it
                entry = new ZipEntry(imgent.getFilename());
                zip.putNextEntry(entry);
                // we need to then read our file into a byte[]
                byte[] tempbyte = FileUtils.readFileToByteArray(tempfile);
                // write it to the zip file
                zip.write(tempbyte, 0, tempbyte.length);
                // close the entry
                zip.closeEntry();
                // next, we need to see if the thumbnail exists on the disk
                tempfile = new File(thumbs, FilenameUtils.getBaseName(imgent.getFilename()) + ".jpg");
                if (!tempfile.exists()){
                    VerandaClient.log.error("ERROR: thumbnail for image file listed in pack.json does not exist!");
                    VerandaClient.log.error("Your pack is probably corrupted or broken!");
                    throw new RuntimeException("Corrupted Uncompressed Image Pack");
                }
                // if it does exist, make a new zip entry for it
                entry = new ZipEntry(FilenameUtils.getBaseName(imgent.getFilename()) + ".thumb");
                zip.putNextEntry(entry);
                // read our thumbnail into memory
                tempbyte = FileUtils.readFileToByteArray(tempfile);
                // write to zip file
                zip.write(tempbyte, 0, tempbyte.length);
                // close the entry
                zip.closeEntry();
                VerandaClient.log.info("Added {} to the zip file", imgent.getFilename());
            }
            // once we're out of the loop, we have no more files to deal with
            // do some cleanup
            zip.close();
            fout.close();
            // and we're done
            VerandaClient.log.info("pack.zip file created without problems");
        } catch (IOException e){
            VerandaClient.log.error("Something went wrong while compressing pack zip", e);
            return;
        }
    }

    /**
     * imports an Image Pack zip into your instance of the gallery
     * you should probably make sure you have erun --server atleast once before hand tho
     * @param packzip
     */
    public static void importPackZip(File packzip){
        // check to see if config.json exists
        File conffile = ConfigUtils.checkForConfig();
        // load the config file
        String readfile;
        try {
            readfile = FileUtils.readFileToString(conffile, StandardCharsets.UTF_8);
        } catch (IOException e){
            Main.LOGGER.error("Error trying to read config file", e);
            return;
        }
        // convert to java object
        ConfigFile config = Main.gson_pretty.fromJson(readfile, ConfigFile.class);
        // get the main database rolling
        Main.LOGGER.info("Now connecting to databases");
        MainDatabase maindb = new MainDatabase(config);
        // get the thumbnail store going too
        ThumbnailStore thumbnails = new ThumbnailStore(config);
        // next, open the zip file
        ZipFile zip;
        try {
            zip = new ZipFile(packzip);
            // some enumeration shit
            // from https://stackoverflow.com/a/15667326
            Enumeration<? extends ZipEntry> entries = zip.entries();
            // the first element should be pack.json
            ZipEntry entry = entries.nextElement();
            if (!entry.getName().equals("pack.json")){
                Main.LOGGER.error("ERROR: First file in zip is not pack.json");
                Main.LOGGER.error("This pack zip is either invalid, corrupted, or incorrectly made!");
                return;
            }
            // its now time to read pack.json out of the file
            InputStream instream = zip.getInputStream(entry);
            Reader read = new InputStreamReader(instream, StandardCharsets.UTF_8);
            // gson can work with a reader, so this is good enough for us
            PackMetaFile packmeta = Main.gson_pretty.fromJson(read, PackMetaFile.class);
            // clean up this shit
            read.close();
            instream.close();
            // and now we should have the pack meta file
            Main.LOGGER.info("Loaded pack from zip: {}", packmeta.getPackname());
            Main.LOGGER.info("Total number of images to import: {}", packmeta.getImages().size());
            // setup the file for the images folder
            File imgfolder = new File(config.getImageDir());
            // then, we have to copy the images to the correct pack folder
            File packimgfolder = new File(imgfolder, packmeta.getPackfoldername());
            // make the folder
            packimgfolder.mkdir();
            // we will now be importing the pack into the database
            // make a new pack object for that task
            ImagePack temppack = new ImagePack();
            // fill in all the objects inside of it
            temppack.setName(packmeta.getPackname());
            temppack.setFsdir(packmeta.getPackfoldername());
            temppack.setDescription(packmeta.getPackdescription());
            Date whattimeisit = new Date();
            temppack.setUploadDate(whattimeisit.toString());
            temppack.setTotalImages(packmeta.getImages().size());
            // store the pack into the DB
            maindb.SaveObject(temppack);
            // get the ID from this pack now that it has saved
            long packid = temppack.getId();
            // init a count variable
            int count = 0;
            while (entries.hasMoreElements()){
                // current entry we are reading
                entry = entries.nextElement();
                Main.LOGGER.info("entry name is {}", entry.getName());
                // check to make sure it lines up with what is in the meta
                if (!entry.getName().equals(packmeta.getImages().get(count).getFilename())){
                    Main.LOGGER.error("ERROR: file does not match the file order in pack.json");
                    Main.LOGGER.error("Your pack is probably malformed");
                    return;
                }
                // if it does match, then we're golden
                // write the file to the images folder
                File temp = new File(packimgfolder, entry.getName());
                InputStream tempstream = zip.getInputStream(entry);
                FileUtils.copyInputStreamToFile(tempstream, temp);
                tempstream.close();
                // next, we need to insert the image into our main DB
                // create the object to insert
                Image dbent = new Image();
                dbent.setUploaderComments(packmeta.getImages().get(count).getComments());
                dbent.setSourceurl(packmeta.getImages().get(count).getOriginalUrl());
                Date date = new Date();
                dbent.setUploaddate(date.toString());
                dbent.setRating(packmeta.getImages().get(count).getRating());
                // we already got the pack id for this
                // so we can just use it here
                dbent.setPackid(packid);
                dbent.setImageres(packmeta.getImages().get(count).getResolution());
                dbent.setArtistid(packmeta.getImages().get(count).getArtistid());
                dbent.setFilename(packmeta.getImages().get(count).getFilename());
                // BREAKING: packs will probably have to be remade for this
                // not that any have been since it hasnt been released yet
                dbent.setAI(packmeta.getImages().get(count).isAiimage());
                // insert that into the db
                maindb.SaveObject(dbent);
                // then, we can get an id from that
                long id = dbent.getId();
                // we need to go to the next element to save the thumbnail
                entry = entries.nextElement();
                // ok, now we have the thumbnail content
                // make a new thumbnail object
                Thumbnail tempnail = new Thumbnail();
                // now we need to get image data
                tempstream = zip.getInputStream(entry);
                byte[] data = IOUtils.toByteArray(tempstream);
                tempstream.close();
                tempnail.setImagedata(data);
                // save that to the thumbnail DB
                thumbnails.SaveThumbnail(tempnail);
                long thumbid = tempnail.getId();
                // see if they match
                if (thumbid != id){
                    Main.LOGGER.error("ERROR: image entry and thumbnail ids do not match!");
                    Main.LOGGER.error("Thumbnail: {} ImageEntry: {}", thumbid, id);
                    Main.LOGGER.error("Something has gone very wrong!");
                    return;
                }
                count++;
            }
            // once we're done with the loop, we can close the zip file
            zip.close();
            Main.LOGGER.info("Imported {} into new pack {}", count, packmeta.getPackname());
        } catch (Exception e){
            Main.LOGGER.error("Something went wrong trying to import pack zip", e);
            thumbnails.close();
            return;
        }
    }
}