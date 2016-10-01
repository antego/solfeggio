package com.github.antego.solfeggio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Pair;


public class MicrophoneListener implements Runnable {
    private final static int[] SAMPLING_RATES = {22050, 44100};
    private final static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public final static int BUFFER_SIZE = 2048;
    private short[] shortBuffer = new short[BUFFER_SIZE];
    private float[] floatBufferRe = new float[BUFFER_SIZE];
    private float[] floatBufferIm = new float[BUFFER_SIZE];
    private AudioRecord recorder;
    private FFT fft = new FFT(BUFFER_SIZE);
    private MainActivity activity;

    public MicrophoneListener(MainActivity activity) {
        this.activity = activity;
    }

    public int init() {
        for (int samplingRate : SAMPLING_RATES) {
            try {
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, RECORDER_CHANNELS, AUDIO_FORMAT, BUFFER_SIZE);
                if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    continue;
                }
                float timeWindowSec = (float) BUFFER_SIZE / samplingRate;
                Log.d("", String.format("Time window: %.2f sec.", timeWindowSec));
                Log.d("", String.format("Refresh rate: %.2f fps", (float) samplingRate / BUFFER_SIZE));
                Log.d("", String.format("Min frequency: %.2f Hz", 1.0 / timeWindowSec * 2));
                return samplingRate;
            } catch (IllegalArgumentException e) {
                Log.i("", "init mic", e);
            }
        }
        throw new RuntimeException("Can't init mic");
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
