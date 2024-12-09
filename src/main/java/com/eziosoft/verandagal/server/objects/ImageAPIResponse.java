package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.database.objects.Image;

import java.util.ArrayList;

public class ImageAPIResponse {
    private Image img;
    private ArrayList<String> urls;

    public ImageAPIResponse(){
        // init the urls
        this.urls = new ArrayList<>();
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
}
