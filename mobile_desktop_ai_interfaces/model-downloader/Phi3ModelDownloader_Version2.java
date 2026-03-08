package com.yourapp.phi3;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Phi3ModelDownloader {

    private final Context context;
    private final String phi3Url = "https://your-server.com/path/to/phi-3-mini-4k.onnx"; // TODO: Replace with your ONNX file URL
    private final String externalFileName = "phi-3-mini-4k.onnx";
    private final String internalDirName = "models";
    private final String internalFileName = "phi-3-mini-4k.onnx";

    public Phi3ModelDownloader(Context context) {
        this.context = context;
    }

    public void startDownload() {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(phi3Url))
                .setTitle("Phi-3 Model")
                .setDescription("Downloading Phi-3 ONNX model for local inference")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, externalFileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = downloadManager.enqueue(request);

        // Listen for download completion, then move to internal storage
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    moveModelToInternalStorage();
                    context.unregisterReceiver(this);
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void moveModelToInternalStorage() {
        File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), externalFileName);
        File internalDir = new File(context.getFilesDir(), internalDirName);
        if (!internalDir.exists()) internalDir.mkdirs();
        File internalFile = new File(internalDir, internalFileName);

        try (FileInputStream input = new FileInputStream(externalFile);
             FileOutputStream output = new FileOutputStream(internalFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            // Optionally delete external copy
            externalFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            // handle error, e.g. show a message to the user
        }
    }
}