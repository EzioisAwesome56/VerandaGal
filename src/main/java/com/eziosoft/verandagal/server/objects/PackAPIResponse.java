package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.database.objects.ImagePack;

import java.util.ArrayList;

public class PackAPIResponse {
    private ImagePack pack;
    private ArrayList<Long> images;

    public ImagePack getPack() {
        return pack;
    }

    public void setPack(ImagePack pack) {
        this.pack = pack;
    }

    public ArrayList<Long> getImages() {
        return this.images;
    }
    public void addimage(long id){
        this.images.add(id);
    }

    public PackAPIResponse(){
        // init the thing
        this.images = new ArrayList<>();
    }
}
