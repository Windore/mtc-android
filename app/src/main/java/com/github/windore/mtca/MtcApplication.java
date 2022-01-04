package com.github.windore.mtca;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;

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
    private Mtc mtc;

    @Override
    public void onCreate() {
        super.onCreate();

        String todoJson = readFile("todos");
        String taskJson = readFile("tasks");
        String eventJson = readFile("events");

        // Only if all strings are empty is it better construct everything again.
        // There should never be cases where some files are empty and some are not unless something
        // has went wrong. In that case the rust will handle possible errors.
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
        writeFile("todos", mtc.getItemsAsJson(MtcItem.ItemType.Todo));
        writeFile("tasks", mtc.getItemsAsJson(MtcItem.ItemType.Task));
        writeFile("events", mtc.getItemsAsJson(MtcItem.ItemType.Event));
    }

    private void writeFile(String filename, String fileContents) {
        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(fileContents.getBytes());
        } catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.error_file_writing)
                    .setMessage(e.getMessage())
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .create()
                    .show();
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.error_file_reading)
                    .setMessage(e.getMessage())
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .create()
                    .show();
        }

        return stringBuilder.toString();
    }
}
