package com.eziosoft.verandagal.client.dialogs;

import org.apache.commons.io.FilenameUtils;
import com.eziosoft.verandagal.client.utils.ClientUtils;
import com.eziosoft.verandagal.client.utils.ImageFileFilter;
import com.eziosoft.verandagal.client.utils.ImageProcessor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class guiImageProcessorSelector {

    private JPanel content;
    private JList imageSelectionList;
    private JButton addSingleImageButton;
    private JButton addFolderButton;
    private JButton removeImageButton;
    private JButton processAllButton;
    private JFrame frame;
    private final ImageFileFilter filter = new ImageFileFilter();
    private DefaultListModel list_items;

    public guiImageProcessorSelector() {
        // this always goes here
        this.frame = new JFrame("Image Selection Dialog");
        this.list_items = new DefaultListModel();
        // set the list to use this
        this.imageSelectionList.setModel(this.list_items);
        // also make sure the list can only have 1 item selected at a time
        this.imageSelectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // action listeners for all the buttons
        this.addSingleImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open a dialog and ask for a file
                File img = ClientUtils.browseForFileFiltered("Open Image", filter);
                if (img == null){
                    ImageProcessor.log.warn("File picker returned null, not doing anything");
                    return;
                }
                // if its not null, add it's absolute path to the list
                list_items.addElement(img.getAbsolutePath());
                // and now we're done
            }
        });
        this.addFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open a dialog and ask the user for a folder
                File folder = ClientUtils.browseForDirectory("Select folder with images");
                // iterate thru the entire folder to find content
                int count = 0;
                for (File f : folder.listFiles()){
                    // skip directories
                    if (f.isDirectory()){
                        continue;
                    }
                    String ext = FilenameUtils.getExtension(f.getAbsolutePath());
                    if (ImageFileFilter.extensions.contains(ext)){
                        // add the file to the list model
                        list_items.addElement(f.getAbsolutePath());
                        count++;
                    }
                }
                ImageProcessor.log.debug("Added {} images from folder", count);
            }
        });
        this.removeImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get the currently selected item
                int sel = imageSelectionList.getSelectedIndex();
                // remove it from the actual list model
                list_items.removeElementAt(sel);
                // in theory this should be all we need!
            }
        });
        this.processAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // break the main thread out
                synchronized (frame){
                    frame.notify();
                }
            }
        });
    }

    public ArrayList<String> openDialog() {
        // last minute things we need to do to open the window
        this.frame.setContentPane(this.content);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.pack();
        this.frame.setVisible(true);
        // make the main thread wait
        try {
            synchronized (this.frame){
                this.frame.wait();
            }
        } catch (Exception e){
            ImageProcessor.log.error("Something broke while waiting on ui", e);
            this.frame.dispose();
            System.exit(1);
        }
        // ok now we're out of that hell scape
        this.frame.setVisible(false);
        // make our output format
        ArrayList<String> out = new ArrayList<>();
        // now we have to get everything out of the default list model
        for (int x = 0; x < this.list_items.size(); x++){
            out.add((String) this.list_items.get(x));
        }
        // dispose of the frame
        this.frame.dispose();
        // return the array
        return out;
    }
}
