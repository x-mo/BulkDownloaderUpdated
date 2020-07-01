package com.example.moretestingtheshizz.bulkdownloader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.moretestingtheshizz.ProductViewerActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MessagerHandler {

    /**
     * This method used to send current status of downloaded image
     *
     * @param messageID unique id
     * @param params    message to sent
     */
    public static void sendMessage(int messageID, @Nullable String params, @Nullable Bundle bundle) {
        String TAG = "sendMessage";
        Messenger mActivityMessenger = null;
        if (ImageDownloaderHelper.mHandler != null) {
            mActivityMessenger = new Messenger(ImageDownloaderHelper.mHandler);
        } else {
            switch (messageID) {
                case 1:
                    //Logic for progressing the notification
                    Intent intent = new Intent(ProductViewerActivity.BULK_DOWNLOADER_NOTIFICATION);
                    intent.putExtra("downloadStatusModel",bundle.getParcelable("downloadStatusModel"));
                    LocalBroadcastManager.getInstance(ProductViewerActivity.getAppContext()).sendBroadcast(intent);
            }
        }
        // If this service is launched by the JobScheduler, there's no callback Messenger. It
        // only exists when the MainActivity calls startService() with the callback in the Intent.

        if (mActivityMessenger == null)
            return;

        Message m = Message.obtain();
        m.what = messageID;
        m.obj = params;
        m.setData(bundle);
        try {
            mActivityMessenger.send(m);
        } catch (RemoteException e) {
            Log.e(TAG, "Error passing service object back to activity.");
        }

    }

    /**
     * This handler is used for getting Success / Failure message from Work Manager Downloading image
     */
    protected static class IncomingMessageHandler extends Handler {
        int total, success = 0, failure = 0, percentage;
        ImageDownloaderHelper.DownloadStatus downloadStatus;
        private String TAG = "IncomingMessageHandler";
        private static LinkedHashMap<String, ProgressModel> trackRecord = new LinkedHashMap<>();
        private ArrayList<ProgressModel> list = new ArrayList<>();

        public IncomingMessageHandler(int total, ImageDownloaderHelper.DownloadStatus status) {
            this.downloadStatus = status;
            this.total = total;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.getData() != null) {
                        if (msg.getData().getBoolean("state", false)) {
                            ResponseBodySerializable responseBodySerializable = (ResponseBodySerializable) msg.getData().getSerializable("stream");
                            Bitmap myBitmap = BitmapFactory.decodeStream(responseBodySerializable.getResponseBody());
                            success++;
                        } else {
                            failure++;
                        }
                        DownloadStatusModel downloadStatusModel = msg.getData().getParcelable("downloadStatusModel");
                        if (downloadStatus != null)
                            downloadStatus.DownloadedItems(downloadStatusModel.getTotal(), ((downloadStatusModel.getSuccess() + downloadStatusModel.getFailure()) * 100) / downloadStatusModel.getTotal(), downloadStatusModel.getSuccess(), downloadStatusModel.getFailure());
                    }
                    break;
                case 2:
                    if (msg.getData() != null) {
                        trackRecord.put(msg.getData().getString("url"), new ProgressModel(msg.getData().getFloat("fileSize", (float) 0.0), msg.getData().getFloat("currentSize", (float) 0.0)));
                        if (downloadStatus != null) {
                            downloadStatus.CurrentDownloadPercentage(trackRecord);
                        }
                    }
            }
        }

    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

}
