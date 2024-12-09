package com.eziosoft.verandagal.client.json;

import java.util.ArrayList;

public class PackMetaFile {
    // this file lays out the pack json file
    // which will be read in later for importing
    private String packname;
    private String packdescription;
    private String packfoldername;
    private ArrayList<ImageEntry> images;

    // getters and setters for this object
    public String getPackname() {
        return this.packname;
    }

    public void setPackname(String packname) {
        this.packname = packname;
    }

    public ArrayList<ImageEntry> getImages() {
        return this.images;
    }

    public void setImages(ArrayList<ImageEntry> images) {
        this.images = images;
    }

    public PackMetaFile(){
        // create the array
        this.images = new ArrayList<>();
    }
    public void addImage(ImageEntry img){
        this.images.add(img);
    }

    public String getPackfoldername() {
        return this.packfoldername;
    }

    public void setPackfoldername(String packfoldername) {
        this.packfoldername = packfoldername;
    }

    public String getPackdescription() {
        return this.packdescription;
    }

    public void setPackdescription(String packdescription) {
        this.packdescription = packdescription;
    }
}
