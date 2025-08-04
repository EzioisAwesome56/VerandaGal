package com.eziosoft.verandagal.database.migrations;

import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.objects.Artist;
import com.eziosoft.verandagal.database.objects.DbMeta;
import com.eziosoft.verandagal.server.VerandaServer;
import com.eziosoft.verandagal.utils.ConfigFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMigrations {
    private static Logger LOGGER = LogManager.getLogger("Database Migrations");

    private static boolean zero_to_2_migration = false;
    /*
    versions are as follows:
    <no version>: made with hibernate 6.6.1
    2: made with hibernate 7.0.1
     */
    private static final int db_version = 2;

    private static Configuration prepareBasicConfig(ConfigFile config){
        // we need to pretty much init the database ourselves in here
        Configuration configuration = new Configuration();
        if (config.isUseH2()){
            VerandaServer.LOGGER.info("Starting up H2 database");
            initH2Database(configuration, config);
        } else {
            VerandaServer.LOGGER.info("Connecting to MariaDB Database");
            initMariaDB(configuration, config);
        }
        // more copy and pasted stuff goes here
        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.toString(config.isShowSQL()).toLowerCase());
        configuration.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, Action.ACTION_UPDATE);
        return configuration;
    }

    /**
     * call this function to perform any database migrations you may need
     * @param config loaded copy of the configuration file
     */
    public static void performMigrations(ConfigFile config){
        // check what type of migrations are enabled and go from there
        if (zero_to_2_migration){
            // only affects mariadb, check if we're using that
            if (!config.isUseH2()){
                LOGGER.info("Your database requires the 0->2 artist table migration");
                perform0to2ArtistMigration(config);
            }
        }
        // all done!
        LOGGER.info("All required migrations, if any, have been performed");
    }

    // these were stolen from the maindb class for convience reasons
    private static void initH2Database(Configuration configuration, ConfigFile config){
        // set this to use the H2 db driver
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver");
        // set the path for where the database file will be stored
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:h2:" + config.getDatabaseDir() + "/verandamain");
        // we apparently don't need this but im gonna put it back for no good reason
        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
    }
    @SuppressWarnings("deprecation") // we dont need multiple configuration files
    private static void initMariaDB(Configuration configuration, ConfigFile config){
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.mariadb.jdbc.Driver");
        // set the path for where the database file will be stored
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:mariadb://" + config.getMaria_host() + "/" + config.getMaria_dbname());
        // we need login stuff for mariadb
        configuration.setProperty(AvailableSettings.USER, config.getMaria_user());
        configuration.setProperty(AvailableSettings.PASS, config.getMaria_pass());
        // we apparently don't need this but im gonna put it back for no good reason
        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.MariaDBDialect");
    }


    /**
     * checks what version the database is, and then will check for any migrations that are required
     */
    public static void checkVersion(){
        int cur_version;
        DbMeta meta = VerandaServer.maindb.LoadObject(DbMeta.class, 1);
        if (meta == null){
            VerandaServer.LOGGER.warn("DBMeta object does not exist, creating one");
            meta = new DbMeta();
            meta.setDbver(db_version);
            // write one to the database
            VerandaServer.maindb.SaveObject(meta);
            // assume we need the 0 -> 2 migration
            zero_to_2_migration = true;
            cur_version = 0;
        } else {
            cur_version = meta.getDbver();
        }
        VerandaServer.LOGGER.info("Current Database version is: {}", cur_version);
    }

    private static SessionFactory connect(Configuration cfg){
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(cfg.getProperties()).build();

        return cfg.buildSessionFactory(serviceRegistry);
    }

    /**
     * converts artists from mariadb's varbinary to whatever it uses for String[]
     * @param cfile loaded configuration file
     */
    private static void perform0to2ArtistMigration(ConfigFile cfile){
        // close the existing connection to the maindb
        VerandaServer.maindb.close();
        // get a configuration
        Configuration conf = prepareBasicConfig(cfile);
        // add some more basics to it
        conf.addAnnotatedClass(ver0_artists.class);
        // connect
        SessionFactory factory = connect(conf);
        // get a count of the records in the artists table
        Session sesh = factory.openSession();
        Transaction act = sesh.beginTransaction();
        Query<Long> q = sesh.createQuery("select count(*) from " + ver0_artists.class.getName(), Long.class);
        // get the size of the returned results
        int size = q.getResultList().size();
        // commit it
        act.commit();
        if (size < 1){
            LOGGER.error("Something has gone horribly wrong trying to perform a migration of artists!");
            LOGGER.error("there are no artists in the table, halting migration and reconnecting to maindb");
            sesh.close();
            factory.close();
            VerandaServer.maindb = new MainDatabase(cfile);
            return;
        }
        long num_arts = q.uniqueResult();
        sesh.close();
        // ok, now we have to start making prepwork for getting every record out of that table
        List<ver0_artists> oldrecords = new ArrayList<>();
        for (long i = 0; i < num_arts; i++){
            // get a record
            sesh = factory.openSession();
            act = sesh.beginTransaction();
            ver0_artists temp = sesh.find(ver0_artists.class, i);
            act.commit();
            sesh.close();
            // check to make sure its valid
            if (temp == null){
                LOGGER.warn("Loaded NULL object for artist id {}, this item will be skipped", i);
                continue;
            } else {
                oldrecords.add(temp);
            }
        }
        // we're done reading records
        LOGGER.info("Retrieved {} records from the old table!", oldrecords.size());
        // next, we need to retrieve the links out of the garbage data
        List<Artist> newrecords = new ArrayList<>();
        for (ver0_artists art : oldrecords){
            Artist newart = new Artist();
            // set the values we can read without any problems
            newart.setId(art.getId());
            newart.setName(art.getName());
            newart.setNotes(art.getNotes());
            // now we need to get the urls
            String blah = new String(art.getUrls(), StandardCharsets.UTF_8);
            // yank out the crap we dont care about
            blah = blah.substring(45);
            if (blah.equals("none")){
                newart.setUrls(new String[]{blah});
            } else {
                // remove 1 more character
                blah = blah.substring(2);
                newart.setUrls(new String[]{blah});
            }
            LOGGER.debug("Retrieved URL: {}", blah);
            // add our artist to the list
            newrecords.add(newart);
        }
        LOGGER.info("Converted {} records to the new format", newrecords.size());
        // now we have to drop the old table
        LOGGER.warn("NOW DROPPING TABLE, YOU BETTER HAVE A BACKUP!");
        sesh = factory.openSession();
        act = sesh.beginTransaction();
        sesh.createNativeQuery("DROP TABLE Artists", ver0_artists.class).executeUpdate();
        act.commit();
        // close it
        sesh.close();
        factory.close();
        // reconnect the main database
        VerandaServer.maindb = new MainDatabase(cfile);
        // now, we resave all of our new artists files
        LOGGER.info("Merging records back into the database");
        for (Artist artist : newrecords){
            VerandaServer.maindb.MergeObject(artist);
        }
        LOGGER.info("Migration done!");
    }
}
