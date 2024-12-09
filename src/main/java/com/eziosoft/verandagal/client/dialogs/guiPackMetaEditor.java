package com.eziosoft.verandagal.client.dialogs;

import com.eziosoft.verandagal.client.VerandaClient;
import com.eziosoft.verandagal.client.json.PackMetaFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class guiPackMetaEditor {

    private JPanel content;
    private JLabel packNameLabel;
    private JTextField packNameField;
    private JTextField packFolderField;
    private JTextArea packDescriptionArea;
    private JButton saveMetaButton;
    private JButton cancelButton;
    // extra stuff to make code flow suck less
    private JFrame frame;
    private int exit_code;

    public PackMetaFile openDialog() {
        // do the final things we need to do before we open the window
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
            VerandaClient.log.error("Something went wrong during dialog wait", e);
            this.frame.dispose();
            return null;
        }
        // hide the window
        this.frame.setVisible(false);
        // setup our object
        PackMetaFile meta = null;
        // check exit code
        // TODO: none of the these values can be null, so
        //      we should probably do some kind of checking
        //      either here or in the caller
        if (this.exit_code == 1){
            // build object
            meta = new PackMetaFile();
            // pack name
            meta.setPackname(this.packNameField.getText());
            // pack description
            meta.setPackdescription(this.packDescriptionArea.getText());
            // folder name
            meta.setPackfoldername(this.packFolderField.getText());
        }
        // get rid of the window before we return
        this.frame.dispose();
        // return the object
        return meta;
    }

    public guiPackMetaEditor(PackMetaFile file){
        /* this will also serve as the editor setup, since it would just need passing in
             one of these meta objects anyway!
         */
        // init some stuff down here for ease of use
        this.frame = new JFrame("Pack Meta Editor");
        // set the label with some simple logic
        if (file.getPackname() == null){
            this.packNameLabel.setText("A new pack");
        } else if (file.getPackname().isEmpty()) {
            this.packNameLabel.setText("A new pack");
        } else {
            this.packNameLabel.setText(file.getPackname());
            // how the fuck did i forget this
            this.packNameField.setText(file.getPackname());
        }
        // also set the description and folder
        if (file.getPackdescription() == null){
            this.packDescriptionArea.setText("");
        } else if (file.getPackdescription().isEmpty()) {
            this.packDescriptionArea.setText("");
        } else {
            this.packDescriptionArea.setText(file.getPackdescription());
        }
        // ONE MORE TIME WOOO
        if (file.getPackfoldername() == null){
            this.packFolderField.setText("");
        } else if (file.getPackfoldername().isEmpty()){
            this.packFolderField.setText("");
        } else {
            this.packFolderField.setText(file.getPackfoldername());
        }
        // action listeners for the buttons we provide on the dialog
        saveMetaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set exit code
                exit_code = 1;
                // wake up main thread
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set exit code
                exit_code = 0;
                // wake up main thread
                synchronized (frame){
                    frame.notify();
                }
            }
        });
    }
}
