package com.crawlsoft.signalclient;


//import javax.sound.sampled.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Context;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.provider.Settings.Secure;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
//import java.util.stream.Stream;

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

    private void sendGetRequest() {
        try {
            //URL url = new URL("http://10.0.2.2:5144/WeatherForecast");
            URL url = new URL("http://80.212.75.42:5144/WeatherForecast");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            //conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void sendPostRequest(long readTime, long readDuration, int currentBufferIndex, short[] sData) {
        try {
            /*
            // Load the self-signed certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getResources().openRawResource(R.raw.my_cert2); // your certificate file
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing the trusted certificate
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the certificate in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = tmf.getTrustManagers();
            sslContext.init(null, trustManagers, null);
             */

            //URL url = new URL("https://10.0.2.2:7144/api/v1/Audio/PostAudio");
            //HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            //URL url = new URL("http://10.0.2.2:5144/api/v1/Audio/PostAudio");
            URL url = new URL("http://85.166.217.96:5144/api/v1/Audio/PostAudio");
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
                /*
                String filePath = "/sdcard/recordings/recorded_audio.wav";
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(filePath);
                    System.out.println("run->FileOutputStream");
                } catch (Exception e) {
                    System.out.println("run->catch");
                    //System.out.println("St.trace: " + e.toString());
                    //e.printStackTrace();
                }
                 */

                short numberOfReads = 0;
                //Stream<short> myStream = Stream.generate(() -> new short[BufferElements2Rec]);
                //short sData[] = new short[BufferElements2Rec];
                for (int i = 0; i < BUFFER_COUNT; i++) {
                    AudioBuffer audioBuffer = new AudioBuffer(new short[BufferElements2Rec], 0);
                    buffers[i] = audioBuffer;
                }
                //int sData2[] = new int[BufferElements2Rec];
                AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
                audioRecord = new AudioRecord.Builder().setAudioFormat(audioFormat).setAudioSource(MediaRecorder.AudioSource.MIC).build();
                audioRecord.startRecording();

                while (isRecording) {
                    //byte[] data = new byte[audioRecord.getBufferSizeInFrames()];
                    long readTime = System.currentTimeMillis();
                    buffers[currentBufferIndex].timestamp = readTime;
                    audioRecord.read(buffers[currentBufferIndex].data, 0, BufferElements2Rec );

                    long readDuration = System.currentTimeMillis() - readTime;

                    numberOfReads++;
                    //System.out.println("Recording" + sData.toString());
                    //boolean anyMatch = Arrays.stream(sData).anyMatch(val -> val > 1024);
                    //boolean anyMatch = Arrays.stream(sData).filter(val -> val > 1024).findFirst().isPresent();

                    //int[] test = Arrays.stream(sData).mapToInt(Short::toUnsignedInt);
                    if (AnyMatch(buffers[currentBufferIndex].data, (short)1024)) {
                        sendPostRequest(readTime, readDuration, currentBufferIndex, buffers[currentBufferIndex].data);
                        //sendGetRequest();
                        //System.out.println("Recording: #" + numberOfReads + ", val=" + sData[0]);

                        //isRecording = false; // Ensure that we only send one recording
                    }
/*
                    try {
                        byte bData[] = short2byte(sData);
                        os.write(bData, 0, BufferElements2Rec  * BytesPerElement);
                        System.out.println("run->os.write " + bData[1024] + " " + bData[1025]);
                    } catch (Exception e) {
                        System.out.println("run->while->catch");
                        //e.printStackTrace();
                    }
 */
                    currentBufferIndex++;
                    if (currentBufferIndex >= BUFFER_COUNT)
                        currentBufferIndex = 0;

                }
/*
                try {
                    os.close();
                    System.out.println("run->os.close");
                } catch (Exception e) {
                    System.out.println("run->os.close->catch");
                    //e.printStackTrace();
                }
 */
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

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    public void stopRecording() {
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
        recordingThread = null;
    }

    /*
    public void startRecording() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        byte[] data = new byte[line.getBufferSize() / 5];
        int readBytes;

        while (true) {
            readBytes = line.read(data, 0, data.length);
            if (readBytes > 0) {
                buffers[currentBufferIndex] = new AudioBuffer(data, System.currentTimeMillis());
                currentBufferIndex = (currentBufferIndex + 1) % BUFFER_COUNT;
            }
        }
    }
    */
}