package com.eziosoft.verandagal.client.dialogs;

import com.eziosoft.verandagal.client.json.ArtistEntry;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

public class guiArtistCreation {

    private JPanel conent;
    private JTextField ArtistNameField;
    private JTextField Artisturlfield;
    private JLabel URLStatusLabel;
    private JButton addURLButton;
    private JLabel urlcount;
    private JTextArea notesArea;
    private JButton saveArtistButton;
    private JButton cancelButton;
    private JComboBox SetURLsCombo;
    private JButton deleteURLButton;
    private JLabel TitleLabel;
    private JFrame frame;
    private ArrayList<String> urls;
    private int exit_code;

    public ArtistEntry openDialog() {
        // last minute init stuff
        this.urlcount.setText("URLs: " + Integer.toString(this.urls.size()));
        // populate the combo box
        for (String url : this.urls){
            this.SetURLsCombo.addItem(url);
        }
        // ok now we can open the window
        this.frame.setContentPane(this.conent);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.pack();
        this.frame.setVisible(true);
        // wait for something to unlock the thread
        try {
            synchronized (this.frame){
                this.frame.wait();
            }
        } catch (Exception e){
            this.frame.dispose();
            return null;
        }
        this.frame.setVisible(false);
        // once the thread has unlocked, do some checks
        ArtistEntry artist = null;
        if (exit_code == 1) {
            // make a new artist object
            artist = new ArtistEntry();
            artist.setName(this.ArtistNameField.getText());
            artist.setNotes(this.notesArea.getText());
            artist.setUrls(this.urls.toArray(new String[0]));
        }
        // clean up and return
        this.frame.dispose();
        return artist;
    }

    public void SetupEditor(ArtistEntry artist){
        // change the title to editor
        this.TitleLabel.setText("Edit Artist");
        // fill in the editor fields
        this.ArtistNameField.setText(artist.getName());
        this.notesArea.append(artist.getNotes());
        Collections.addAll(this.urls, artist.getUrls());

    }

    public guiArtistCreation() {
        // set up shit
        this.frame = new JFrame("Create new Artist");
        this.urls = new ArrayList<>();
        // event listeners for the buttons that you click on and they go wow a button!
        addURLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // when this button is clicked, we want to add the URL to the array list
                // get the current text
                String url = Artisturlfield.getText();
                // clear the box
                Artisturlfield.setText("");
                // add it to the array
                urls.add(url);
                // add it to the combo box
                SetURLsCombo.addItem(url);
                // update things
                urlcount.setText("URLs: " + Integer.toString(urls.size()));
                URLStatusLabel.setText("Added URL");
                //frame.update(frame.getGraphics());
            }
        });
        saveArtistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set the exit code
                exit_code = 1;
                // notify the thread
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // set the exit code
                exit_code = 0;
                // notify the thread
                synchronized (frame){
                    frame.notify();
                }
            }
        });
        deleteURLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get the current selection from the combobox
                String url = (String) SetURLsCombo.getSelectedItem();
                // remove it from the combobox
                int index = SetURLsCombo.getSelectedIndex();
                SetURLsCombo.setSelectedIndex(index - 1);
                SetURLsCombo.removeItemAt(index);
                SetURLsCombo.updateUI();
                // remove it from the arraylist
                urls.remove(url);
                // update labels
                urlcount.setText("URLs: " + Integer.toString(urls.size()));
                URLStatusLabel.setText("Deleted URL");
            }
        });
    }
}
