package com.eziosoft.verandagal.client.utils;

import com.eziosoft.verandagal.client.json.ArtistEntry;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;
import com.eziosoft.verandagal.client.objects.BulkImageObject;
import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.ThumbnailStore;
import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.database.objects.ImagePack;
import com.eziosoft.verandagal.database.objects.Thumbnail;
import com.eziosoft.verandagal.server.utils.ServerUtils;
import com.eziosoft.verandagal.utils.ConfigFile;
import com.eziosoft.verandagal.utils.ConfigUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class BulkImageImport {

    // list of all supported filetypes by the bulk importer
    private static final String[] supportedTypes = new String[]{
            "png",
            "jpg",
            "jpeg",
            "psd",
            "webp"
    };

    // create a new logger for this class
    public static Logger log = LogManager.getLogger("Bulk Image Importer");

    /**
     * this is the main class you call to perform a bulk image import
     * @param srcfolder
     */
    public static void doBulkImport(File srcfolder){
        // setup a scanner, since closing scanners apparently breaks everything
        Scanner scan = new Scanner(System.in);
        // disable imageio caching right off the bat
        ImageIO.setUseCache(false);
        // check if folder exists
        if (!(srcfolder.exists() && srcfolder.isDirectory())){
            log.error("Error: provided folder does not exist!");
            return;
        }
        // get all files in the directory
        File[] allfiles = srcfolder.listFiles();
        // form a new list to store files we detect as supported
        ArrayList<File> supportedfiles = new ArrayList<>();
        for (File f : allfiles){
            // get file extension
            String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
            // check if we support it
            boolean support = false;
            for (String ext : supportedTypes){
                if (extension.contains(ext)){
                    // supported file type, set flag and yeet
                    support = true;
                    break;
                }
            }
            // did we fine a supported filetype?
            if (!support){
                log.warn("Unsupported file detected: {}", f.getName());
                continue;
            }
            // otherwise, add it to our list
            supportedfiles.add(f);
        }
        // done sorting shit
        log.info("File detection done; found {} supported image files", supportedfiles.size());
        // get a copy of the config file
        ConfigFile config;
        try {
            config = ConfigUtils.getConfigFile();
        } catch (IOException e){
            // an error was already reported for this, so we can just fail
            log.error("Failure while trying to get config file");
            return;
        }
        // spin up the main database
        log.info("Connecting to main database");
        MainDatabase maindb = new MainDatabase(config);
        // load the artists file
        ImportableArtistsFile artists;
        try {
            artists = ClientUtils.exportArtists(maindb);
        } catch (NullPointerException e){
            // we dont actually care if the database is empty, so we will just make a new importable artists file
            // that is empty
            artists = new ImportableArtistsFile();
        }
        // prepare our basic image metadata
        ArrayList<BulkImageObject> bulkobjs = prepareMetadata(supportedfiles, scan, artists);
        // import our artists file for changes
        ClientUtils.importArtistsWithDatabaseConnection(artists, maindb);
        // get a basic image pack
        ImagePack pack = createImagePack(bulkobjs.size(), scan);
        log.info("Connecting to thumbnail database");
        ThumbnailStore thumbnail = new ThumbnailStore(config);
        // create the directory for storing images
        File baseimgdir = new File(config.getImageDir());
        File packimgdir = new File(baseimgdir, pack.getFsdir());
        packimgdir.mkdir();
        // also open the directory for storing image previews
        File preview = new File(config.getImagePreviewDir());
        // save our pack to the database
        maindb.SaveObject(pack);
        // now we can get our packid
        long packid = pack.getId();
        // get the date also
        Date date = new Date();
        // now we can start importing the images
        log.info("Now importing images into database...");
        for (BulkImageObject bulk : bulkobjs){
            // make a new database image object
            Image dbent = new Image();
            // fill in what we can about the images
            dbent.setUploaddate(date.toString());
            dbent.setRating(bulk.getRating());
            dbent.setPackid(packid);
            // from bulk image importer
            dbent.setUploaderComments("Uploaded using Bulk Image Importer<br>some information may be missing or inaccurate");
            dbent.setSourceurl(parseFilenameforOriginalURL(bulk.getFilename(), bulk.getSitesource()));
            // other provided imformation
            dbent.setAI(bulk.isAi());
            dbent.setFilename(bulk.getFilename());
            dbent.setArtistid(bulk.getArtistid());
            // we have to load the image eventually, so we will do so now
            File ogsource = new File(srcfolder, bulk.getFilename());
            // verify it exists
            if (!ogsource.exists()){
                log.error("ERROR: somehow, the file {} has gone missing! skipping, this will probably break everything!");
                continue;
            }
            // read it with imageio
            BufferedImage temp;
            try {
                temp = ImageIO.read(ogsource);
            } catch (IOException e){
                // what happened?
                log.error("Something broke while trying to read in file {}, skipping", bulk.getFilename());
                log.error(e);
                continue;
            }
            // set the resolution
            dbent.setImageres(temp.getWidth() + "x" + temp.getHeight());
            // then, we can import this image into the database
            maindb.SaveObject(dbent);
            // now we have an imageid!
            // create a preview for it
            byte[] previewbytes = null;
            byte[] thumbnailbytes = null;
            try {
                // webp previews may be disabled, dont waste diskspace if they are
                if (!config.isDontUsePreviews() || ImageUtils.checkIfFormatRequiresPreview(bulk.getFilename())){
                    previewbytes = ImageUtils.generateImagePreview(temp);
                }
                thumbnailbytes = ImageProcessor.generateThumbnail(temp);
            } catch (IOException e){
                log.error("Something went wrong during thumbnail/preview gen, this may break things!");
                log.error(e);
            }
            // we can cheat by checking to make sure the array isnt null before writing
            if (previewbytes != null) {
                // create a new file, then write the webp to it
                File preview_file = new File(preview, dbent.getId() + ".webp");
                try {
                    FileUtils.writeByteArrayToFile(preview_file, previewbytes);
                } catch (IOException e) {
                    log.error("Error while trying to write preview webp file");
                    log.error(e);
                }
            }
            // now we have to write the thumbnail into the database
            Thumbnail thumbnailent = new Thumbnail();
            // make sure its not null, if it is we have a problem
            if (thumbnailbytes == null){
                log.error("Thumbnail bytes are null, filling with dummy bytes. this will break shit!");
                thumbnailbytes = new byte[]{(byte) 255, (byte) 255};
            }
            thumbnailent.setImagedata(thumbnailbytes);
            // write this to the thumbnail database
            thumbnail.SaveThumbnail(thumbnailent);
            if (thumbnailent.getId() != dbent.getId()){
                log.warn("Thumbnail db id does not match image id in database. something may be wrong");
            }
            // now we just need to move the file
            try {
                Files.move(ogsource.toPath(), new File(packimgdir, dbent.getFilename()).toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e){
                log.error("Something went wrong while trying to move a file!");
                log.error(e);
            }
            log.info("Imported image {}", dbent.getFilename());
        }
        log.info("Done importing images. enjoy!");
        // clean up some shit
        thumbnail.close();
        maindb.close();
        scan.close();
    }

    /**
     * some image websites will save files with special names
     * we can use this to determine what site its from
     * @param filename file name to parse
     * @return int to dictate what site its from
     * 0 - unknown source
     * 1 - pixiv
     * 2 - deviantart
     */
    private static int parseFilename(String filename){
        // set to all lowercase for sanity
        // also remove the file extension
        String sane = FilenameUtils.removeExtension(filename.toLowerCase());
        if (sane.contains("_p")){
            boolean fail = false;
            // file is maybe from pixiv
            // pixiv usually ends filenames with _px, so try to see if its an int
            String[] split = sane.split("_p");
            // check if value is int
            int test;
            try {
                test = Integer.parseInt(split[1]);
            } catch (NumberFormatException e){
                // set the fail flag
                fail = true;
            }
            if (!fail){
                log.info("File {} is likely from pixiv based on filename", filename);
                return 1;
            }
        }
        if (sane.contains("_by_")){
            // file is probably from deviantart
            // DA puts a _by_<artist> at the end of downloads from their site
            // no further checking is really required for this
            log.info("File {} is likely from deviantart based on filename", filename);
            return 2;
        }
        // default case; no site was found
        return 0;
    }

    /**
     * before we can actually perform the bulk import, the files must have some very basic metadata applied to them
     * this method handles that
     * @param input input list of supported files
     * @return list of objects with basic metadata
     */
    private static ArrayList<BulkImageObject> prepareMetadata(List<File> input, Scanner scan, ImportableArtistsFile artists){
        // create our new list
        ArrayList<BulkImageObject> bulkobjs = new ArrayList<>();
        // options
        boolean promptforrating = false;
        boolean promptforai = false;
        boolean defaultai = false;
        int defaultrate = 2;
        boolean flag = false;
        // prompt for options, in order
        while (!flag){
            System.out.print("Do you want to be prompted for rating for each image? [y/n]: ");
            String dank = scan.nextLine().toLowerCase();
            if (!(dank.equals("y") || dank.equals("n"))){
                System.out.println("Invalid option, try again");
            } else {
                if (dank.equals("y")){
                    System.out.println("Ok, you will be prompted for each image");
                    promptforrating = true;
                    flag = true;
                } else {
                    boolean flag2 = false;
                    while (!flag2){
                        System.out.print("Please enter default rating to assign to all images [0-3]: ");
                        int hi = scan.nextInt();
                        if (hi >= 0 && hi < 4){
                            // store it
                            defaultrate = hi;
                            // break out of the sub loop
                            flag2 = true;
                            System.out.println("Ok, all images will be assigned the rating: " + ServerUtils.getRatingText(defaultrate));
                        } else {
                            System.out.println("Entered rating is out of bounds!");
                        }
                    }
                    // break out of main loop too
                    flag = true;
                }
            }
        }
        // reset our flag
        flag = false;
        // now we need to prompt for the default ai setting
        while (!flag){
            System.out.print("Do you want to be prompted for AI setting for each image? [y/n]: ");
            String dank = scan.nextLine().toLowerCase();
            if (!(dank.equals("y") || dank.equals("n"))) {
                System.out.println("Invalid option, try again");
            } else {
                if (dank.equals("y")){
                    // set our option
                    promptforai = true;
                    flag = true;
                    System.out.println("Ok, you will be prompted for each image");
                } else if (dank.equals("n")){
                    // set a dumb second flag
                    boolean flag2 = false;
                    while (!flag2){
                        System.out.print("Please enter default AI value [1-true 0-false]: ");
                        int hi = scan.nextInt();
                        if (hi >= 0 && hi <= 1){
                            // store our variable
                            defaultai = hi != 0;
                            flag2 = true;
                            System.out.println("Ok, all images will have their ai flag set to: " + defaultai);
                        } else {
                            System.out.println("Entered value is out of bounds!");
                        }
                        // break out of main loop
                        flag = true;
                    }
                }
            }
        }
        // we're done with settings
        log.info("Settings have been applied!");
        log.info("Prompt for rating: {}, default rating value: {}", promptforrating, defaultrate);
        log.info("Prompt for AI: {}, default AI value: {}", promptforai, defaultai);
        // verify we have a bulk artist in the list already
        boolean hasbulk = false;
        long bulkid = -1;
        for (Map.Entry<Long, ArtistEntry> ent : artists.getArtists().entrySet()){
            if (ent.getValue().getName().toLowerCase().equals("bulkimportbot")){
                hasbulk = true;
                bulkid = ent.getKey();
            }
        }
        if (!hasbulk){
            log.info("No bulk artist exists, creating a new one");
            ArtistEntry bulkart = new ArtistEntry();
            bulkart.setName("Bulk Importer Bot 9000");
            bulkart.setNotes("uploaded using the bulk import feature");
            bulkart.setUrls(new String[]{"none"});
            // get our id for the new artist
            bulkid = artists.getArtists().size() + 1;
            // add our new artist
            artists.addArtist(bulkid, bulkart);
        }
        // now to start generating entries
        for (File f : input){
            // get the file name
            String name = f.getName();
            // figure out what website its from
            int site = parseFilename(name);
            // what rating should this image be set as?
            int rate;
            if (!promptforrating){
                rate = defaultrate;
            } else {
                rate = promptForRating(name, scan);
            }
            // is this image made by AI?
            boolean ai;
            if (!promptforai){
                ai = defaultai;
            } else {
                ai = promptForAi(name, scan);
            }
            // figure out if we should set this file as owned by an artist
            long artistid;
            if (site == 2){
                artistid = parseDAFilenameForArtist(name, artists);
            } else {
                // we have no idea, default to bulkid
                artistid = bulkid;
            }
            // create a new object
            BulkImageObject temp = new BulkImageObject(name, site, ai, rate, artistid);
            // add to list
            bulkobjs.add(temp);
        }
        // return the completed array of images
        return bulkobjs;
    }

    /**
     * prompt the user for image rating
     * @param name filename of current image
     * @return entered rating
     */
    private static int promptForRating(String name, Scanner scan){
        int rate = -1;
        boolean flag = false;
        while (!flag){
            System.out.print("Please enter rating for " + name + " [0-3]: ");
            int hi = scan.nextInt();
            if (hi >= 0 && hi < 4){
                // store it
                rate = hi;
                // break out of the sub loop
                flag = true;
                System.out.println("This image will be rated as: " + ServerUtils.getRatingText(rate));
            } else {
                System.out.println("Entered rating is out of bounds!");
            }
        }
        // return our value
        return rate;
    }

    /**
     * prompts the user to enter a value for the AI flag
     * @param name filename that is being prompted for
     * @return true or false
     */
    private static boolean promptForAi(String name, Scanner scan){
        boolean ai = false;
        boolean flag = false;
        while (!flag){
            System.out.print("Please enter AI value for " + name + " [0-false, 1-true]: ");
            int hi = scan.nextInt();
            if (hi >= 0 && hi < 2){
                // set the boolean based on what number it is
                ai = hi != 0;
                // break out of the sub loop
                flag = true;
                System.out.println("This image will have the following set for its AI value: " + ai);
            } else {
                System.out.println("Entered AI value is out of bounds!");
            }
        }
        // return our value
        return ai;
    }

    /**
     * call this function to ask the user a couple of questions, required to fill in pack information
     * @param num_items number of items that will be present in the pack
     * @param scan scanner to handle console
     * @return imagepack object that can be placed into the db later
     */
    private static ImagePack createImagePack(int num_items, Scanner scan){
        // setup all the information we need to collect right now
        String packname;
        String packdec;
        Date whattimeisit = new Date();
        String uploaddate = whattimeisit.toString();
        long total = num_items;
        String fsdir;
        // ask for folder name
        while (true){
            System.out.print("Please enter name for filesystem folder: ");
            fsdir = scan.nextLine();
            if (!fsdir.isEmpty()){
                System.out.println("Folder name set to: " + fsdir);
                break;
            }
        }
        // ask for pack name
        while (true){
            System.out.print("Please enter name for image pack: ");
            packname = scan.nextLine();
            if (!packname.isEmpty()){
                System.out.println("Pack name set to: " + packname);
                break;
            }
        }
        // ask for pack description
        while (true){
            System.out.print("Please enter pack description: ");
            packdec = scan.nextLine();
            if (!packdec.isEmpty()){
                System.out.println("Pack description set to: " + packdec);
                break;
            }
        }
        // create new basic image pack object
        ImagePack pack = new ImagePack();
        pack.setTotalImages(total);
        pack.setDescription(packdec);
        pack.setFsdir(fsdir);
        pack.setName(packname);
        pack.setUploadDate(uploaddate);
        // return that object
        return pack;
    }

    /**
     * Parse filename for Deviantart files to extract artist information
     * those files usually end in the format of _by_<artist>_<nonsense>
     * @param name filename to parse
     * @param artists artists file
     * @return id of artist if found, or created. return bulkid on any errors
     */
    private static long parseDAFilenameForArtist(String name, ImportableArtistsFile artists){
        // start by making the entire filename lowercase
        // remove file extension from artist names
        String sane = FilenameUtils.getBaseName(name.toLowerCase());
        // split via _by_
        String[] firstsplit = sane.split("_by_");
        // split again by _ to remove the extra crap
        String[] oofsplit = firstsplit[1].split("_");
        // we now need a third split, to get rid of trailing -
        // based on https://stackoverflow.com/a/20905080
        int i = oofsplit[0].lastIndexOf("-");
        String thesplit = oofsplit[0].substring(0, i);
        log.debug("Found artist name: {}", thesplit);
        long artid = -1;
        // try and find it
        for (Map.Entry<Long, ArtistEntry> ent : artists.getArtists().entrySet()){
            if (ent.getValue().getName().toLowerCase().equals(thesplit)){
                // it exists, get the value and yeet
                artid = ent.getKey();
                break;
            }
        }
        // if we have something, return that, otherwise make a new artist
        if (artid <= -1) {
            // we have to make a new artist, so do that
            log.info("Creating new artist: {}", thesplit);
            ArtistEntry artent = new ArtistEntry();
            artent.setName(thesplit);
            artent.setNotes("Automatically created by bulk importer from a detected deviantart filename");
            artent.setUrls(new String[]{"https://www.deviantart.com/" + thesplit});
            // get our new artid
            artid = artists.getArtists().size() + 1;
            // add our artist
            artists.addArtist(artid, artent);
            // return our new value
        }
        return artid;
    }

    /**
     * some sites, namely pixiv, encode enough information in the filename to obtain the original url
     * this function will obtain said url
     * @param filename filename to parse for information
     * @param sitesource what site is this from?
     * @return original url, "No URL could be parsed" if none could be found
     */
    private static String parseFilenameforOriginalURL(String filename, int sitesource){
        // check to see if the site is supported
        if (sitesource == 1){
            log.info("PIXIV url parser now active");
            // split by _
            String[] split = filename.split("_");
            // construct url
            // return that
            return "https://www.pixiv.net/artworks/" + split[0];
        } else {
            return "No URL could be parsed";
        }
    }
}
