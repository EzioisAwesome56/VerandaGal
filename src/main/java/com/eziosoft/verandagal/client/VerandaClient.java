package com.eziosoft.verandagal.client;

import com.eziosoft.verandagal.client.dialogs.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.client.json.ArtistEntry;
import com.eziosoft.verandagal.client.json.ImageEntry;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;
import com.eziosoft.verandagal.client.json.PackMetaFile;
import com.eziosoft.verandagal.client.utils.ClientUtils;
import com.eziosoft.verandagal.client.utils.ImageProcessor;
import com.eziosoft.verandagal.client.utils.SimpleFileFilter;
import com.eziosoft.verandagal.client.utils.ZipFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class VerandaClient {

    // setup the new logger
    public static final Logger log = LogManager.getLogger("VerandaClient");

    public static void RunClient() {
        log.info("Client mode now starting up");
        guiMainWindow main = new guiMainWindow();
        int action = main.openDialog();
        switch (action){
            case 0:
                ArtistExportGUI();
                break;
            case 1:
                ArtistImportGui();
                break;
            case 2:
                ArtistManager();
                break;
            case 3:
                PackCreator();
                break;
            case 4:
                PackZipImportGui();
                break;
            case 5:
                zipPackGUI();
                break;
            case 6:
                PackEditor();
                break;
            default:
                log.error("Invalid action selected");
        }
    }

    /**
     * again, pretty much a shim for the real call in ZipFileUtils
     * because that needs to be usable from a CLI on a server
     */
    private static void PackZipImportGui(){
        // prompt user for zip file
        File packzip = ClientUtils.browseForFileFiltered("Select pack zip file", new SimpleFileFilter("zip"));
        // run the main class for this
        ZipFileUtils.importPackZip(packzip);
    }

    /**
     * shim call for the actual routine for this in clientutils
     * this will export all artists currently in the DB to a file
     */
    private static void ArtistExportGUI(){
        // pop open a file selector to pick where you wish to sae the file
        File output_file = ClientUtils.browseForFile("Save artists file as...");
        // pass it on to the actual method for doing this
        ClientUtils.exportArtistsToFile(output_file);
    }

    /**
     * GUI shim to prompt for an artists.json
     * and then import it
     */
    private static void ArtistImportGui(){
        // prompt for json
        File jsonfile = ClientUtils.browseForFileFiltered("Select artists.json file", new SimpleFileFilter("json"));
        try {
            // we have to now read this file in
            String content = FileUtils.readFileToString(jsonfile, StandardCharsets.UTF_8);
            // read into the right type of java object we need
            ImportableArtistsFile artfile = Main.gson_pretty.fromJson(content, ImportableArtistsFile.class);
            // run the main code
            ClientUtils.importArtists(artfile);
        } catch (IOException e){
            VerandaClient.log.error("Error while trying to read selected file", e);
        }
    }

    /**
     * prompts the user for a folder, then hands it off to the ZipFileUtils Class
     */
    private static void zipPackGUI(){
        // get the pack folder
        File packfolder = ClientUtils.browseForDirectory("Select directory of Image Pack");
        // pass it to the other class
        ZipFileUtils.CreateImagePackZip(packfolder);
    }

    /**
     * used for editing a pre-existing Image Pack
     * needs to be uncompressed and in a folder
     */
    private static void PackEditor(){
        // prompt the user to select a folder
        File packfolder = ClientUtils.browseForDirectory("Select directory of Image Pack");
        // check to see if a pack.json file exists in the directory
        File packmetafile = new File(packfolder, "pack.json");
        if (!packmetafile.exists()){
            log.error("Pack.json file does not exist in given directory!");
            log.error("Exiting");
            return;
        }
        // read the file into memory
        String filecontent;
        try {
            filecontent = FileUtils.readFileToString(packmetafile, StandardCharsets.UTF_8);
        } catch (IOException e){
            log.error("Error while trying to read pack.json: ", e);
            return;
        }
        // then, get the object from it
        PackMetaFile packmeta = Main.gson_pretty.fromJson(filecontent, PackMetaFile.class);
        log.info("Loaded pack: {}", packmeta.getPackname());
        // first, open the pack meta editor
        guiPackMetaEditor metaEditor = new guiPackMetaEditor(packmeta);
        PackMetaFile tempmeta = metaEditor.openDialog();
        if (tempmeta == null){
            log.warn("Either something broke or user pressed cancel on meta editor!");
        } else {
            // put the old image array into temp
            tempmeta.setImages(packmeta.getImages());
            // update our meta with the temp meta
            packmeta = tempmeta;
            // write any new changes to disk
            try {
                FileUtils.writeStringToFile(packmetafile, Main.gson_pretty.toJson(packmeta), StandardCharsets.UTF_8);
            } catch (IOException e){
                log.error("ERROR while trying to write to file", e);
                return;
            }
        }
        // then, we can deal with the image manager
        // but first we need the artists file
        ImportableArtistsFile artists = ClientUtils.getOrCreateArtistsFile();
        // NOW we can do the thing
        packmeta = runPackImageManager(packfolder, packmeta, artists);
        // save to disk once done
        try {
            FileUtils.writeStringToFile(packmetafile, Main.gson_pretty.toJson(packmeta), StandardCharsets.UTF_8);
        } catch (IOException e){
            log.error("ERROR while trying to write to file", e);
            return;
        }
        log.info("Done editing pack!");
    }

    /**
     * this code was going to get copy and pasted in two different place
     * so i broke it out into its own function
     * so that i didnt have to make more pasta monsters
     * @param outfolder the directory of the pack you want to operate on
     * @param packmeta the pack.json loaded as a java object
     * @param artists the local artists.json file loaded as a java object
     */
    private static PackMetaFile runPackImageManager(File outfolder, PackMetaFile packmeta, ImportableArtistsFile artists){
        // we dont really need to pass in another argument
        // so this is here just for ""code glue""
        File packmetafile = new File(outfolder, "pack.json");
        // make the images and thumbnails directory
        File imagedir = new File(outfolder, "images");
        File thumbdir = new File(outfolder, "thumbs");
        imagedir.mkdir();
        thumbdir.mkdir();
        // next, we need to setup the image manager
        // pretty much keep looping until we get told its time to yeet
        boolean exit = false;
        while (!exit){
            // create a new image manager
            guiPackImageManager manager = new guiPackImageManager(packmeta, outfolder, artists);
            // launch it
            packmeta = manager.openDialog();
            // get the exit code
            int code = manager.getExit_code();
            switch (code){
                case 0:
                    // 0 is the finish button, so break out of this
                    exit = true;
                    break;
                case 1:
                    // 1 means add images
                    // so run the image adder code
                    ArrayList<ImageEntry> source = packmeta.getImages();
                    ArrayList<ImageEntry> output = ImageProcessor.doImageAddition(source, outfolder, artists);
                    // update the packmeta
                    packmeta.setImages(output);
                    // for good measure, try to save the packmeta object
                    try {
                        FileUtils.writeStringToFile(packmetafile, Main.gson_pretty.toJson(packmeta), StandardCharsets.UTF_8);
                    } catch (IOException e){
                        log.error("ERROR while trying to write to file", e);
                    }
                    break;
                case 2:
                    // get the item that was selected from the last gui
                    int index = manager.getSelected_index();
                    // get the targeted imageentry object
                    ImageEntry ent = packmeta.getImages().get(index);
                    // get the actual image file path
                    File ourimg = new File(outfolder, "images/" + ent.getFilename());
                    // next, we can setup the meta editor
                    guiImageMetaEditor editor = new guiImageMetaEditor(ourimg.getAbsolutePath(), artists);
                    editor.SetupEditor(ent);
                    ImageEntry temp = editor.OpenDialog();
                    // check to see if the new object is valid
                    if (temp != null){
                        // if its not null, then a change was made
                        // update our entry
                        ent = temp;
                        // remove the old version
                        packmeta.getImages().remove(index);
                        // insert the new one at the old one's index
                        packmeta.getImages().add(index, ent);
                    } else {
                        log.debug("No changes to this image where made");
                    }
                    break;
                default:
                    log.warn("Invalid exit code made during image manager loop!");
            }
        }
        // once we're done, return the packmeta
        return packmeta;
    }

    /**
     * this method is pretty much the entire pack creator routines
     * it is very big and annoying
     */
    private static void PackCreator(){
        log.info("Now starting the pack creator...");
        // load the artists file from disk
        ImportableArtistsFile artists = ClientUtils.getOrCreateArtistsFile();
        // init a blank pack object
        PackMetaFile packmeta = new PackMetaFile();
        // next we need to prompt the user for a folder to store the pack in
        File outfolder = ClientUtils.browseForDirectory("Select output folder for pack");
        if (outfolder == null){
            VerandaClient.log.error("Invalid selection made, cancelling pack creator");
            return;
        }
        // ok, now we have this, so then we need to let the user configure the meta of the pack
        guiPackMetaEditor metaEditor = new guiPackMetaEditor(packmeta);
        packmeta = metaEditor.openDialog();
        // so now we have pack meta. we should do the bare basics and save this file to disk now
        File packmetafile = new File(outfolder, "pack.json");
        // check if file exists
        if (packmetafile.exists()){
            log.warn("File already exists: ", packmetafile.getAbsoluteFile());
            log.warn("the contents of this file will be overwritten");
            log.warn("additionally, you may want to remove any other random files from this folder!");
        }
        // write to the file
        try {
            FileUtils.writeStringToFile(packmetafile, Main.gson_pretty.toJson(packmeta), StandardCharsets.UTF_8);
        } catch (IOException e){
            log.error("ERROR while trying to write to file", e);
            log.error("aborting operation");
            return;
        }
        // do the main image manager loop
        packmeta = runPackImageManager(outfolder, packmeta, artists);
        // we have now left the main loop.
        // once we're done managing images, we're pretty much done so make sure its saved again
        try {
            FileUtils.writeStringToFile(packmetafile, Main.gson_pretty.toJson(packmeta), StandardCharsets.UTF_8);
        } catch (IOException e){
            log.error("ERROR while trying to write to file", e);
        }
        log.info("Image Pack basic creation done!");
        log.info("It would be advisable to use the \"zip pack\" feature on the main menu to compress the pack");
        log.info("Or, you could just do it yourself");
    }

    /**
     * this handles the artist manager option that u can select from the main menu
     */
    private static void ArtistManager(){
        // time to spin up the artist manager
        // check to see if we have a local database of artists
        ImportableArtistsFile artists = ClientUtils.getOrCreateArtistsFile();
        // we still need this file here anyway tho
        File localdb = new File("artists.json");
        log.info("Artists file loaded!");
        boolean exit = false;
        while (!exit) {
            guiArtistManager manager = new guiArtistManager(artists);
            int action = manager.openDialog();
            switch (action){
                case 0 -> exit = true;
                case 1 -> doArtistCreation(artists);
                case 2 -> saveArtistsLocal(artists, localdb);
                case 3 -> doArtistEdit(artists, manager.getSelected_artist());
                case 4 -> DeleteArtist(artists, manager.getSelected_artist());
                default -> log.error("Invalid action selected!");
            }
        }
    }

    private static void DeleteArtist(ImportableArtistsFile file, int index){
        // convert to long
        long ldex = Long.parseLong(String.valueOf(index));
        // remove artist
        file.getArtists().remove(ldex);
        log.info("Artist Deleted!");
    }

    private static void saveArtistsLocal(ImportableArtistsFile content, File file){
        // pretty much just write to the file
        log.info("Now saving artists file...");
        try {
            FileUtils.writeStringToFile(file, Main.gson_pretty.toJson(content), StandardCharsets.UTF_8, false);
        } catch (IOException e){
            log.error("Error while trying to write file", e);
            return;
        }
        log.info("File saved!");
        return;
    }

    private static void doArtistEdit(ImportableArtistsFile file, int index){
        // we need to convert the index to a long
        long londex = Long.parseLong(String.valueOf(index));
        ArtistEntry ent = file.getArtists().get(londex);
        // make a new creator object
        guiArtistCreation creator = new guiArtistCreation();
        // set it to be in editor mode
        creator.SetupEditor(ent);
        // run the editor
        ent = creator.openDialog();
        if (ent != null){
            // update it in the list
            file.getArtists().remove(londex);
            file.addArtist(londex, ent);
            // and now we're done
        }
    }

    private static void doArtistCreation(ImportableArtistsFile file){
        // init the creation dialog
        guiArtistCreation creator = new guiArtistCreation();
        // get a new artist object
        ArtistEntry newent = creator.openDialog();
        if (newent != null){
            log.debug("new artist created");
            int next = file.getArtists().size() + 1;
            file.addArtist(Long.parseLong(String.valueOf(next)), newent);
        } else {
            log.debug("The user cancelled the creation operation");
        }

    }
}
