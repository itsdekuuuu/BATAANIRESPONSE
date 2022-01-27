package com.example.bataanresponse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context){
        super(context, "Cities.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Cities(id INTEGER PRIMARY KEY, city TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS Cities");
    }

    public void insertCities(int id, String city){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("id",id);
        contentValues.put("city", city);
        long result = db.insert("Cities", null, contentValues);
    }

    public  Boolean updateCities(int id, String city) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("city", city);
        Cursor cursor = db.rawQuery("SELECT * FROM Cities WHERE id=?",new String[]{String.valueOf(id)});
        if (cursor.getCount() > 0){
            long result = db.update("Cities", contentValues, "id=?", new String[]{String.valueOf(id)});
            return result != -1;
        }else{
            return false;
        }
    }

    public Boolean deleteCity (int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Cities WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.getCount() > 0) {
            long result = db.delete("Userdetails", "name=?", new String[]{String.valueOf(id)});
            return result != -1;
        } else {
            return false;
        }

    }

    public Cursor getdata ()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Cities", null);
        return cursor;

    }
}
