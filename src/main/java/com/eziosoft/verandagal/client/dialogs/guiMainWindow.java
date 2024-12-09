package com.eziosoft.verandagal.client.dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class guiMainWindow {
    private JFrame frame;

    public int openDialog() {
        this.frame.setContentPane(this.content);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.pack();
        this.frame.setVisible(true);
        try {
            synchronized (this.frame){
                frame.wait();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        // ok the frame has let us go
        // see if the actionlist has anything
        String res;
        // patch to make sure clicking execute without picking an action
        // wont just break everything
        try {
            res = this.ActionList.getSelection().getActionCommand();
        } catch (Exception e){
            this.frame.dispose();
            return -1;
        }
        this.frame.dispose();
        return switch (res) {
            case "exportartist" -> 0;
            case "importartist" -> 1;
            case "artistmanager" -> 2;
            case "createpack" -> 3;
            case "importpack" -> 4;
            default -> Integer.parseInt(res);
        };
    }

    private JPanel content;
    private JRadioButton exportArtistsRadioButton;
    private JRadioButton importArtistsRadioButton;
    private JRadioButton artistManagerRadioButton;
    private JRadioButton createPackRadioButton;
    private JRadioButton importPackRadioButton;
    private JButton executeActionButton;
    private JRadioButton ZIPPackRadioButton;
    private JRadioButton editPackRadioButton;
    private ButtonGroup ActionList;

    public guiMainWindow() {
        // define our main window here
        this.frame = new JFrame("VerandaGal Client GUI");
        // set all of our action commands here
        this.exportArtistsRadioButton.setActionCommand("exportartist");
        this.importArtistsRadioButton.setActionCommand("importartist");
        this.artistManagerRadioButton.setActionCommand("artistmanager");
        this.createPackRadioButton.setActionCommand("createpack");
        this.importPackRadioButton.setActionCommand("importpack");
        this.ZIPPackRadioButton.setActionCommand("5");
        this.editPackRadioButton.setActionCommand("6");
        // event listeners
        executeActionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // inform the main thread its time to close
                synchronized (frame){
                    frame.notify();
                }
                // then get rid of it
                frame.setVisible(false);
            }
        });
    }
}
