package com.eziosoft.verandagal.server.objects;

import com.eziosoft.verandagal.server.VerandaServer;

public class SessionObject {

    // set all of the values here
    //  this will get gson'd later
    private boolean show_normal;
    private boolean show_spicy;
    private boolean show_extra_spicy;
    private boolean show_ai;
    private int itemsperrow;
    private boolean use_pagination;
    private int items_per_page;

    public boolean isShow_ai() {
        return show_ai;
    }

    public boolean isShow_extra_spicy() {
        return show_extra_spicy;
    }

    public boolean isShow_normal() {
        return show_normal;
    }

    public boolean isShow_spicy() {
        return show_spicy;
    }

    public void setShow_ai(boolean show_ai) {
        this.show_ai = show_ai;
    }

    public void setShow_extra_spicy(boolean show_extra_spicy) {
        this.show_extra_spicy = show_extra_spicy;
    }

    public void setShow_normal(boolean show_normal) {
        this.show_normal = show_normal;
    }

    public void setShow_spicy(boolean show_spicy) {
        this.show_spicy = show_spicy;
    }

    public int getItemsperrow() {
        return this.itemsperrow;
    }

    public void setItemsperrow(int itemsperrow) {
        this.itemsperrow = itemsperrow;
    }

    public boolean isUse_pagination() {
        return this.use_pagination;
    }

    public void setUse_pagination(boolean use_pagination) {
        this.use_pagination = use_pagination;
    }

    public int getItems_per_page() {
        return this.items_per_page;
    }

    public void setItems_per_page(int items_per_page) {
        this.items_per_page = items_per_page;
    }

    public void setDefaults(){
        // set the default settings
        this.show_ai = false;
        this.show_normal = false;
        this.show_spicy = false;
        this.show_extra_spicy = false;
        // set the default to the one in the config file
        this.itemsperrow = VerandaServer.configFile.getItemsPerRow();
        this.use_pagination = VerandaServer.configFile.isPageinationdefault();
        this.items_per_page = VerandaServer.configFile.getItemsperpage();
    }
}
