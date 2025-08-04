package com.eziosoft.verandagal.database.objects;

import jakarta.persistence.*;

@Entity
@Table(name="DBMeta", uniqueConstraints = {@UniqueConstraint(columnNames = {"ID"})})
public class DbMeta {

    // define all attributes for what needs to go in the table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, unique = true)
    private long id;
    @Column(name = "DbVer", nullable = false)
    private int dbver;

    public long getId() {
        return this.id;
    }
    public int getDbver() {
        return this.dbver;
    }

    public void setDbver(int dbver) {
        this.dbver = dbver;
    }
}
