package com.example.hnipun.testrotation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

/**
 * Created by Padmal on 10/6/17.
 */

public class Logger {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private static final String PREFERENCES_LABEL = "CellScanner";
    private static final String CELL_IDS = "CellIDs";

    @SuppressLint("CommitPrefEdits")
    public Logger(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_LABEL, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public HashMap<String, String> getCellIDs() {
        return processIDs();
    }

    public void setCellIDs(String iDs) {
        String oldIDs;
        if (preferences.getString(CELL_IDS, null) != null) {
            oldIDs = preferences.getString(CELL_IDS, null) + "," + iDs;
        } else {
            oldIDs = iDs;
        }
        editor.putString(CELL_IDS, oldIDs);
        editor.commit();
    }

    public String getCellIDString() {
        return preferences.getString(CELL_IDS, null);
    }

    private HashMap<String, String> processIDs() {
        HashMap<String, String> map = new HashMap<>();
        String IDList = preferences.getString(CELL_IDS, null);
        if (IDList != null) {
            String[] Cells = IDList.split(",");

            for (int i = 0; i < Cells.length; i = i + 2) {
                map.put(Cells[i], Cells[i + 1]);
            }
        }
        return map;
    }
}
