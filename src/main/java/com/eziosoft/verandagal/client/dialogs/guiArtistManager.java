package com.eziosoft.verandagal.client.dialogs;

import com.eziosoft.verandagal.client.json.ArtistEntry;
import com.eziosoft.verandagal.client.json.ImportableArtistsFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class guiArtistManager {
    private JPanel content;
    private JTextField modeSelectField;
    private JComboBox ArtistDropDown;
    private JButton saveToLocalButton;
    private JButton editArtistButton;
    private JButton deleteArtistButton;
    private JButton createArtistButton;
    private JButton exitButton;
    private JFrame frame;
    private int exit_code;
    private int selected_artist = 0;

    public int getSelected_artist() {
        return selected_artist;
    }

    public guiArtistManager(ImportableArtistsFile file) {
        // we need this here too
        this.frame = new JFrame("Local Artist Manager");
        // we need to popular some options first
        this.modeSelectField.setText(Integer.toString(file.getMode()));
        // now we need to fill in the combobox
        for (Map.Entry<Long, ArtistEntry> ent : file.getArtists().entrySet()){
            this.ArtistDropDown.addItem(ent.getValue());
        }

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 0;
                synchronized (frame){
                    frame.notify();
                }
                frame.setVisible(false);
            }
        });
        createArtistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 1;
                synchronized (frame){
                    frame.notify();
                }
                frame.setVisible(false);
            }
        });
        saveToLocalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 2;
                synchronized (frame){
                    frame.notify();
                }
                frame.setVisible(false);
            }
        });
        editArtistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 3;
                // also set the selected artist id
                // we add one bc the list is 1-index, but the box is 0 index
                selected_artist = ArtistDropDown.getSelectedIndex() + 1;
                synchronized (frame){
                    frame.notify();
                }
                frame.setVisible(false);
            }
        });
        deleteArtistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit_code = 4;
                // also set the selected artist id
                // we add one bc the list is 1-index, but the box is 0 index
                selected_artist = ArtistDropDown.getSelectedIndex() + 1;
                synchronized (frame){
                    frame.notify();
                }
                frame.setVisible(false);
            }
        });
    }

    public int openDialog() {
        this.frame.setContentPane(this.content);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.pack();
        this.frame.setVisible(true);
        synchronized (this.frame){
            try {
                this.frame.wait();
            } catch (Exception e){}
        }
        this.frame.dispose();
        return this.exit_code;
    }
}
