package com.eziosoft.verandagal.database.objects;

import jakarta.persistence.*;

@Entity
@Table(name="ImagePacks", uniqueConstraints = {@UniqueConstraint(columnNames = {"ID"})})
public class ImagePack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, unique = true)
    private long id;
    @Column(name = "PackName", nullable = false)
    private String name;
    @Column(name = "PackDesc", nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(name = "AddDate", nullable = false)
    private String uploadDate;
    @Column(name = "ImageCount", nullable = false)
    private long totalImages;
    @Column(name = "FileSystemDir", nullable = false)
    private String fsdir;

    // getters and setters
    public long getId() {
        return this.id;
    }
    public long getTotalImages() {
        return this.totalImages;
    }
    public String getDescription() {
        return this.description;
    }
    public String getFsdir() {
        return this.fsdir;
    }
    public String getName() {
        return this.name;
    }
    public String getUploadDate() {
        return this.uploadDate;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setFsdir(String fsdir) {
        this.fsdir = fsdir;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setTotalImages(long totalImages) {
        this.totalImages = totalImages;
    }
    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }
    // other code if needed can go down below here
}
