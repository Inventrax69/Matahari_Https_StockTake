package com.inventrax.falconsl_new.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface StockTakeDAO {

    @Query("SELECT * FROM StockTakeTable")
    List<StockTakeTable> getAll();

   /* @Query("SELECT * FROM StockTakeTable WHERE divisionID IN (SELECT divisionID FROM CustomerTable WHERE customerId=:customerId)")
    List<StockTakeTable> getAllByCustomer(String customerId);*/


    @Query("SELECT bin FROM StockTakeTable")
    List<String> getLocations();


    @Insert (onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StockTakeTable> stockTakeTables);

    @Delete
    void delete(StockTakeTable stockTakeTable);

    @Query("DELETE FROM StockTakeTable")
    void deleteAll();

    @Update
    void update(StockTakeTable stockTakeTable);

    @Query("UPDATE StockTakeTable SET qty =:qty WHERE bin=:bin and carton=:carton and sku=:sku")
    void update(String bin, String carton, String sku,String qty);


    @Query("SELECT COUNT(*) FROM StockTakeTable")
    int getAllItemsAllCount();

    // Master data updates
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    void insert(StockTakeTable stockTakeTable);

    @Query("SELECT * FROM StockTakeTable WHERE bin=:bin and carton=:carton and sku=:sku")
    StockTakeTable getPreviousRecord(String bin,String carton,String sku);

}
