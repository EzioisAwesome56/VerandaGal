package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.objects.ImagePack;

import java.util.HashMap;

public class PackInfoManager {

    // having to query the DB over and over sucks and is dumb and bad
    // and you should feel bad
    // instead, we shall be using a cacher, kind of
    private final HashMap<Long, ImagePack> packcache;
    private final MainDatabase db;

    public PackInfoManager(MainDatabase maindb){
        // p much just init the cacher
        this.packcache = new HashMap<>();
        // also put the database in its place
        this.db = maindb;
    }

    public ImagePack getImagePack(long id){
        // check if we have it cached
        if (this.packcache.containsKey(id)){
            return this.packcache.get(id);
        }
        // attempt to load a pack with this id
        ImagePack temp = this.db.LoadObject(ImagePack.class, id);
        // check if null
        if (temp == null){
            // we failed at finding anything
            return null;
        }
        // otherwise, we have something
        // cache it
        this.packcache.put(id, temp);
        // return it
        return temp;
    }
}
