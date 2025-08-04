package com.eziosoft.verandagal.server;

import com.eziosoft.verandagal.database.migrations.DatabaseMigrations;
import com.eziosoft.verandagal.server.objects.PackInfoManager;
import com.eziosoft.verandagal.server.objects.TemplateManager;
import com.eziosoft.verandagal.server.objects.VerandaGalHTTP;
import com.eziosoft.verandagal.server.servlets.*;
import com.eziosoft.verandagal.server.utils.SidebarBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.ThumbnailStore;
import com.eziosoft.verandagal.utils.ConfigFile;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.eziosoft.verandagal.Main.gson_pretty;

public class VerandaServer {
    static VerandaGalHTTP server;
    public static ConfigFile configFile;
    public static TemplateManager template;
    public static ThumbnailStore thumbnails;
    public static MainDatabase maindb;
    public static PackInfoManager packinfo;
    public static SidebarBuilder sidebarBuilder;
    public static Logger LOGGER = LogManager.getLogger("VerandaServer");

    public static void StartServer() throws Exception{
        LOGGER.info("Checking for configuration file");
        // check if the config file exists
        File cfg = new File("config.json");
        if (!cfg.exists()){
            LOGGER.warn("It appears you do not have a config file!");
            // make one
            ConfigFile bruh = new ConfigFile();
            try {
                FileUtils.write(cfg, gson_pretty.toJson(bruh), StandardCharsets.UTF_8);
            } catch (IOException e){
                LOGGER.error("Well, I tried creating one for you, but it failed", e);
                System.exit(1);
            }
            LOGGER.warn("I have created a default one for you");
            LOGGER.warn("Consider editing it and then restarting this program");
            System.exit(0);
        } else {
            // read the file in
            String temp = FileUtils.readFileToString(cfg, StandardCharsets.UTF_8);
            configFile = gson_pretty.fromJson(temp, ConfigFile.class);
            LOGGER.info("Configuration file loaded");
        }
        // NEW FEATURE: enable debug logs if set in config file
        if (configFile.isShowDebugLog()){
            Configurator.setLevel(LogManager.getRootLogger().getName(), Level.TRACE);
        }
        LOGGER.info("Now starting server...");
        // init the db
        maindb = new MainDatabase(configFile);
        // check for migrations
        DatabaseMigrations.checkVersion();
        DatabaseMigrations.performMigrations(configFile);
        // init the other db, too
        thumbnails = new ThumbnailStore(configFile);
        // init the cachers for various things
        template = new TemplateManager();
        packinfo = new PackInfoManager(maindb);
        sidebarBuilder = new SidebarBuilder();
        // test to see if usermain.html exists
        File usermain = new File("usermain.html");
        if (!usermain.exists()){
            // copy our included copy to the folder
            String read = IOUtils.resourceToString("/resource/usermain.html", StandardCharsets.UTF_8);
            FileUtils.write(usermain, read, StandardCharsets.UTF_8);
            // and we're done, womp womp
        }
        // make new server
        server = new VerandaGalHTTP(configFile.getWebPort(), configFile.getMaxNumbThreads());
        // add all servlets
        server.addAsyncServlet(MainPageServlet.class, "/");
        server.addAsyncServlet(ResourceServlet.class, "/res/*");
        server.addAsyncServlet(ImageViewServlet.class, "/image/*");
        server.addAsyncServlet(ImageBackendServlet.class, "/img/*");
        server.addAsyncServlet(ThumbnailBackendServlet.class, "/thumb/*");
        server.addAsyncServlet(GalleryBrowseServlet.class, "/pack/*");
        server.addAsyncServlet(RandomImageServlet.class, "/random/*");
        server.addAsyncServlet(SettingsPageServlet.class, "/setting/*");
        server.addAsyncServlet(APIHandlerServlet.class, "/api/*");
        server.addAsyncServlet(ArtistInfoServlet.class, "/artist/*");
        server.addAsyncServlet(ArtistListServlet.class, "/artlist/*");
        server.addAsyncServlet(PackListServlet.class, "/packlist/*");
        server.addAsyncServlet(PackInfoServlet.class, "/packinfo/*");
        server.addAsyncServlet(PreviewBackendServlet.class, "/preview/*");
        server.addAsyncServlet(ViewAllBrowseServlet.class, "/all/*");
        server.addAsyncServlet(SearchPageServlet.class, "/search/*");
        // start it
        server.start();
        /*
        with debug logs turned off by default, i feel like there should atleast be some message saying it started up
        otherwise it would bother even me when nothing was printed yet it started up anyway
         */
        LOGGER.info("VerandaGal Server startup complete");
    }
}
