package com.eziosoft.verandagal.utils;

public class ConfigFile {
    // we can define values for our main config file here
    private final String serviceName;
    private final String databaseDir;
    private final String imageDir;
    private final String imagePreviewDir;
    private final int webPort;
    private final boolean showStockFAQ;
    private final boolean showUserFAQ;
    private final boolean showStockMain;
    private final boolean showUserMain;
    private final int MaxNumbThreads;
    private final boolean EnableAPI;
    private final boolean ShowSQL;
    private final int ItemsPerRow;
    private final boolean ShowDebugLog;
    private final boolean DontUsePreviews;
    private final boolean pageinationdefault;
    private final int itemsperpage;

    public boolean isShowStockFAQ() {
        return this.showStockFAQ;
    }

    public boolean isShowUserFAQ() {
        return this.showUserFAQ;
    }

    public int getWebPort() {
        return this.webPort;
    }

    public String getDatabaseDir() {
        return this.databaseDir;
    }

    public String getImageDir() {
        return this.imageDir;
    }
    public String getImagePreviewDir() { return this.imagePreviewDir; }

    public String getServiceName() {
        return this.serviceName;
    }

    public boolean isShowStockMain() {
        return this.showStockMain;
    }

    public boolean isShowUserMain() {
        return this.showUserMain;
    }

    public boolean isEnableAPI() {
        return this.EnableAPI;
    }

    public int getMaxNumbThreads() {
        return this.MaxNumbThreads;
    }
    public boolean isShowSQL(){
        return this.ShowSQL;
    }

    public int getItemsPerRow() {
        return this.ItemsPerRow;
    }

    public boolean isShowDebugLog() {
        return this.ShowDebugLog;
    }

    public boolean isDontUsePreviews() {
        return this.DontUsePreviews;
    }
    public boolean isPageinationdefault() {
        return this.pageinationdefault;
    }
    public int getItemsperpage() {
        return this.itemsperpage;
    }

    public ConfigFile(){
        // make a blank config file with default values
        this.serviceName = "VerandaGal";
        this.databaseDir = "/folder";
        this.imageDir = "/folder";
        this.imagePreviewDir = "/folder";
        this.webPort = 6969;
        this.showStockFAQ = true;
        this.showUserFAQ = true;
        this.showStockMain = true;
        this.showUserMain = true;
        this.MaxNumbThreads = 10;
        this.EnableAPI = true;
        this.ShowSQL = false;
        this.ItemsPerRow = 7;
        this.ShowDebugLog = false;
        this.DontUsePreviews = false;
        this.pageinationdefault = false;
        this.itemsperpage = 40;
    }
}
