package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 *
 * A simple activity that displays the protein
 *
 * Created by larsonmattr on 12/17/2014.
 */
public class ProteinActivity extends CardboardActivity implements CardboardView.StereoRenderer {
    public static final int BYTES_PER_FLOAT = 4;

    private static final String TAG = "ProteinActivity";

    private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    // OpenGL address handles.
    int vertexShader;
    int fragmentShader;
    // Will hold one buffer of Vec3 atom positions.
    int[] buffers;
    int mAtomPositionsIdx;

    // BioJava to load a protein coordinates.
    ProteinStructure protein;
    int number_of_vertices;

    // Will be our client-side Vec3 data to upload.
    private FloatBuffer atomVerticesVec3;

    // Address of our shader program
    int mGlProgram;

    // Attrib handles
    int mPositionHandle;
    // Uniform Handle for MVP matrix
    int mMVPMatrixHandle;

    // Contains 2 subviews to do stereo viewport.
    private CardboardOverlayView mOverlayView;

    // matrix
    private float[] mView = new float[16];
    private float[] mCamera = new float[16];
    private float[] mModelView = new float[16];
    private float[] mHeadView = new float[16];
    private float[] mModelProtein = new float[16];
    private float[] mMVPMatrix = new float[16];

    // Distance to the protein
    private float mObjectDistance = 12f;


    @Override
    public void onNewFrame(HeadTransform headTransform) {
        /*
        GLES20.glUseProgram(mGlProgram);

        mModelViewProjectionParam = GLES20.glGetUniformLocation(mGlProgram, "u_MVP");
        mLightPosParam = GLES20.glGetUniformLocation(mGlProgram, "u_LightPos");
        mModelViewParam = GLES20.glGetUniformLocation(mGlProgram, "u_MVMatrix");
        mModelParam = GLES20.glGetUniformLocation(mGlProgram, "u_Model");
        mIsFloorParam = GLES20.glGetUniformLocation(mGlProgram, "u_IsFloor");

        // Build the Model part of the ModelView matrix.
        Matrix.rotateM(mModelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);
        */

        if (!GLES20.glIsProgram(mGlProgram)) {
            Log.d(TAG, "onNewFrame: not a valid program");
        }

        GLES20.glUseProgram(mGlProgram);

        // This location makes sense.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "u_MVPMatrix");

        // Bind attributes
        // GLES20.glBindAttribLocation(mGlProgram, 0, "a_Position");
        // checkGLError("mPositionHandle");

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(mHeadView, 0);

        checkGLError("onReadyToDraw");
    }

    @Override
    public void onDrawEye(EyeTransform eyeTransform) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Why is this handle set here, but uniform handle set elsewhere?
        mPositionHandle = GLES20.glGetAttribLocation(mGlProgram, "a_Position");
        Log.d(TAG, "mPositionHandle = " + mPositionHandle);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mView, 0, eyeTransform.getEyeView(), 0, mCamera, 0);

        // Set mModelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelProtein, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, eyeTransform.getPerspective(), 0,
                mModelView, 0);

        // method to
        drawProtein();

    }

    @Override
    public void onFinishFrame(Viewport viewport) { }

    /**
     * draw calls to display the protein.
     */
    public void drawProtein() {

        // How many floats describe a position (Vec3 = 3).
        int POSITION_DATA_SIZE = 3;

        // Draw should look something like this.
        // glBindBuffer, then glEnableVertexAttribArray, then glVertexAttribPointer..
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        // mPositionHandle = reference to the a_Position of the simple vertex shader.
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);

        // Check be the last of a set of handles
        checkGLError("mPositionHandle");

        // Set the MVP matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // In this case, glDrawArrays is the way to go, no vertex sharing = no use of glDrawElements.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, number_of_vertices);

        // At end of the drawing, unbind the buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        checkGLError("Drawing protein");
    }

    @Override
    public void onSurfaceChanged(int i, int i2) { Log.i(TAG, "onSurfaceChanged"); }

    /**
     * Setup the shader
     */
    void initOpenGL() {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well

        /*
        / Create the shader
        / Load the shader text
        / Compile shader
        / Check Errors..
        */
        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.matt_vertex);
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.matt_fragment);

        // Create our shader program
        mGlProgram = GLES20.glCreateProgram();

        // Attach shaders
        GLES20.glAttachShader(mGlProgram, vertexShader);
        GLES20.glAttachShader(mGlProgram, fragmentShader);

        GLES20.glBindAttribLocation(mGlProgram, 0, "a_Position");

        // Link the program
        GLES20.glLinkProgram(mGlProgram);

        // Create a float buffer.
        /**
         * ByteBuffers implemented from representing a protein structure as a model.
         */
        protein = new ProteinStructure(getApplicationContext());

        // Allocates and copies in coordinates in a Vec3 of floats.
        atomVerticesVec3 = protein.getVertices("A");
        atomVerticesVec3.position(0);

        // 3 floats per vertex, want to know # of vertices.
        number_of_vertices = atomVerticesVec3.capacity() / 3;

        // Object first appears directly in front of user
        Matrix.setIdentityM(mModelProtein, 0);
        Matrix.translateM(mModelProtein, 0, 0, 0, -mObjectDistance);

        // Setup atomVertices as a VBO.
        buffers = new int[1];
        mAtomPositionsIdx = 0; // will be the first index.
        // Generate 1 buffer
        GLES20.glGenBuffers(1, buffers, 0);

        // Bind to the buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        // Transfer the client data into the gpu memory
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, atomVerticesVec3.capacity() * BYTES_PER_FLOAT,
                atomVerticesVec3, GLES20.GL_STATIC_DRAW);

        //  Unbind from the buffer when we are done setting up.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "u_MVPMatrix");
        Log.d(TAG, "mMVPMatrixHandle = " + mMVPMatrixHandle);

        /*  Um...??
        // Pass in the position information
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE,
                GLES20.GL_FLOAT, false, 0, mCubeBuffer);

                */
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");

        // Calls that only should be setup at the start.
        initOpenGL();
    }

    @Override
    public void onRendererShutdown() { Log.i(TAG, "onRendererShutdown"); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        mOverlayView.show3DToast("onCreate called: CardboardView and CardboardOverlayView created.");
    }

    // Helper functions below:

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader
     * @param type The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return
     */
    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    /**
     * Converts a raw text file into a string.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return
     */
    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     * @param func
     */
    private static void checkGLError(String func) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, func + ": glError " + error);
            throw new RuntimeException(func + ": glError " + error);
        }
    }
}
