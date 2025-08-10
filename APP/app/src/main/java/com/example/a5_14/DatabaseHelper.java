package com.example.a5_14;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "environment_data.db";
    private static final int DATABASE_VERSION = 2; // 更新数据库版本

    public static final String TABLE_NAME = "EnvironmentalData";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TEMP = "temp";
    public static final String COLUMN_HUMI = "humi";
    public static final String COLUMN_SOILTEMP = "soiltemp";
    public static final String COLUMN_SOILHUM = "soilhum";
    public static final String COLUMN_PH = "ph";
    public static final String COLUMN_LIGHT = "light";
    public static final String COLUMN_CO2 = "co2";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_TEMP + " TEXT, " +
                    COLUMN_HUMI + " TEXT, " +
                    COLUMN_SOILTEMP + " TEXT, " +
                    COLUMN_SOILHUM + " TEXT, " +
                    COLUMN_PH + " TEXT, " +
                    COLUMN_LIGHT + " TEXT, " +
                    COLUMN_CO2 + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME); // 删除旧表
        onCreate(db); // 创建新表
    }
}
