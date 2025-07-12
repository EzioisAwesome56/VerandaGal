package com.eziosoft.verandagal.client.objects;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class ImageImportJob {
    /**
     * simple object to hold information required for image import-related jobs
     */

    private final File targetfile;
    private final File targetPackDir;
    private final long id;

    public ImageImportJob(File target, long id, File packdir){
        this.targetfile = target;
        this.id = id;
        this.targetPackDir = packdir;
    }

    public File getTargetfile() {
        return this.targetfile;
    }
    public File getTargetPackDir() {
        return this.targetPackDir;
    }
    public long getId() {
        return this.id;
    }
    public String getFilename(){
        return FilenameUtils.getName(this.targetfile.getName());
    }
}
