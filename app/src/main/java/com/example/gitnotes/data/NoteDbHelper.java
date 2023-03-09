package com.example.gitnotes.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NoteDbHelper extends SQLiteOpenHelper implements BaseColumns {
    public static final String TABLE_NAME = "entry";
    public static final String _ID = "id";
    public static final String COLUMN_NAME_TITLE = "title";
    public static final String COLUMN_NAME_BODY = "body";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_NAME_TITLE + " TEXT," +
            COLUMN_NAME_BODY + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MyData.db";

    private final SQLiteDatabase dbWritable = this.getWritableDatabase();
    private final SQLiteDatabase dbReadable = this.getReadableDatabase();

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }


    public boolean addData(Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteDbHelper.COLUMN_NAME_TITLE, note.getTitle());
        values.put(NoteDbHelper.COLUMN_NAME_BODY, note.getBody());

        long res = dbWritable.insert(NoteDbHelper.TABLE_NAME, null, values);
        return res != -1;
    }

    public List<Note> viewData() {
        List<Note> notes = new ArrayList<>();
        Cursor data = dbWritable.rawQuery("SELECT * FROM " + NoteDbHelper.TABLE_NAME, null);

        if (data.getCount() == 0) {
            data.close();
            Log.d("MYLOG", "no data");
        }

        StringBuilder sb = new StringBuilder();
        while (data.moveToNext()) {
            String id = data.getString(0);
            String title = data.getString(1);
            String body = data.getString(2);

            notes.add(
                    (title.isEmpty() && body.isEmpty()) ? null : new Note(title, body)
            );

            sb.append("ID: " + id + "\n");
            sb.append("TITLE: " + data.getString(1) + "\n");
            sb.append("BODY: " + data.getString(2) + "\n");

            Log.d("MYLOG", sb.toString());
        }
        data.close();
        return notes;
    }

    public boolean updateData(String id, String title, String body) {
        try {
            Integer.parseInt(id);
            ContentValues values = new ContentValues();
            values.put(NoteDbHelper._ID, id);
            values.put(NoteDbHelper.COLUMN_NAME_TITLE, title);
            values.put(NoteDbHelper.COLUMN_NAME_BODY, body);
            dbWritable.update(NoteDbHelper.TABLE_NAME, values, "ID = ?", new String[] {id});
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int deleteData(String id) {
        try {
            Integer.parseInt(id);
            return dbWritable.delete(NoteDbHelper.TABLE_NAME, "ID = ?", new String[] {id});
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
