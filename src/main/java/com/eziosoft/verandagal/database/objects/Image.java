package com.eziosoft.verandagal.database.objects;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Table(name="Images", uniqueConstraints = {@UniqueConstraint(columnNames = {"ID"})})
@Indexed
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, unique = true)
    private long id;

    @Column(name = "PackID", nullable = false)
    private long packid;

    @Column(name = "ArtistID", nullable = false)
    private long artistid;

    @Column(name = "Rating", nullable = false)
    private int rating;

    @Column(name = "Resolution", nullable = false)
    private String imageres;

    @Column(name = "UploadDate", nullable = false)
    private String uploaddate;

    @Column(name = "Filename", nullable = false)
    @FullTextField
    private String filename;

    @Column(name = "OriginalURL", nullable = false)
    private String sourceurl;

    @Column(name = "Comments", nullable = false, columnDefinition = "TEXT")
    @FullTextField
    private String uploaderComments;

    @Column(name = "isAI", nullable = false)
    private boolean isAI;

    // obtain all the things in the object here
    public int getRating() {
        return this.rating;
    }

    public long getArtistid() {
        return this.artistid;
    }

    public long getId() {
        return this.id;
    }

    public long getPackid() {
        return this.packid;
    }

    public String getSourceurl() {
        return this.sourceurl;
    }

    public String getUploaddate() {
        return this.uploaddate;
    }

    public String getUploaderComments() {
        return this.uploaderComments;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getImageres() {
        return this.imageres;
    }

    public boolean isAI() {
        return this.isAI;
    }

    // set values in the object using these
    public void setArtistid(long artistid) {
        this.artistid = artistid;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public void setImageres(String imageres) {
        this.imageres = imageres;
    }
    public void setPackid(long packid) {
        this.packid = packid;
    }
    public void setRating(int rating) {
        this.rating = rating;
    }
    public void setSourceurl(String sourceurl) {
        this.sourceurl = sourceurl;
    }
    public void setUploaddate(String uploaddate) {
        this.uploaddate = uploaddate;
    }
    public void setUploaderComments(String uploaderComments) {
        this.uploaderComments = uploaderComments;
    }
    public void setAI(boolean AI) { this.isAI = AI; }
}
