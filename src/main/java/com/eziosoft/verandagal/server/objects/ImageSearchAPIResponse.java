package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.database.MainDatabase;
import com.eziosoft.verandagal.database.objects.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageSearchAPIResponse {

    private String search_terms;
    private ArrayList<Long> image_list;
    private int found_items;

    public ImageSearchAPIResponse(String search_terms, MainDatabase db){
        this.search_terms = search_terms;
        this.image_list = new ArrayList<>();
        // perform the search
        List<Image> found = db.searchImages(search_terms);
        if (found.size() < 1){
            this.found_items = 0;
            return;
        } else {
            this.found_items = found.size();
        }
        // otherwise, start adding ids to the list
        for (Image img : found){
            this.image_list.add(img.getId());
        }
        Collections.sort(this.image_list);
    }


}
