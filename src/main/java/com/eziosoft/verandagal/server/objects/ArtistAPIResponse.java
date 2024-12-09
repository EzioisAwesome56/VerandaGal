package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.database.objects.Artist;

import java.util.ArrayList;

public class ArtistAPIResponse {
    private Artist artist;
    private ArrayList<Long> images;

    public ArtistAPIResponse(){
        this.images = new ArrayList<>();
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }
    public void addImage(long id){
        this.images.add(id);
    }
}
