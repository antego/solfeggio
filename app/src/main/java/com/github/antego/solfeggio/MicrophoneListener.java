package com.github.antego.solfeggio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


public class MicrophoneListener implements Runnable {
    private final static int SAMPLING_RATE = 22050;
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
        float timeWindowSec = (float)BUFFER_SIZE / SAMPLING_RATE;
        Log.d("", String.format("Time window: %.2f sec.", timeWindowSec));
        Log.d("", String.format("Refresh rate: %.2f fps", (float)SAMPLING_RATE / BUFFER_SIZE));
        Log.d("", String.format("Min frequency: %.2f Hz", 1.0 / timeWindowSec * 2));
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
