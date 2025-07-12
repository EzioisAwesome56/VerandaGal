package com.eziosoft.verandagal.database;

import com.eziosoft.verandagal.utils.ConfigFile;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import com.eziosoft.verandagal.database.objects.Thumbnail;

public class ThumbnailStore {
    // this will be an object instead of a static class
    // no reason, thought it would be funny
    private SessionFactory factory;
    private final String dbdir;
    private final boolean showsql;

    private void initSessionFactory() {
        Configuration configuration = new Configuration();
        // set this to use the sqlite db driver
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.sqlite.JDBC");
        // set the path for where the database file will be stored
        configuration.setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:sqlite:" + this.dbdir + "/thumbnails.db");
        // debug stuff
        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.toString(this.showsql).toLowerCase());
        // idk if we need this even, but it is here
        configuration.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        // set the auto mode
        configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, Action.ACTION_UPDATE);
        // we apparently don't need this but im gonna put it back for no good reason
        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.community.dialect.SQLiteDialect");

        // we need to register all of our tables/objects here or else
        // we won't be able to store anything in the DB
        configuration.addAnnotatedClass(Thumbnail.class);

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties()).build();

        this.factory = configuration.buildSessionFactory(serviceRegistry);
    }

    public void close(){
        this.factory.close();
    }

    public void SaveThumbnail(Thumbnail obj){
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
    }

    /**
     * use this to save a thumbnail that already has an ID attached to its object
     * FIXME: oh my god somebody pelase make this work
     * @param obj thumbnail with manually set ID
     */
    public void MergeThumbnail(Thumbnail obj){
        // create session
        StatelessSession sesh = this.factory.openStatelessSession();
        // open a transaction
        Transaction act = sesh.beginTransaction();
        // we have to use merge instead of save
        sesh.upsert(obj);
        // commit and close it
        act.commit();
        sesh.close();
    }

    public Thumbnail LoadThumbnail(long id){
        // make a session
        Session sesh = this.factory.openSession();
        // and make a transaction
        Transaction act = sesh.beginTransaction();
        // get our thing
        Thumbnail obj = sesh.get(Thumbnail.class, id);
        act.commit();
        if (obj == null){
            // we didnt find anything, so return it
            sesh.close();
            return null;
        }
        // if its not null we should have something
        sesh.close();
        return obj;
    }

    public ThumbnailStore(ConfigFile config){
        // pretty much just init the db backend
        this.dbdir = config.getDatabaseDir();
        this.showsql = config.isShowSQL();
        this.initSessionFactory();
    }

}
