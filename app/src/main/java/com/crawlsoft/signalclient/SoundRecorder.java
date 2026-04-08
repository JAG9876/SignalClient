package com.crawlsoft.signalclient;

import android.Manifest;
import android.content.Context;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import androidx.annotation.RequiresPermission;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class AudioBuffer {
    short[] data;
    long timestamp;

    public AudioBuffer(short[] data, long timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }
}

public class SoundRecorder {
    private final Context context;
    private String bearer;
    private final String refreshToken;
    private static final int BUFFER_COUNT = 64;
    private int currentBufferIndex = 0;

    private final int BufferElements2Rec = 44100;
    //private final int BytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread = null;

    private final AudioBuffer[] buffers = new AudioBuffer[BUFFER_COUNT];

    public SoundRecorder(Context ctx) {
        context = ctx;
        android.content.SharedPreferences sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        bearer = sharedPrefs.getString("access_token", null);
        refreshToken = sharedPrefs.getString("refresh_token", null);
    }

    private void sendPostRequest(long readTime, long readDuration, int currentBufferIndex, short[] sData, boolean isRetry) {
        try {
            URL url = new URL(BuildConfig.SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Bearer", bearer);
            conn.setDoOutput(true);

            JSONObject jsonParam = new JSONObject();
            String correlationId = UUID.randomUUID().toString();
            jsonParam.put("CorrelationId", correlationId);
            jsonParam.put("RequestedByServer", false);

            JSONArray recordings = new JSONArray();

            JSONObject recording = new JSONObject();
            recording.put("ReadTime", readTime);
            recording.put("ReadDuration", readDuration);
            recording.put("BufferIndex", currentBufferIndex);
            JSONArray jsonArray = new JSONArray();
            for (short s : sData) {
                jsonArray.put(s);
            }
            recording.put("Audio", jsonArray);
            recordings.put(recording);

            jsonParam.put("Recordings", recordings);



            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            if (responseCode == 401 && !isRetry) {
                System.out.println("Unauthorized. Attempting to refresh token...");

                if (refreshAccessToken()) {
                    sendPostRequest(readTime, readDuration, currentBufferIndex, sData, true);
                } else {
                    System.out.println("Refresh token failed. User may need to log in again.");
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean refreshAccessToken() {
        try {
            URL url = new URL(BuildConfig.BASE_URL + "api/v1/auth/refreshaccesstoken");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("refreshToken", refreshToken);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                JSONObject responseJson = new JSONObject(response.toString());
                String newAccessToken = responseJson.getString("accessToken");

                this.bearer = newAccessToken;
                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("access_token", newAccessToken)
                        .apply();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void init() {
        /*
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        */

        recordingThread = new Thread(new Runnable() {
            @RequiresPermission(Manifest.permission.RECORD_AUDIO)
            @Override
            public void run() {
                for (int i = 0; i < BUFFER_COUNT; i++) {
                    AudioBuffer audioBuffer = new AudioBuffer(new short[BufferElements2Rec], 0);
                    buffers[i] = audioBuffer;
                }
                AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
                audioRecord = new AudioRecord.Builder().setAudioFormat(audioFormat).setAudioSource(MediaRecorder.AudioSource.MIC).build();
                audioRecord.startRecording();

                while (isRecording) {
                    long readTime = System.currentTimeMillis();
                    buffers[currentBufferIndex].timestamp = readTime;
                    audioRecord.read(buffers[currentBufferIndex].data, 0, BufferElements2Rec );

                    long readDuration = System.currentTimeMillis() - readTime;

                    if (AnyMatch(buffers[currentBufferIndex].data, (short)1024)) {
                        sendPostRequest(readTime, readDuration, currentBufferIndex, buffers[currentBufferIndex].data, false);
                    }
                    currentBufferIndex++;
                    if (currentBufferIndex >= BUFFER_COUNT)
                        currentBufferIndex = 0;
                }
            }
        });
        isRecording = true;
        recordingThread.start();
    }

    private boolean AnyMatch(short[] sData, short limitValue) {
        for (short s : sData) {
            if (Math.abs(s) > limitValue) {
                return true;
            }
        }
        return false;
    }

    public void stopRecording() {
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
        recordingThread = null;
    }
}