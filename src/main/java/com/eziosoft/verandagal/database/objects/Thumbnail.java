package com.eziosoft.verandagal.database.objects;

import jakarta.persistence.*;

@Entity
@Table(name="thumbnails", uniqueConstraints = {@UniqueConstraint(columnNames = {"ID"})})
public class Thumbnail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, unique = true)
    private long id;
    @Column(name = "imgdata", nullable = false)
    private byte[] imagedata;
    // getters and setters
    public long getId() {
        return this.id;
    }
    public byte[] getImagedata() {
        return this.imagedata;
    }
    public void setImagedata(byte[] imagedata) {
        this.imagedata = imagedata;
    }
    // new: in some cases, setting a id manually is required
    public void setId(long id){
        this.id = id;
    }
}
