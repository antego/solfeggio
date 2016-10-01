package com.github.antego.solfeggio;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import static com.github.antego.solfeggio.GraphicUtils.loadShader;

public class Labels {
    private int textProgram;
    private int[] textures = new int[1];
    private Bitmap textBitmap = createBitmap();
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private FloatBuffer uvBuffer;
    // We have to create the vertices of our triangle.
    private float[] vertices;
    private short[] indices;
    private float[] uvs;

    private String textVs =
                    "attribute vec4 vPosition;" +
                    "attribute vec2 a_texCoord;" +
                    "varying vec2 v_texCoord;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "  v_texCoord = a_texCoord;" +
                    "}";
    private String textFs =
                    "precision mediump float;" +
                    "varying vec2 v_texCoord;" +
                    "uniform sampler2D s_texture;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(s_texture, v_texCoord);" +
                    "}";

    public void init(Map<Float, String> ticks) {
        setupTriangle();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, textVs);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, textFs);
        textProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(textProgram, vertexShader);
        GLES20.glAttachShader(textProgram, fragmentShader);
        GLES20.glLinkProgram(textProgram);


        // Create our UV coordinates.
        uvs = new float[] {
                0.0f, 0.0f,  // bottom left
                0.0f, 1.0f,  // top left
                1.0f, 1.0f,  // top right
                1.0f, 0.0f   // bottom right
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
        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GL10.GL_BLEND);
        //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, textBitmap, 0);

        //Clean up
//        bitmap.recycle();
    }

    public void render() {
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
    }

    public void setupTriangle() {
        // We have to create the vertices of our triangle.
        vertices = new float[] {
                -0.5f,  0.5f, 0.0f,   // top left
                -0.5f, -0.5f, 0.0f,   // bottom left
                 0.5f, -0.5f, 0.0f,   // bottom right
                 0.5f,  0.5f, 0.0f    // top right
        };

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

    private Bitmap createBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        bitmap.eraseColor(0x00000000);

        Paint textPaint = new Paint();
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        textPaint.setARGB(0xFF, 0x99, 0x99, 0xFF);
        canvas.drawText("Hello World", 16,112, textPaint);
        return bitmap;
    }
}
