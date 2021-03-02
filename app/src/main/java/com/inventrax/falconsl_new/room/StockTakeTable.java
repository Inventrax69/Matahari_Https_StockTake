package com.inventrax.falconsl_new.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.io.Serializable;

/*@Entity(indices = {@Index(value = {"bin","carton","sku"},
        unique = true)})*/
@Entity
public class StockTakeTable implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "bin")
    public String bin;

    @ColumnInfo(name = "carton")
    public String carton;

    @ColumnInfo(name = "sku")
    public String sku;

    @ColumnInfo(name = "qty")
    public String qty;


    public StockTakeTable(String bin, String carton, String sku,String qty) {
        this.bin = bin;
        this.carton = carton;
        this.sku = sku;
        this.qty = qty;
    }


    @Ignore
    public StockTakeTable(){

    }

    @Ignore
    public StockTakeTable(int id, String bin, String carton, String sku) {
        this.id = id;
        this.bin = bin;
        this.carton = carton;
        this.sku = sku;

    }
}
