package com.github.antego.solfeggio;


import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Map;

import static com.github.antego.solfeggio.GraphicUtils.loadShader;

public class Grid {
    private float[] xIndex;
    private float[] yPos;
    private int COORDS_PER_VERTEX = 1;
    private int vertexStride = COORDS_PER_VERTEX * 4;
    private int graphProgram;
    private FloatBuffer xPosBuffer;
    private FloatBuffer yPosBuffer;
    private String vertexShaderCode =
                    "attribute float xPosition;" +
                    "attribute float yPosition;" +
                    "void main() {" +
                    "  gl_Position = vec4(xPosition, yPosition, 0.0, 1.0);" +
                    "  gl_PointSize = 5.0;" +
                    "}";

    private String fragmentShaderCode =
                    "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    float color[] = {0.5f, 0.5f, 0.5f, 1.0f};

    public void init(Map<Float, String> ticks) {
        xIndex = new float[ticks.size() * 2];
        Iterator<Float> it = ticks.keySet().iterator();
        for(int i = 0; i < xIndex.length - 1; i += 2) {
            float pos = it.next();
            xIndex[i] = pos;
            xIndex[i+1] = pos;
        }
        yPos = new float[xIndex.length];
        for(int i = 0; i < yPos.length - 1; i += 2) {
            yPos[i] = -1;
            yPos[i+1] = 1;
        }
        ByteBuffer xPosBB = ByteBuffer.allocateDirect(xIndex.length * 4);
        xPosBB.order(ByteOrder.nativeOrder());
        xPosBuffer = xPosBB.asFloatBuffer();
        xPosBuffer.put(xIndex);
        xPosBuffer.position(0);

        ByteBuffer yPosBB = ByteBuffer.allocateDirect(yPos.length * 4);
        yPosBB.order(ByteOrder.nativeOrder());
        yPosBuffer = yPosBB.asFloatBuffer();;
        yPosBuffer.put(yPos);
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
    }

    public void render() {
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
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, xIndex.length);

        GLES20.glDisableVertexAttribArray(xPosHandler);
        GLES20.glDisableVertexAttribArray(yPosHandler);
    }
}
