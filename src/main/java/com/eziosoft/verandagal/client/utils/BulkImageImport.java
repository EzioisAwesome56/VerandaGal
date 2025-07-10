package com.eziosoft.verandagal.client.utils;

import com.eziosoft.verandagal.server.utils.ServerUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
        // prepare our basic image metadata
        ArrayList<BulkImageObject> bulkobjs = prepareMetadata(supportedfiles, scan);
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
    private static ArrayList<BulkImageObject> prepareMetadata(List<File> input, Scanner scan){
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
            // create a new object
            BulkImageObject temp = new BulkImageObject(name, site, ai, rate);
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
}
