package com.eziosoft.verandagal.database;

import com.eziosoft.verandagal.Main;
import com.eziosoft.verandagal.utils.ConfigFile;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import com.eziosoft.verandagal.database.objects.Artist;
import com.eziosoft.verandagal.database.objects.Image;
import com.eziosoft.verandagal.database.objects.ImagePack;
import com.eziosoft.verandagal.server.VerandaServer;

import java.io.File;
import java.util.List;

public class MainDatabase {

    // code borrowed from a dead project
    // it may have been a death, but the free code is much appreciated!
    private SessionFactory factory;
    private final String dbdir;
    private final boolean showsql;
    private final boolean useH2;
    private final String username;
    private final String password;
    private final String host;
    private final String database;

    public MainDatabase(ConfigFile config){
        // set the db dir
        this.dbdir = config.getDatabaseDir();
        this.showsql = config.isShowSQL();
        this.useH2 = config.isUseH2();
        this.username = config.getMaria_user();
        this.password = config.getMaria_pass();
        this.host = config.getMaria_host();
        this.database = config.getMaria_dbname();
        // DONT FORGET TO INIT THE FACTORY DUMBASS
        this.initSessionFactory();
        this.doInitialIndex(config);
    }

    public void close(){
        this.factory.close();
    }

    /**
     * these two are called for whatever database driver is used. both are included in the jar
     */
    private void initH2Database(Configuration configuration){
        // set this to use the H2 db driver
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver");
        // set the path for where the database file will be stored
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:h2:" + this.dbdir + "/verandamain");
        // we apparently don't need this but im gonna put it back for no good reason
        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
    }
    @SuppressWarnings("deprecation") // we dont need multiple configuration files
    private void initMariaDB(Configuration configuration){
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.mariadb.jdbc.Driver");
        // set the path for where the database file will be stored
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:mariadb://" + this.host + "/" + this.database);
        // we need login stuff for mariadb
        configuration.setProperty(AvailableSettings.USER, this.username);
        configuration.setProperty(AvailableSettings.PASS, this.password);
        // we apparently don't need this but im gonna put it back for no good reason
        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.MariaDBDialect");
    }

