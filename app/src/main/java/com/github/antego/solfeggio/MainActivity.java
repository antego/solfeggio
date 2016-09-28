package com.github.antego.solfeggio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView mGLView;
    private float[] fftPoints = new float[MicrophoneListener.BUFFER_SIZE / 2];
    private float[] xIndex = new float[MicrophoneListener.BUFFER_SIZE / 2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLView = new MyGLSurfaceView(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.addView(mGLView);
        for(int i = 0; i < xIndex.length; i++) {
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
        private int graphProgram;
        private int textProgram;
        int[] textures = new int[1];
        private Bitmap textBitmap = createBitmap();
        public FloatBuffer vertexBuffer;
        public ShortBuffer drawListBuffer;
        public FloatBuffer uvBuffer;
        // We have to create the vertices of our triangle.
        private float[] vertices;
        private short[] indices;
        private float[] uvs;
        private FloatBuffer xPosBuffer;
        private FloatBuffer yPosBuffer;
        private final String vertexShaderCode =
                        "attribute float xPosition;" +
                        "attribute float yPosition;" +
                        "void main() {" +
                        "  gl_Position = vec4(xPosition / " + xIndex.length + ".0 * 2.0 - 1.0, yPosition / 16383.0 - 1.0, 0.0, 1.0);" +
                        "  gl_PointSize = 5.0;" +
                        "}";

        private final String fragmentShaderCode =
                        "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

        private Bitmap createBitmap() {
            // Create an empty, mutable bitmap
            Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
// get a canvas to paint over the bitmap
            Canvas canvas = new Canvas(bitmap);
            bitmap.eraseColor(0);

// Draw the text
            Paint textPaint = new Paint();
            textPaint.setTextSize(32);
            textPaint.setAntiAlias(true);
            textPaint.setARGB(0x99, 0x99, 0x99, 0xFF);
// draw the text centered
            canvas.drawText("Hello World", 16,112, textPaint);
            return bitmap;
        }

        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            setupTriangle();

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

            graphProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(graphProgram, vertexShader);
            GLES20.glAttachShader(graphProgram, fragmentShader);
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(graphProgram);

            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, GraphicUtils.textVs);
            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, GraphicUtils.textFs);
            textProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(textProgram, vertexShader);
            GLES20.glAttachShader(textProgram, fragmentShader);
            GLES20.glLinkProgram(textProgram);


            // Create our UV coordinates.
            uvs = new float[] {
                    0.0f, 0.0f,  // bottom left
                    0.0f, 1.0f,  // top left
                    1.0f, 1.0f,   // top right
                    1.0f, 0.0f  // bottom right
            };

            // The texture buffer
            ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
            bb.order(ByteOrder.nativeOrder());
            uvBuffer = bb.asFloatBuffer();
            uvBuffer.put(uvs);
            uvBuffer.position(0);


            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

//Generate one texture pointer...
            GLES20.glGenTextures(1, textures, 0);
//...and bind it to our array
            GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

//Create Nearest Filtered Texture
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, textBitmap, 0);

//Clean up
//            bitmap.recycle();
        }

        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            yPosBuffer.put(fftPoints);
            yPosBuffer.position(0);
            GLES20.glUseProgram(graphProgram);
            int xPosHandler = GLES20.glGetAttribLocation(graphProgram, "xPosition"); //todo extract to init
            int yPosHandler = GLES20.glGetAttribLocation(graphProgram, "yPosition");
            GLES20.glEnableVertexAttribArray(xPosHandler);
            GLES20.glEnableVertexAttribArray(yPosHandler);
            GLES20.glVertexAttribPointer(xPosHandler, 1,
                    GLES20.GL_FLOAT, false,
                    vertexStride, xPosBuffer);
            GLES20.glVertexAttribPointer(yPosHandler, 1,
                    GLES20.GL_FLOAT, false,
                    vertexStride, yPosBuffer);

            int mColorHandle = GLES20.glGetUniformLocation(graphProgram, "vColor");
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);

            GLES20.glDisableVertexAttribArray(xPosHandler);
            GLES20.glDisableVertexAttribArray(yPosHandler);


            GLES20.glUseProgram(textProgram);
            int mPositionHandle =
                    GLES20.glGetAttribLocation(textProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3,
                    GLES20.GL_FLOAT, false,
                    0, vertexBuffer);

            int mTexCoordLoc = GLES20.glGetAttribLocation(textProgram, "a_texCoord");
            GLES20.glEnableVertexAttribArray(mTexCoordLoc);
            GLES20.glVertexAttribPointer(mTexCoordLoc, 2,
                    GLES20.GL_FLOAT, false,
                    0, uvBuffer);

            int mSamplerLoc = GLES20.glGetUniformLocation(textProgram, "s_texture");
            GLES20.glUniform1i(mSamplerLoc, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                    GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTexCoordLoc);

            drawText();
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

        private void drawText() {



        }

        public void setupTriangle() {
            // We have to create the vertices of our triangle.
            vertices = new float[] { -0.5f,  0.5f, 0.0f,   // top left
                                     -0.5f, -0.5f, 0.0f,   // bottom left
                                     0.5f, -0.5f, 0.0f,   // bottom right
                                     0.5f,  0.5f, 0.0f }; // top right

            indices = new short[] {0, 1, 2, 0, 2, 3}; // The order of vertexrendering.

            // The vertex buffer.
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);

            // initialize byte buffer for the draw list
            ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(indices);
            drawListBuffer.position(0);
        }
    }
}
