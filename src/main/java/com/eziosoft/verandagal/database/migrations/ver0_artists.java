package com.eziosoft.verandagal.database.migrations;

import jakarta.persistence.*;

@Entity
@Table(name="Artists", uniqueConstraints = {@UniqueConstraint(columnNames = {"ID"})})
public class ver0_artists {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, unique = true)
    private long id;
    @Column(name = "ArtistName", nullable = false)
    private String name;
    @Column(name = "ArtistURLs", nullable = false, columnDefinition = "VARBINARY")
    private byte[] urls;
    @Column(name = "Notes", nullable = false, columnDefinition = "TEXT")
    private String notes;

    public String getName() {
        return name;
    }
    public long getId() {
        return id;
    }
    public String getNotes() {
        return notes;
    }
    public byte[] getUrls() {
        return urls;
    }
}