    private void initSessionFactory() {
        Configuration configuration = new Configuration();
                /* rewriten configuration settings based on the
                 hibernate 6 docs
                 this avoids having to make a hibernate.cfg.xml file
                 which i just think is sort of a waste of time
                 */

        // figure out what backend to use
        if (this.useH2){
            VerandaServer.LOGGER.info("Starting up H2 database");
            this.initH2Database(configuration);
        } else {
            VerandaServer.LOGGER.info("Connecting to MariaDB Database");
            this.initMariaDB(configuration);
        }
        // debug stuff
        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.toString(this.showsql).toLowerCase());
        // idk if we need this even, but it is here
        configuration.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        // set the auto mode
        configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, Action.ACTION_UPDATE);

        // TODO: add config file option for full text search
        configuration.setProperty("hibernate.search.backend.directory.root", new File(this.dbdir, "search").getAbsolutePath());

        // we need to register all of our tables/objects here or else
        // we won't be able to store anything in the DB
        configuration.addAnnotatedClass(Image.class);
        configuration.addAnnotatedClass(ImagePack.class);
        configuration.addAnnotatedClass(Artist.class);

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties()).build();

        this.factory = configuration.buildSessionFactory(serviceRegistry);
    }

    private void doInitialIndex(ConfigFile conf){
        // TODO: check if search is enabled at all
        if (1 == 1){
            // get a session
            Session sesh = this.factory.openSession();
            // make it into a search session
            SearchSession searchsesh = Search.session(sesh);
            // then, setup the indexer
            MassIndexer indexer = searchsesh.massIndexer(Image.class).threadsToLoadObjects(7);
            try {
                indexer.startAndWait();
            } catch (Exception e){
                Main.LOGGER.error("Something broke while trying to index", e);
            }
            // once we're done, close everything
            sesh.close();
        }
    }

    public List<Image> searchImages(String terms){
        // TODO: hibernate has wildcard things, escape them?
        // open a new session
        Session sesh = this.factory.openSession();
        SearchSession search = Search.session(sesh);
        // run the search
        SearchResult<Image> result = search.search(Image.class)
                .where(f -> f.wildcard().fields("filename", "uploaderComments").matching("*" + terms + "*"))
                .fetchAll();
        // convert to list
        List<Image> hits = result.hits();
        // cleanup
        sesh.close();
        return hits;
    }

    /**
     * takes an object and saves it to the database
     * @param obj the object to save
     */
    public void SaveObject(Object obj){
        // get a session
        Session sesh = this.factory.openSession();
        // make a transaction
        Transaction act = sesh.beginTransaction();
        // put it in the database
        sesh.persist(obj);
        // commit
        act.commit();
        // close it
        sesh.close();
        //return id;
    }

    public void UpdateObject(Object obj){
        // get a session
        Session sesh = this.factory.openSession();
        // also get a transaction
        Transaction act = sesh.beginTransaction();
        // merge the thing?
        sesh.merge(obj);
        // commit and yeet
        act.commit();
        sesh.close();
    }

    public <T> T LoadObject(Class<T> type, long id){
        // make a session
        Session sesh = this.factory.openSession();
        // and make a transaction
        Transaction act = sesh.beginTransaction();
        // get our thing
        Object obj = sesh.get(type, id);
        act.commit();
        if (obj == null){
            // we didnt find anything, so return it
            sesh.close();
            return null;
        }
        // if its not null we should have something
        sesh.close();
        return (T) obj;
    }

    public long getCountOfRecords(Class<?> type){
        // get a session
        Session sesh = this.factory.openSession();
        // open a transaction
        Transaction act = sesh.beginTransaction();
        Query q = sesh.createQuery("select count(*) from " + type.getName());
        // get the size of the returned results
        int size = q.getResultList().size();
        // commit it
        act.commit();
        // apparently we cant read from q if the session is closed
        // so we will instead do that now
        long result = (Long) q.uniqueResult();
        // clean up
        sesh.close();
        if (size < 1){
            return -1;
        } else {
            return result;
        }
    }

    /**
     * get how many images exist in a pack
     * @param packid packid to check
     * @return number of images, -1 if none
     */
    public long getImageCountInPack(long packid){
        // get a session
        Session sesh = this.factory.openSession();
        // open a transaction
        Transaction act = sesh.beginTransaction();
        Query<Long> q = sesh.createQuery("select count(*) from " + Image.class.getName() + " where packid=:pid");
        q.setParameter("pid", packid);
        // clean up a little
        act.commit();
        // get our result
        long result = (Long) q.uniqueResult();
        // clean up
        sesh.close();
        if (result <= 0){
            return -1;
        } else {
            return result;
        }
    }

    public Long[] getAllImagesInPack(long packid){
        // get a session
        Session sesh = this.factory.openSession();
        // open a transaction as well
        Transaction act = sesh.beginTransaction();
        // query for what we want
        Query q = sesh.createQuery("select ID from " + Image.class.getName() + " where packid=:pid");
        q.setParameter("pid", packid);
        // how many are there
        List<Long> images = q.getResultList();
        int size = images.size();
        // commit
        act.commit();
        // close the session
        sesh.close();
        // check to make sure we got anything
        if (size < 1){
            return null;
        } else {
            // we have a list of things
            return images.toArray(new Long[0]);
        }

    }

    /**
     * gets a subset of imageids from a pack from the database
     * @param packid packid to get images from
     * @param start starting index for what to return
     * @param amount how many items to return
     * @return list of ids, or null if there wherent any
     */
    public Long[] getSubsetOfImagesInPack(long packid, int start, int amount){
        // init a database connection
        Session sesh = this.factory.openSession();
        Transaction act = sesh.beginTransaction();
        // write a query for what we want
        Query<Long> q = sesh.createQuery("select ID from " + Image.class.getName() + " where packid=:pid");
        q.setParameter("pid", packid);
        // append bounds information
        q.setFirstResult(start);
        q.setMaxResults(amount);
        List<Long> images = q.getResultList();
        int size = images.size();
        // commit
        act.commit();
        // close the session
        sesh.close();
        // check to make sure we got anything
        if (size < 1){
            return null;
        } else {
            // we have a list of things
            return images.toArray(new Long[0]);
        }
    }

    public Long[] getSubsetOfAllImages(long start, long amount){
        // get a connection to the database
        Session sesh = this.factory.openSession();
        Transaction act = sesh.beginTransaction();
        // write the query to get all images
        Query<Long> q = sesh.createQuery("select ID from " + Image.class.getName());
        // setup bounds information
        // TODO: why is this argument just an int? shouldnt it be a long?
        q.setFirstResult(Math.toIntExact(start));
        q.setMaxResults(Math.toIntExact(amount));
        // get the images
        List<Long> images = q.getResultList();
        // commit and close
        act.commit();
        sesh.close();
        // return our list
        return images.toArray(new Long[0]);
    }

    /**
     * gets all image ids in the database
     * @return all image ids as longs
     */
    public Long[] getAllImages(){
        // setup a database connection
        Session sesh = this.factory.openSession();
        Transaction act = sesh.beginTransaction();
        // make the query
        Query<Long> q = sesh.createQuery("select ID from " + Image.class.getName());
        // get our stuff
        List<Long> images = q.getResultList();
        // commit and close
        act.commit();
        sesh.close();
        // return what we just got
        return images.toArray(new Long[0]);
    }

    /**
     * returns a list of longs with every image id by artist
     * returns null if the list has less then 1 entry
     * @param artistid the artist id of which you wish to get all images of
     * @return list of longs
     */
    public Long[] getAllImagesByArtist(long artistid){
        // get a session
        Session sesh = this.factory.openSession();
        // open a transaction as well
        Transaction act = sesh.beginTransaction();
        // query for what we want
        Query q = sesh.createQuery("select ID from " + Image.class.getName() + " where artistid=:aid");
        q.setParameter("aid", artistid);
        // how many are there
        List<Long> images = q.getResultList();
        int size = images.size();
        // commit
        act.commit();
        // close the session
        sesh.close();
        // check to make sure we got anything
        if (size < 1){
            return null;
        } else {
            // we have a list of things
            return images.toArray(new Long[0]);
        }

    }

    /**
     * used for artist pagination. gets a subset of all images by the,
     * @param artistid the artist id of who drew the art
     * @param start start index
     * @param amount how many we want
     * @return list of ids, or null
     */
    public Long[] getSubsetOfImagesByArtist(long artistid, int start, int amount){
        // init a database connection
        Session sesh = this.factory.openSession();
        Transaction act = sesh.beginTransaction();
        // write a query for what we want
        Query<Long> q = sesh.createQuery("select ID from " + Image.class.getName() + " where artistid=:aid");
        q.setParameter("aid", artistid);
        // append bounds information
        q.setFirstResult(start);
        q.setMaxResults(amount);
        List<Long> images = q.getResultList();
        int size = images.size();
        // commit
        act.commit();
        // close the session
        sesh.close();
        // check to make sure we got anything
        if (size < 1){
            return null;
        } else {
            // we have a list of things
            return images.toArray(new Long[0]);
        }
    }

    /**
     * gets the total number of images attributed to an artist
     * @param artid the artist id we are working with
     * @return total number of items, or -1 if there are none
     */
    public long getImageCountByArtist(long artid){
        // get a session
        Session sesh = this.factory.openSession();
        // open a transaction
        Transaction act = sesh.beginTransaction();
        Query<Long> q = sesh.createQuery("select count(*) from " + Image.class.getName() + " where artistid=:aid");
        q.setParameter("aid", artid);
        // clean up a little
        act.commit();
        // get our result
        long result = (Long) q.uniqueResult();
        // clean up
        sesh.close();
        if (result <= 0){
            return -1;
        } else {
            return result;
        }
    }


}
