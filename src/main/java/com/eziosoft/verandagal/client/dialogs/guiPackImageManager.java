package com.eziosoft.verandagal.client.dialogs;

import org.apache.commons.io.FilenameUtils;
import com.eziosoft.verandagal.client.VerandaClient;
import com.eziosoft.verandagal.client.json.ImageEntry;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;
import com.eziosoft.verandagal.client.json.PackMetaFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class guiPackImageManager {

    private JPanel content;
    private JLabel packNameLabel;
    private JList imageList;
    private JButton addImagesButton;
    private JButton removeImageButton;
    private JButton finishButton;
    private JButton editImageInfoButton;
    private JFrame frame;
    private int exit_code;
    private DefaultListModel listcontent;
    private PackMetaFile metafile;
    private File outfolder;
    private ImportableArtistsFile artists;

    public PackMetaFile openDialog(){
        // do some last minute stuff
        this.frame.setContentPane(this.content);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.pack();
        this.frame.setVisible(true);
        try {
            synchronized (this.frame){
                this.frame.wait();
            }
        } catch (Exception e){
            VerandaClient.log.error("Error while waiting for ui", e);
            this.frame.dispose();
            return null;
        }
        // hide the window
        this.frame.setVisible(false);
        // update the pack meta file if required
        if (this.exit_code == 0 || this.exit_code == 1){
            // we need to make a new arraylist of all of the entires
            ArrayList<ImageEntry> temp = new ArrayList<>();
            // we need to use a for loop bc this shit is cringe
            for (int x = 0; x < this.listcontent.size(); x++){
                temp.add((ImageEntry) this.listcontent.get(x));
            }
            // then update the metafile
            this.metafile.setImages(temp);
        }
        // get rid of the frame
        this.frame.dispose();
        // return the stored metafile object
        return this.metafile;
    }

    public int getExit_code(){
        return this.exit_code;
    }

    public guiPackImageManager(PackMetaFile file, File out, ImportableArtistsFile art) {
        // basic init goes here
        this.frame = new JFrame("Pack Image Manager");
        this.outfolder = out;
        this.artists = art;
        // set some basic properties
        this.packNameLabel.setText(file.getPackname());
        // populate the list with images
        this.listcontent = new DefaultListModel<>();
        for (ImageEntry ent : file.getImages()){
            this.listcontent.addElement(ent);
        }
        // set the list to be on the uhhh jlist
        this.imageList.setModel(this.listcontent);
        // store this shit for later, we might need it
        this.metafile = file;

        // action listeners for various things on the form
        addImagesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 1;
                // notify the main thread of waking up
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        removeImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get current selected index
                int sel = imageList.getSelectedIndex();
                // get the image Entry that is selected
                ImageEntry ent = (ImageEntry) listcontent.getElementAt(sel);
                // create files for the image and thumbnail
                File realimg = new File(outfolder, "images/" + ent.getFilename());
                if (realimg.exists()){
                    // delete it
                    realimg.delete();
                }
                // then also delete the thumbnail
                File thumb = new File(outfolder, "thumbs/" + FilenameUtils.getBaseName(ent.getFilename()) + ".jpg");
                if (thumb.exists()){
                    // also delete it
                    thumb.delete();
                }
                // remove the entry from the list
                listcontent.removeElementAt(sel);
            }
        });
        finishButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 0;
                // notify the main thread of waking up
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        editImageInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set the exit code
                exit_code = 2;
                // also set a special variable for what the selected index
                sel_index = imageList.getSelectedIndex();
                // unlock the main thread
                synchronized (frame){
                    frame.notify();
                }


            }
        });
    }

    private int sel_index;
    public int getSelected_index(){
        return this.sel_index;
    }
}
