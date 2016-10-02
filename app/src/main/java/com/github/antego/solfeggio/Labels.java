package com.github.antego.solfeggio;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import static com.github.antego.solfeggio.GraphicUtils.loadShader;

public class Labels {
    private int textProgram;
    private ShortBuffer drawListBuffer;
    private FloatBuffer uvBuffer;
    private Map<Bitmap, FloatBuffer> bitmapPositions = new LinkedHashMap<>();
    short[] indices;
    int[] textures;

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

    public void init(Map<Float, String> ticks, int width, int height) {
        float ratio = (float) width / height;
        for (Map.Entry<Float, String> entry : ticks.entrySet()) {
            setupLabel(entry.getKey(), entry.getValue(), ratio);
        }

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, textVs);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, textFs);
        textProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(textProgram, vertexShader);
        GLES20.glAttachShader(textProgram, fragmentShader);
        GLES20.glLinkProgram(textProgram);

        // Create our UV coordinates.
        float[] uvs = new float[] {
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

        indices = new short[] {0, 1, 2, 0, 2, 3};

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        textures = new int[bitmapPositions.size()];
        int i = 0;
        for (Bitmap bitmap : bitmapPositions.keySet()) {
            //Generate one texture pointer...
            GLES20.glGenTextures(1, textures, i);
            //...and bind it to our array
            GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[i++]);

            //Create Nearest Filtered Texture
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

            //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
            GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glEnable(GL10.GL_BLEND);
            //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

            //Clean up
            bitmap.recycle();
        }
    }

    public void render() {
        GLES20.glUseProgram(textProgram);
        int i = 0;
        int mPositionHandle = GLES20.glGetAttribLocation(textProgram, "vPosition");
        int mTexCoordLoc = GLES20.glGetAttribLocation(textProgram, "a_texCoord");
        int mSamplerLoc = GLES20.glGetUniformLocation(textProgram, "s_texture");
        for (FloatBuffer vertexBuffer : bitmapPositions.values()) {
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3,
                    GLES20.GL_FLOAT, false,
                    0, vertexBuffer);

            GLES20.glEnableVertexAttribArray(mTexCoordLoc);
            GLES20.glVertexAttribPointer(mTexCoordLoc, 2,
                    GLES20.GL_FLOAT, false,
                    0, uvBuffer);

            //GLES20.glUniform1i(mSamplerLoc, 0);
            GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[i++]);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                    GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        }
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);

    }

    public void setupLabel(Float xOffset, String label, float ratio) {
        // We have to create the vertices of our triangle.
        float[] vertices = new float[] {
                -0.5f + xOffset,  0.5f * ratio + 0.5f, 0.0f,   // top left
                -0.5f + xOffset, -0.5f * ratio + 0.5f, 0.0f,   // bottom left
                 0.5f + xOffset, -0.5f * ratio + 0.5f, 0.0f,   // bottom right
                 0.5f + xOffset,  0.5f * ratio + 0.5f, 0.0f    // top right
        };

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        bitmapPositions.put(createBitmap(label), vertexBuffer);
    }

    private Bitmap createBitmap(String label) {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        bitmap.eraseColor(0x00000000);

        Paint textPaint = new Paint();
        textPaint.setTextSize(12);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        canvas.drawRect(128 - textPaint.measureText(label) / 2,
                        84 + fm.top,
                        128 + textPaint.measureText(label) / 2,
                        84 + fm.bottom,
                        textPaint);

        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, 128, 84, textPaint);
        return bitmap;
    }
}
