package com.example.messenger;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class WakeWordService extends Service {
    private static final String TAG = "WakeWordService";
    private static final int SAMPLE_RATE = 16000; // Common sample rate for wake word models
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeAudioRecord();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initializeAudioRecord() {
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AudioRecord: " + e.getMessage());
        }
    }

    private void startRecording() {
        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized");
            return;
        }

        isRecording = true;
        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    // Process audio data with your wake word model
                    processAudioData(buffer, read);
                }
            }
        });
        recordingThread.start();
    }

    private void processAudioData(short[] buffer, int read) {
        // TODO: Implement your wake word model processing here
        // 1. Convert audio data to the format your model expects
        // 2. Run inference with your model
        // 3. Handle detection results
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    private void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
} 