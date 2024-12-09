package com.eziosoft.verandagal.client;

import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;
import com.eziosoft.verandagal.client.utils.ClientUtils;
import com.eziosoft.verandagal.client.utils.ZipFileUtils;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class VerandaCLI {

    public static void runCLI(String[] args) throws ParseException {
        // create new options parser thing
        Options cliopts = new Options();
        // define options
        Option input_file = new Option("f", "file", true, "The input (or output) file for any of the other options");
        Option mode = new Option("m", "mode", true, "Operation mode for cli");
        // cli complains unless we add this dummb option
        Option dummy = new Option("t", "cli", false, "dummy option to make the parser happy");
        // add all the options to the main options thing
        cliopts.addOption(input_file);
        cliopts.addOption(mode);
        cliopts.addOption(dummy);
        // blah blah boilerplate shit
        CommandLineParser parse = new DefaultParser();
        CommandLine cmd = parse.parse(cliopts, args);
        // check for mode function
        if (cmd.hasOption("m") && cmd.hasOption("f")){
            String sel_mode = cmd.getOptionValue("m");
            // code to check to make sure said file even exists
            // read the file from the option
            String file = cmd.getOptionValue("f");
            File thefile = new File(file);
            // check the basics
            if (!thefile.exists()){
                VerandaClient.log.error("Error: file {} does not exist", file);
                System.exit(1);
            }
            if (thefile.isDirectory() && !thefile.isFile()){
                VerandaClient.log.error("Error: provided path {} is a directory and not file!", file);
                System.exit(1);
            }
            // and now the actual mode select
            switch (sel_mode){
                case "packimport" -> {
                    // check file extension just to be sure
                    if (!FilenameUtils.getExtension(file).equalsIgnoreCase("zip")){
                        VerandaClient.log.error("Error: provided file {} is not a .zip file!", file);
                        System.exit(1);
                    }
                    // ok, now we can go do the actual import
                    VerandaClient.log.info("Starting import");
                    ZipFileUtils.importPackZip(thefile);
                }
                case "artistimport" -> {
                    // check file extension to be sure
                    if (!FilenameUtils.getExtension(file).equalsIgnoreCase("json")){
                        VerandaClient.log.error("Error: provided file {} is not a .json file", file);
                        System.exit(1);
                    }
                    // alright, now we have to try and read this file in
                    try {
                        String content = FileUtils.readFileToString(thefile, StandardCharsets.UTF_8);
                        // use gson to convert to object
                        ImportableArtistsFile artfile = Main.gson_pretty.fromJson(content, ImportableArtistsFile.class);
                        // run the method
                        ClientUtils.importArtists(artfile);
                    } catch (IOException e){
                        VerandaClient.log.error("Error while trying to read in json file!", e);
                        // bail out, shit didnt work
                        System.exit(1);
                    }
                }
                case "artistexport" -> {
                    // note: you may have to make a dummy file to get past the exists check
                    // TODO: fix this at some point
                    VerandaClient.log.info("running artists exporter...");
                    ClientUtils.exportArtists(thefile);
                }
                default -> {
                    VerandaClient.log.error("Invalid mode \"{}\" selected!", sel_mode);
                    System.exit(1);
                }
            }
        } else {
            // display help
            VerandaClient.log.error("No mode or file provided (or both)!");
            HelpFormatter help = new HelpFormatter();
            help.printHelp("verandagal -t", cliopts);
            VerandaClient.log.info("Valid modes are:\n" +
                    "packimport: import an image pack from a .zip file\n" +
                    "artistimport: import artist.json file for artists\n" +
                    "artistexport: export artists to output file");
        }
    }
}
