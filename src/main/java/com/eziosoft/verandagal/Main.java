package com.eziosoft.verandagal;

import com.eziosoft.verandagal.client.VerandaCLI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eziosoft.verandagal.client.VerandaClient;
import com.eziosoft.verandagal.server.VerandaServer;

import javax.imageio.ImageIO;

public class Main {
    public static Gson gson_pretty = new GsonBuilder().setPrettyPrinting().create();
    // new logging!!!
    // this one is public so that anything that doesnt deserve its own logger can access it
    public static Logger LOGGER = LogManager.getLogger("MainClass");

    public static void main(String[] args) throws ParseException {
        LOGGER.info("VerandaGal v1.0");
        // setup extra runtime options
        setBootProperties();
        // setup options for stuff
        Options cliopts = new Options();
        Option server = new Option("s", "server", false, "Start as Server");
        Option client = new Option("c", "client", false, "Start as Client");
        Option cli = new Option("t", "cli", false, "Start as client with CLI");
        cliopts.addOption(server);
        cliopts.addOption(client);
        cliopts.addOption(cli);
        // parse our args
        CommandLineParser parse = new DefaultParser();
        CommandLine cmd = parse.parse(cliopts, args, true);
        if (cmd.hasOption("s")){
            LOGGER.info("Starting in Server mode");
            try {
                VerandaServer.StartServer();
            } catch (Exception e){
                LOGGER.error("Error has occured while trying to start server!", e);
                System.exit(1);
            }
        } else if (cmd.hasOption("c")){
            LOGGER.info("Starting in Client mode");
            try {
                VerandaClient.RunClient();
            } catch (Exception e){
                LOGGER.error("Error while trying to run the client!", e);
                System.exit(1);
            }
        } else if (cmd.hasOption("t")){
            LOGGER.info("Starting in cli mode...");
            try {
                VerandaCLI.runCLI(args);
            } catch (Exception e){
                LOGGER.error("Error while running cli!", e);
                System.exit(1);
            }
        } else {
            LOGGER.warn("No mode selected");
            HelpFormatter help = new HelpFormatter();
            help.printHelp("verandagal", cliopts);
            // exit on no mode selected, because that is just raw cringe
            System.exit(1);
        }
    }

    private static void setBootProperties(){
        // just to be safe, disable imageIO cache right away
        ImageIO.setUseCache(false);
        // make hibernate use SLF4j for logging
        System.setProperty("org.jboss.logging.provider", "slf4j");
    }
}