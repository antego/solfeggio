package com.github.antego.solfeggio;


import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.github.antego.solfeggio.GraphicUtils.loadShader;

public class Spectrogram {
    private float[] xIndex = new float[MicrophoneListener.BUFFER_SIZE / 2];
    private int COORDS_PER_VERTEX = 1;
    private int vertexCount;
    private int vertexStride = COORDS_PER_VERTEX * 4;
    private int graphProgram;
    private FloatBuffer xPosBuffer;
    private FloatBuffer yPosBuffer;
    private String vertexShaderCode =
                    "attribute float xPosition;" +
                    "attribute float yPosition;" +
                    "void main() {" +
                    "  gl_Position = vec4(xPosition / " + xIndex.length + ".0 * 2.0 - 1.0, yPosition / 16383.0 - 1.0, 0.0, 1.0);" +
                    "  gl_PointSize = 5.0;" +
                    "}";

    private String fragmentShaderCode =
                    "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    public void init(int fftPointsCount) {
        vertexCount = fftPointsCount / COORDS_PER_VERTEX;
        for(int i = 0; i < xIndex.length; i++) {
            xIndex[i] = i;
        }
        ByteBuffer xPosBB = ByteBuffer.allocateDirect(xIndex.length * 4);
        xPosBB.order(ByteOrder.nativeOrder());
        xPosBuffer = xPosBB.asFloatBuffer();
        xPosBuffer.put(xIndex);
        xPosBuffer.position(0);

        ByteBuffer yPosBB = ByteBuffer.allocateDirect(fftPointsCount * 4);
        yPosBB.order(ByteOrder.nativeOrder());
        yPosBuffer = yPosBB.asFloatBuffer();;



        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        graphProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(graphProgram, vertexShader);
        GLES20.glAttachShader(graphProgram, fragmentShader);
        // creates OpenGL ES program executables
        GLES20.glLinkProgram(graphProgram);
    }

    public void render(float[] fftPoints) {
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
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(xPosHandler);
        GLES20.glDisableVertexAttribArray(yPosHandler);
    }
}
