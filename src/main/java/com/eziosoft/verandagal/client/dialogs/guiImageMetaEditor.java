package com.eziosoft.verandagal.client.dialogs;

import org.apache.commons.io.FilenameUtils;
import com.eziosoft.verandagal.client.VerandaClient;
import com.eziosoft.verandagal.client.json.ArtistEntry;
import com.eziosoft.verandagal.client.json.ImageEntry;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;
import com.eziosoft.verandagal.server.utils.ServerUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class guiImageMetaEditor {

    private JPanel content;
    private JLabel TopLabel;
    private JLabel ExtraInfo;
    private JLabel ImagePreview;
    private JLabel filenameLabel;
    private JComboBox ArtistDropdown;
    private JComboBox ratingDropdown;
    private JTextField sourceURLfield;
    private JLabel resolutionLabel;
    private JTextArea commentsTextBox;
    private JButton addImageButton;
    private JButton skipImageButton;
    private JButton finishButton;
    private JButton cancelButton;
    private JCheckBox isAIImageCheckBox;
    private JFrame frame;
    private final ImportableArtistsFile artistsfile;
    // additional code for handling button presses n shit
    private int exit_code;
    private String resolution;

    public ImageEntry OpenDialog() {
        this.frame.setContentPane(this.content);
        this.frame.setLocationRelativeTo(null);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.pack();
        this.frame.setVisible(true);
        // wait until thread is unlocked
        try {
            synchronized (this.frame){
                this.frame.wait();
            }
        } catch (Exception e){}
        // hide the frame
        this.frame.setVisible(false);
        // make a new object
        ImageEntry image = null;
        // check exit code
        // 1 is the add image button, 2 is the finish button
        if (exit_code == 1 || exit_code == 2){
            // fill in the object
            image = new ImageEntry();
            // we can just get the selected index from the combobox, we will have to deal with this later mind
            image.setArtistid(Long.parseLong(String.valueOf(this.ArtistDropdown.getSelectedIndex() + 1)));
            image.setRating(this.ratingDropdown.getSelectedIndex());
            if (this.sourceURLfield.getText().isEmpty()){
                image.setOriginalUrl("No URL was provided");
            } else {
                image.setOriginalUrl(this.sourceURLfield.getText());
            }
            // we have set the resolution eariler, so just get that now
            image.setResolution(this.resolution);
            if (this.commentsTextBox.getText().isEmpty()){
                image.setComments("No uploader comments where provided during pack creation");
            } else {
                image.setComments(this.commentsTextBox.getText());
            }
            // set the ai value from the check box
            image.setAiimage(this.isAIImageCheckBox.isSelected());

            // we can cheat and set the filename from the label
            image.setFilename(this.filenameLabel.getText());
        }
        // dispose of the frame
        this.frame.dispose();
        return image;
    }

    public int getExit_code(){
        return this.exit_code;
    }

    public void SetupEditor(ImageEntry ent){
        // we actually have some use for this, wow!
        this.TopLabel.setText("Edit Image Metadata");
        this.ExtraInfo.setText("Existing information loaded");
        // fill in all the existing info
        this.ArtistDropdown.setSelectedIndex(Integer.parseInt(Long.toString(ent.getArtistid())) - 1);
        this.ratingDropdown.setSelectedIndex(ent.getRating());
        this.sourceURLfield.setText(ent.getOriginalUrl());
        this.commentsTextBox.setText(ent.getComments());
        // new: ai field
        this.isAIImageCheckBox.setSelected(ent.isAiimage());
        // should be good to go now i think
    }

    public guiImageMetaEditor(String filename, ImportableArtistsFile file){
        // disable cache because its cringe
        ImageIO.setUseCache(false);
        // make our frame here
        this.frame = new JFrame("Image Metadata Editor");
        // set the filename
        this.filenameLabel.setText(FilenameUtils.getName(filename));
        this.artistsfile = file;
        // fill out the combo box
        for (Map.Entry<Long, ArtistEntry> artist : this.artistsfile.getArtists().entrySet()){
            this.ArtistDropdown.addItem(artist.getValue());
        }
        // fill out the rating combo box
        for (int x = 0; x < ServerUtils.max_rating + 1; x++){
            this.ratingDropdown.addItem(ServerUtils.getRatingText(x));
        }
        // we need to load our image now
        BufferedImage ourimage = null;
        try {
            ourimage = ImageIO.read(new File(filename));
        } catch (IOException e){
            VerandaClient.log.error("Error while trying to load image", e);
            this.frame.dispose();
            throw new RuntimeException("Error while trying to load image", e);
        }
        // once loaded, store the resolution
        this.resolution = Integer.toString(ourimage.getWidth()) + "x" + Integer.toString(ourimage.getHeight());
        // then set the label with that text
        this.resolutionLabel.setText(this.resolution);
        // display the image
        this.ImagePreview.setText("");
        this.ImagePreview.setIcon(new ImageIcon(ourimage.getScaledInstance(500, 500, BufferedImage.SCALE_FAST)));
        // action listeners for the various buttons we will need later
        this.addImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set the exit code to 1
                exit_code = 1;
                // unlock the main thread
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set exit code to 0
                exit_code = 0;
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        this.finishButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set exit code to 2
                // stops processing the directory
                exit_code = 2;
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        this.skipImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set exit code to 3
                exit_code = 3;
                synchronized (frame){
                    frame.notify();
                }
            }
        });
    }
}
