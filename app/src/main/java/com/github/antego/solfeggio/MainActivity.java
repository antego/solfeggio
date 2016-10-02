package com.github.antego.solfeggio;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView mGLView;
    private float[] fftPoints = new float[MicrophoneListener.BUFFER_SIZE / 2];
    private int samplingRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLView = new MyGLSurfaceView(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.addView(mGLView);
        MicrophoneListener mic = new MicrophoneListener(this);
        samplingRate = mic.init();
        new Thread(mic).start();
    }

    public void requestDraw(float[] spectr) {
        System.arraycopy(spectr, 0, fftPoints, 0, fftPoints.length);
        mGLView.requestRender();
    }

    class MyGLSurfaceView extends GLSurfaceView {
        private final MyGLRenderer mRenderer;

        public MyGLSurfaceView(Context context) {
            super(context);
            setEGLContextClientVersion(2);
            mRenderer = new MyGLRenderer();
            setRenderer(mRenderer);
        }

        class MyGLRenderer implements Renderer {
            private Spectrogram spectr = new Spectrogram();
            private Labels labels = new Labels();
            private Grid grid = new Grid();
            private TickGenerator tickGenerator = new TickGenerator();

            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                Map<Float, String> ticks = tickGenerator.generateTicks(samplingRate);
                grid.init(ticks);
                labels.init(ticks, getWidth(), getHeight());
                spectr.init(fftPoints.length);
            }

            public void onDrawFrame(GL10 unused) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                grid.render();
                labels.render();
                spectr.render(fftPoints);
            }

            public void onSurfaceChanged(GL10 unused, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
            }
        }
    }
}
