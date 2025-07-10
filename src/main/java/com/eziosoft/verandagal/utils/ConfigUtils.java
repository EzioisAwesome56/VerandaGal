package com.eziosoft.verandagal.utils;

import com.eziosoft.verandagal.Main;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConfigUtils {

    public static File checkForConfig(){
        Main.LOGGER.info("Checking to see if there is a config.json file...");
        File conffile = new File("config.json");
        if (!conffile.exists()){
            Main.LOGGER.error("No config.json file found!");
            Main.LOGGER.error("You should probably run this program with --server atleast once");
            Main.LOGGER.error("and then edit the newly created config.json before you try");
            Main.LOGGER.error("to import any packs");
            System.exit(1);
        }
        // otherwise we probably have it
        return conffile;
    }

    /**
     * Gets the config file. errors out if it doesnt exit
     * @return the config file
     */
    public static ConfigFile getConfigFile() throws IOException{
        File conffile = ConfigUtils.checkForConfig();
        // load the config file
        String readfile;
        try {
            readfile = FileUtils.readFileToString(conffile, StandardCharsets.UTF_8);
        } catch (IOException e){
            Main.LOGGER.error("Error trying to read config file", e);
            throw new IOException("Failed to load config", e);
        }
        // convert to java object
        return Main.gson_pretty.fromJson(readfile, ConfigFile.class);
    }
}
