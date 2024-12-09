package com.eziosoft.verandagal.client.utils;

import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Set;

public class ImageFileFilter extends FileFilter {
    // add extensions to this list if you want to support more formats
    public static final Set<String> extensions = Set.of(
            "png", "jpg", "jpeg"
    );

    @Override
    public boolean accept(File f) {
        // allow directories to be shown
        if (f.isDirectory()){
            return true;
        }
        // we need to get the file extension
        String ext = FilenameUtils.getExtension(f.getAbsolutePath()).toLowerCase();
        return extensions.contains(ext);
    }

    @Override
    public String getDescription() {
        return "Supported Image Files (*.png, *.jpg, *.jpeg)";
    }
}
