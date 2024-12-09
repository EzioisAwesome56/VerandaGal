package com.eziosoft.verandagal.client.utils;

import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class SimpleFileFilter extends FileFilter {

    private final ArrayList<String> extensions;
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()){
            return true;
        }
        return this.extensions.contains(FilenameUtils.getExtension(f.getAbsolutePath()).toLowerCase());
    }

    @Override
    public String getDescription() {
        StringBuilder b = new StringBuilder();
        b.append("Supported files (");
        for (String s : this.extensions){
            b.append(s);
            b.append(" ");
        }
        b.append(")");
        return b.toString();
    }

    public SimpleFileFilter(String... ex){
        // init the array
        this.extensions = new ArrayList<>();
        Collections.addAll(this.extensions, ex);
    }
}
