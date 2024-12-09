package com.eziosoft.verandagal.client.json;

public class ImageEntry {
    // file to layout how an image is stored in json
    private String filename;
    // we don't need packid as all images come in their own pack
    private long artistid;
    private int rating;
    // date isnt relevent as its only needed on the server
    private String originalUrl;
    private String resolution;
    private String comments;
    // friend told me this should be its own property, instead of just rolled into ratings
    // so now it is
    private boolean aiimage;

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public long getArtistid() {
        return artistid;
    }

    public void setArtistid(long artistid) {
        this.artistid = artistid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isAiimage() {
        return aiimage;
    }

    public void setAiimage(boolean aiimage) {
        this.aiimage = aiimage;
    }
    // fix for incorrect display in JList elements

    @Override
    public String toString() {
        return this.filename;
    }
}
