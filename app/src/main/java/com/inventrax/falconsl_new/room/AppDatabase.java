package com.inventrax.falconsl_new.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;


@Database(entities = {StockTakeTable.class}, version = 1)
public abstract  class AppDatabase extends RoomDatabase {

    public abstract StockTakeDAO getStockTakeDAO();

}
