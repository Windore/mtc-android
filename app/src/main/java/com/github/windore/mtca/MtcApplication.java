package com.github.windore.mtca;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.github.windore.mtca.mtc.Mtc;
import com.github.windore.mtca.mtc.MtcItem;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MtcApplication extends Application {
    private static final String TAG = "MtcApplication";

    private static final String todoFilename = "todos";
    private static final String taskFilename = "tasks";
    private static final String eventFilename = "events";

    private Mtc mtc;

    @Override
    public void onCreate() {
        super.onCreate();

        String todoJson = readFile(todoFilename);
        String taskJson = readFile(taskFilename);
        String eventJson = readFile(eventFilename);

        // Only if all strings are empty is it better construct everything again.
        // There should never be cases where some files are empty and some are not unless something
        // has gone wrong. In that case the rust will handle possible errors.
        if (todoJson.isEmpty() && taskJson.isEmpty() && eventJson.isEmpty()) {
            mtc = Mtc.constructOnlyOnce();
        } else {
            mtc = Mtc.constructOnlyOnce(todoJson, taskJson, eventJson);
        }

        // Save mtc if it changes
        mtc.addObserver(((observable, o) -> saveMtc()));
    }

    public Mtc getMtc() {
        return mtc;
    }

    private void saveMtc() {
        writeFile(todoFilename, mtc.getItemsAsJson(MtcItem.ItemType.Todo));
        writeFile(taskFilename, mtc.getItemsAsJson(MtcItem.ItemType.Task));
        writeFile(eventFilename, mtc.getItemsAsJson(MtcItem.ItemType.Event));
    }

    private void writeFile(String filename, String fileContents) {
        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(fileContents.getBytes());
        } catch (IOException e) {
            Log.e(TAG, String.format("Failed to write file %s: %s", filename, e.getMessage()));
        }
    }

    private String readFile(String filename) {
        FileInputStream fis;
        try {
            fis = openFileInput(filename);
        } catch (FileNotFoundException e) {
            return "";
        }
        InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            Log.e(TAG, String.format("Failed to read file %s: %s", filename, e.getMessage()));
        }

        return stringBuilder.toString();
    }
}
