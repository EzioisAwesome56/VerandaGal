package com.eziosoft.verandagal.client.utils;

import com.eziosoft.verandagal.client.json.ArtistEntry;
import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.objects.Artist;
import com.eziosoft.verandagal.utils.ConfigFile;
import com.eziosoft.verandagal.utils.ConfigUtils;
import org.apache.commons.io.FileUtils;
import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.client.VerandaClient;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ClientUtils {

    public static ImportableArtistsFile getOrCreateArtistsFile(){
        ImportableArtistsFile artists;
        File localdb = new File("artists.json");
        if (!localdb.exists()){
            Main.LOGGER.warn("no artists.json found! creating new one");
            ImportableArtistsFile temp = new ImportableArtistsFile();
            // write this file out to disk
            try {
                FileUtils.write(localdb, Main.gson_pretty.toJson(temp), StandardCharsets.UTF_8);
            } catch (Exception e){
                Main.LOGGER.error("Error trying to write artists.json to disk!", e);
                System.exit(1);
            }
            // once its written to the disk, set the variable to our new blank object
            artists = temp;
        } else {
            // read the file in
            String read = "";
            try {
                read = FileUtils.readFileToString(localdb, StandardCharsets.UTF_8);
            } catch (IOException e){
                Main.LOGGER.error("Failed while attempting to read in artists file", e);
                System.exit(1);
            }
            // then get the object from the json
            artists = Main.gson_pretty.fromJson(read, ImportableArtistsFile.class);
        }
        return artists;
    }

    public static File browseForDirectory(String title){
        JFileChooser filediag = new JFileChooser();
        filediag.setDialogTitle(title);
        filediag.setApproveButtonText("Use this Folder");
        filediag.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = filediag.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION){
            VerandaClient.log.warn("Somehow, an invalid selection was made!");
            return null;
        }
        // return our file
        return filediag.getSelectedFile();
    }

    public static File browseForFileFiltered(String title, FileFilter filter){
        JFileChooser filediag = new JFileChooser();
        filediag.setDialogTitle(title);
        filediag.setFileFilter(filter);
        return baseBrowseFile(filediag);
    }
    public static File browseForFile(String title){
        JFileChooser filediag = new JFileChooser();
        filediag.setDialogTitle(title);
        return baseBrowseFile(filediag);
    }

    private static File baseBrowseFile(JFileChooser filediag){
        filediag.setFileSelectionMode(JFileChooser.FILES_ONLY);
        filediag.setMultiSelectionEnabled(false);
        int result = filediag.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION){
            VerandaClient.log.warn("Somehow, an invalid selection was made!");
            return null;
        }
        // return our file
        return filediag.getSelectedFile();
    }

    /**
     * this was in importArtists before, but split it out so that it didnt make a seperate db connection and make
     * memory leaks
     * @param file artists file to import
     * @param maindb connection to main database
     */
    public static void importArtistsWithDatabaseConnection(ImportableArtistsFile file, MainDatabase maindb){
        // lifted from the original importArtists function
        VerandaClient.log.info("now checking mode of artist files");
        if (file.getMode() == 0){
            VerandaClient.log.info("Running in import mode (0)");
            // do a loop for each artist in the file
            for (Map.Entry<Long, ArtistEntry> ent : file.getArtists().entrySet()){
                // get the id of the ent
                long id = ent.getKey();
                // attempt to load it from the DB
                Artist tempart = maindb.LoadObject(Artist.class, id);
                // does it actually exist?
                if (tempart != null){
                    // oh no, something is already there
                    // do they match?
                    if (!tempart.getName().equals(ent.getValue().getName())){
                        VerandaClient.log.error("ERROR ON ARTIST IMPORT: there is an entry at the ID already");
                        VerandaClient.log.error("The names do not match, so we will be aborting this import operation");
                        VerandaClient.log.error("Artist at ID {}: {}", id, tempart.getName());
                        VerandaClient.log.error("New artist in that slot: {}", ent.getValue().getName());
                        VerandaClient.log.warn("If you don't care about the problems this can cause, set the import mode to 2 in the json");
                        // BAIL THE FUCK OUT
                        return;
                    }
                } else {
                    // if it is null, we dont have to worry about it
                    // create a new object and fill it out
                    tempart = new Artist();
                    tempart.setName(ent.getValue().getName());
                    tempart.setUrls(ent.getValue().getUrls());
                    tempart.setNotes(ent.getValue().getNotes());
                    // ok, we have our object. save it to the database
                    maindb.SaveObject(tempart);
                    VerandaClient.log.info("Imported artist {} into db", tempart.getName());
                }
            }
            // we're done, i think
            VerandaClient.log.info("Done importing artists");
        } else if (file.getMode() == 2){
            // TODO: drop an atomic bomb on everything and dont care what breaks
        } else if (file.getMode() == 1){
            // TODO: update records
        } else {
            Main.LOGGER.error("Invalid import mode in artists file: {}", file.getMode());
        }
    }

    /**
     * used to take a local artists.json file
     * and import their information into the DB
     * @param file
     */
    public static void importArtists(ImportableArtistsFile file){
        // load the config file
        File conffile = ConfigUtils.checkForConfig();
        String readfile;
        try {
            readfile = FileUtils.readFileToString(conffile, StandardCharsets.UTF_8);
        } catch (IOException e){
            Main.LOGGER.error("Error trying to read config file", e);
            return;
        }
        // convert to java object
        ConfigFile config = Main.gson_pretty.fromJson(readfile, ConfigFile.class);
        // spin up the main DB
        MainDatabase maindb = new MainDatabase(config);
        // this part of the function was moved to another call, run that instead
        importArtistsWithDatabaseConnection(file, maindb);
        // close the connection
        maindb.close();
    }

    /**
     * broke out of exportArtistsToFile below, because i needed this information for my own usages later
     * @param maindb provide a connection to the main database here
     * @return the artists file object
     */
    public static ImportableArtistsFile exportArtists(MainDatabase maindb){
        // get the count of artists in the database
        long total_artists = maindb.getCountOfRecords(Artist.class);
        if (total_artists < 1){
            VerandaClient.log.error("Error: no artists returned or there was an error");
            VerandaClient.log.error("returned value was: {}", total_artists);
            // bail out
            System.exit(1);
        }
        // create a new object to store all of this crap
        ImportableArtistsFile file = new ImportableArtistsFile();
        // 0 is a reasonable mode for us to export with
        file.setMode(0);
        // next, we have to do some funny things
        // mostly loop thru the DB to get every artist
        VerandaClient.log.info("Now reading the database...");
        for (long x = 0; x < total_artists; x++){
            // attempt to get something
            Artist temp = maindb.LoadObject(Artist.class, x + 1);
            // check if we got anything
            if (temp == null){
                // oh no, there was an error
                // we'll just skip it
                VerandaClient.log.warn("Artist {} returned null, skipping", x+1);
                continue;
            }
            // otherwise, make an artist entry
            ArtistEntry tmpent = new ArtistEntry();
            // fill it in with our information
            tmpent.setName(temp.getName());
            tmpent.setNotes(temp.getNotes());
            tmpent.setUrls(temp.getUrls());
            // we have the thing, put it in the artists file
            file.addArtist(x + 1, tmpent);
        }
        return file;
    }

    /**
     * exports all artists in the database to an artists.json file at the provided file object
     * @param outputfile
     */
    public static void exportArtistsToFile(File outputfile){
        // attempt to load the config file
        File conffile = ConfigUtils.checkForConfig();
        String readfile;
        try {
            readfile = FileUtils.readFileToString(conffile, StandardCharsets.UTF_8);
        } catch (IOException e){
            VerandaClient.log.error("Error trying to read config file", e);
            return;
        }
        // then, convert it to a java object
        ConfigFile config = Main.gson_pretty.fromJson(readfile, ConfigFile.class);
        // spin up the main DB, we will need it shortly
        MainDatabase maindb = new MainDatabase(config);
        // export the artists
        ImportableArtistsFile file = exportArtists(maindb);
        maindb.close();
        // we're done, output some debug shit
        VerandaClient.log.info("Done reading database. Exported {} artists", file.getArtists().size());
        VerandaClient.log.info("Now saving file to {}", outputfile.getAbsolutePath());
        // convert the object to json
        String json = Main.gson_pretty.toJson(file);
        // write it
        try {
            FileUtils.write(outputfile, json, StandardCharsets.UTF_8);
        } catch (IOException e){
            VerandaClient.log.error("Fatal error while trying to write file!");
            VerandaClient.log.error(e);
            VerandaClient.log.error("Giving up");
            // give up, we failed
            System.exit(1);
        }
    }


}
