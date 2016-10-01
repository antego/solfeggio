package com.github.antego.solfeggio;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.github.antego.solfeggio.GraphicUtils.loadShader;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView mGLView;
    private float[] fftPoints = new float[MicrophoneListener.BUFFER_SIZE / 2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLView = new MyGLSurfaceView(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.addView(mGLView);
        new Thread(new MicrophoneListener(this)).start();
    }

    class MyGLSurfaceView extends GLSurfaceView {
        private final MyGLRenderer mRenderer;

        public MyGLSurfaceView(Context context){
            super(context);
            setEGLContextClientVersion(2);
            mRenderer = new MyGLRenderer();
            setRenderer(mRenderer);
        }
    }

    public void requestDraw(float[] spectr) {
        System.arraycopy(spectr, 0, fftPoints, 0, fftPoints.length);
        mGLView.requestRender();
    }

    class MyGLRenderer implements GLSurfaceView.Renderer {
        // number of coordinates per vertex in this array

        private Spectrogram spectr = new Spectrogram();
        private Labels labels = new Labels();


        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            spectr.init(fftPoints.length);
            labels.init();
        }

        public void onDrawFrame(GL10 unused) {
            spectr.render(fftPoints);
            labels.render();
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }
    }
}
