package com.eziosoft.verandagal.database.objects;

import jakarta.persistence.*;

@Entity
@Table(name="Artists", uniqueConstraints = {@UniqueConstraint(columnNames = {"ID"})})
public class Artist {

    // define all attributes for what needs to go in the table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, unique = true)
    private long id;
    @Column(name = "ArtistName", nullable = false)
    private String name;
    @Column(name = "ArtistURLs", nullable = false, columnDefinition = "VARBINARY")
    private String[] urls;
    @Column(name = "Notes", nullable = false, columnDefinition = "TEXT")
    private String notes;

    // getters and setters for the object
    public void setName(String name) {
        this.name = name;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public void setUrls(String[] urls) {
        this.urls = urls;
    }
    public String getName() {
        return name;
    }
    public long getId() {
        return id;
    }
    public String getNotes() {
        return notes;
    }
    public String[] getUrls() {
        return urls;
    }
    // todo: i dont think i have a use case for this
    //      if i run into one, change this
    public void setId(long id) {
        throw new UnsupportedOperationException("You are not allowed to do this!");
    }
}
