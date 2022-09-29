package com.emanuelef.remote_capture.activities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class Database extends SQLiteOpenHelper {
    public static final String name = "My_Phone";
    public static final String table = "phone";
    public static final String col1 = "ID";
    public static final String col2 = "Phone_num";

    public Database(Context context) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + table + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, PHONE TEXT, PHONE_NUM TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + table);
        onCreate(db);
    }

    // 데이터베이스 추가하기 insert

    public boolean insertData(String phone){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(col2, phone);
        long result = db.insert(table, null,contentValues);
        if(result == -1)
            return false;
        else
            return true;

    }

    //데이터베이스 항목 읽어오기 Read
    public Cursor getAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+table,null);
        return  res;
    }

    // 데이터베이스 삭제하기
    public Integer deleteData(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(table, "ID = ? ",new String[]{id});
    }

    //데이터베이스 수정하기
    public boolean updateData(String phone){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(col2, phone);
        db.update(table,contentValues,"PHONE_NUM = ?", new String[] { phone });
        return true;
    }
}
