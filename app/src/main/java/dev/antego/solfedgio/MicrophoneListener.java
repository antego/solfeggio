package dev.antego.solfedgio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by anton on 31.01.2016.
 */
public class MicrophoneListener implements Runnable {
    private final static int SAMPLING_RATE = 44100;
    private final static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public final static int BUFFER_SIZE = 2048;
    short[] shortBuffer;
    float[] floatBufferRe;
    float[] floatBufferIm;
    AudioRecord recorder;
    FFT fft = new FFT(BUFFER_SIZE);
    MainActivity activity;

    public MicrophoneListener(MainActivity activity) {
        this.activity = activity;
        shortBuffer = new short[BUFFER_SIZE];
        floatBufferRe = new float[BUFFER_SIZE];
        floatBufferIm = new float[BUFFER_SIZE];
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE, RECORDER_CHANNELS, AUDIO_FORMAT, BUFFER_SIZE);
    }

    @Override
    public void run() {
//        int size = AudioRecord.getMinBufferSize(SAMPLING_RATE, RECORDER_CHANNELS, AUDIO_FORMAT);
//        System.out.println("size is: " + size);
        try {
            recorder.startRecording();
            while (!Thread.currentThread().isInterrupted()) {
                int len = recorder.read(shortBuffer, 0, BUFFER_SIZE);
                while (len < BUFFER_SIZE) {
                    len += recorder.read(shortBuffer, len, BUFFER_SIZE - len);
                }
                for (int i = 0; i < shortBuffer.length; i++) {
                    floatBufferRe[i] = shortBuffer[i];
                    floatBufferIm[i] = 0;
                }
                fft.fft(floatBufferRe, floatBufferIm);
                for (int i = 0; i < BUFFER_SIZE / 2; i++) {
                    floatBufferRe[i] = (short) Math.sqrt((double) floatBufferRe[i] * floatBufferRe[i] + floatBufferIm[i] * floatBufferIm[i]);
                }
                activity.requestDraw(floatBufferRe);
            }
        } finally {
            if(recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }
        }
    }
}
