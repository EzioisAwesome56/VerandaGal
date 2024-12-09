package com.eziosoft.verandagal.client.json;

public class ArtistEntry {

    private String name;
    private String[] urls;
    private String notes;

    public String getNotes() {
        return notes;
    }

    public String getName() {
        return name;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getUrls() {
        return urls;
    }

    @Override
    public String toString() {
        // this is only really used for the comboboxes
        // so just return the artist name
        return this.name;
    }
}
