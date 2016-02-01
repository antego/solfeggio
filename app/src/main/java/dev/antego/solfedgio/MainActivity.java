package dev.antego.solfedgio;

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

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView mGLView;
    private float[] fftPoints = new float[MicrophoneListener.BUFFER_SIZE / 2];
    private float[] xIndex = new float[MicrophoneListener.BUFFER_SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLView = new MyGLSurfaceView(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.addView(mGLView);
        for(short i = 0; i < xIndex.length; i++) {
            xIndex[i] = i;
        }
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
        private final int COORDS_PER_VERTEX = 1;
        private final int vertexCount = fftPoints.length / COORDS_PER_VERTEX;
        private final int vertexStride = COORDS_PER_VERTEX * 4;
        private int mProgram;

        private FloatBuffer xPosBuffer;
        private FloatBuffer yPosBuffer;

        private final String vertexShaderCode =
                "attribute float xPosition;" +
                        "attribute float yPosition;" +
                        "void main() {" +
                        "  gl_Position = vec4(xPosition / " + fftPoints.length / 2 + ".0 - 1.0, yPosition / 16383.0 - 1.0, 0.0, 1.0);" +
                        "  gl_PointSize = 5.0;" +
                        "}";
        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";
        float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
        private int xPosHandler;
        private int yPosHandler;
        private int mColorHandle;

        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            ByteBuffer xPosBB = ByteBuffer.allocateDirect(xIndex.length * 4);
            xPosBB.order(ByteOrder.nativeOrder());

            ByteBuffer yPosBB = ByteBuffer.allocateDirect(fftPoints.length * 4);
            yPosBB.order(ByteOrder.nativeOrder());

            xPosBuffer = xPosBB.asFloatBuffer();
            xPosBuffer.put(xIndex);
            xPosBuffer.position(0);

            yPosBuffer = yPosBB.asFloatBuffer();
            yPosBuffer.put(fftPoints);
            yPosBuffer.position(0);

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                    vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                    fragmentShaderCode);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(mProgram);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            yPosBuffer.put(fftPoints);
            yPosBuffer.position(0);
            GLES20.glUseProgram(mProgram);
            xPosHandler = GLES20.glGetAttribLocation(mProgram, "xPosition");
            yPosHandler = GLES20.glGetAttribLocation(mProgram, "yPosition");
            GLES20.glEnableVertexAttribArray(xPosHandler);
            GLES20.glEnableVertexAttribArray(yPosHandler);
            GLES20.glVertexAttribPointer(xPosHandler, 1,
                    GLES20.GL_FLOAT, false,
                    vertexStride, xPosBuffer);
            GLES20.glVertexAttribPointer(yPosHandler, 1,
                    GLES20.GL_FLOAT, false,
                    vertexStride, yPosBuffer);

            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);

            GLES20.glDisableVertexAttribArray(xPosHandler);
            GLES20.glDisableVertexAttribArray(yPosHandler);
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        private int loadShader(int type, String shaderCode){
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }
}
