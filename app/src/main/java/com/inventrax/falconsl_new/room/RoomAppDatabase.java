package com.inventrax.falconsl_new.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;


public class RoomAppDatabase {

    public Context context;

    public RoomAppDatabase(Context context){
       this.context=context;
    }

    public AppDatabase getAppDatabase(){
        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "stockTake")
                .allowMainThreadQueries()
                //.fallbackToDestructiveMigration()
                //.addMigrations(MIGRATION_1_3)
                .build();
        return db;
    }

    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
           // database.execSQL("ALTER TABLE VariantTable ADD COLUMN stackSize INTEGER NOT NULL DEFAULT 0");
            //database.execSQL("ALTER TABLE VariantTable ADD COLUMN packSize INTEGER NOT NULL DEFAULT 0");
        }
    };

}
