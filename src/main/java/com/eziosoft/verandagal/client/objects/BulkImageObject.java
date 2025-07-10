package com.eziosoft.verandagal.client.objects;

public class BulkImageObject {

    // outlines a basic image for the bulk image importer to handle
    private final String filename;
    private final int sitesource;
    private final int rating;
    private final boolean ai;
    private final long artistid;

    public BulkImageObject(String name, int source, boolean ai, int rating, long artid){
        this.filename = name;
        this.sitesource = source;
        this.rating = rating;
        this.ai = ai;
        this.artistid = artid;
    }

    public long getArtistid() {
        return artistid;
    }

    public String getFilename() {
        return this.filename;
    }

    public int getSitesource() {
        return this.sitesource;
    }

    public boolean isAi() {
        return this.ai;
    }

    public int getRating() {
        return this.rating;
    }
}
