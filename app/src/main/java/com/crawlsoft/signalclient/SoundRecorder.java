package com.crawlsoft.signalclient;

import android.Manifest;
import android.content.Context;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import androidx.annotation.RequiresPermission;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class AudioBuffer {
    short[] data;
    long timestamp;

    public AudioBuffer(short[] data, long timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }
}

public class SoundRecorder {
    private String android_id;
    private Context context;
    private static final int BUFFER_COUNT = 64;
    private int currentBufferIndex = 0;

    private int BufferElements2Rec = 44100;
    private int BytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread = null;

    private AudioBuffer[] buffers = new AudioBuffer[BUFFER_COUNT];

    public SoundRecorder(/*Context context*/) {
        //this.context = context;
        android_id =  "123456789";
        //android_id =  Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    private void sendPostRequest(long readTime, long readDuration, int currentBufferIndex, short[] sData) {
        try {
            //URL url = new URL("https://10.0.2.2:7144/api/v1/Audio/PostAudio");
            //URL url = new URL("https://kiva.crawlsoft.com:7144/api/v1/Audio/PostAudio");
            URL url = new URL("https://kiva.crawlsoft.com/api/v1/Audio/PostAudio");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("DeviceId", "Android-" + android_id);
            jsonParam.put("ReadTime", readTime);
            jsonParam.put("ReadDuration", readDuration);
            jsonParam.put("BufferIndex", currentBufferIndex);
            JSONArray jsonArray = new JSONArray();
            for (short s : sData) {
                jsonArray.put(s);
            }
            jsonParam.put("AudioBuffer", jsonArray);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonParam.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

                short numberOfReads = 0;
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

                    numberOfReads++;

                    if (AnyMatch(buffers[currentBufferIndex].data, (short)1024)) {
                        sendPostRequest(readTime, readDuration, currentBufferIndex, buffers[currentBufferIndex].data);
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