package com.eziosoft.verandagal.client.json;

import java.util.HashMap;

public class ImportableArtistsFile {

    /*
        this json file will contain instructions for importing
        along side raw arists
        namely: MODE;
        mode 0: import, add to existing DB
        mode 1: update existing records
        mode 2: drop table and replace contents with this file
     */
    private int mode;
    // the actual list of artists starts here
    private HashMap<Long, ArtistEntry> artists;

    public HashMap<Long, ArtistEntry> getArtists() {
        return this.artists;
    }
    public void addArtist(long id, ArtistEntry ent){
        this.artists.put(id, ent);
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
    public ImportableArtistsFile(){
        this.mode = 0;
        this.artists = new HashMap<>();
    }
}
