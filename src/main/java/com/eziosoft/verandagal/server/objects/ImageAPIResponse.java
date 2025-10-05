package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.database.objects.Image;

import java.util.ArrayList;

public class ImageAPIResponse {
    private Image img;
    private ArrayList<String> urls;
    private boolean has_preview;

    public ImageAPIResponse(){
        // init the urls
        this.urls = new ArrayList<>();
        // default value
        this.has_preview = false;
    }

    public Image getImg() {
        return img;
    }

    public void setImg(Image img) {
        this.img = img;
    }
    public void addURL(String url){
        this.urls.add(url);
    }
    public void enable_preview(){
        this.has_preview = true;
    }
}
