package com.eziosoft.verandagal.utils;

import com.eziosoft.verandagal.Main;

import java.io.File;

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
}
