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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 *
 * Will implement billboard / impostors to render the spheres.
 *
 * Created by larsonmattr on 1/10/2015.
 */
public class FancyProteinActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    public static final int BYTES_PER_FLOAT = 4;
    private static final String TAG = "FancyProteinActivity";
    private static final float CAMERA_Z = 0.01f;

    // Where I can, I will use same names for data members as what the shaders use.
    // This activity will have to build each of these data members, build a shader program
    // and supply the relevant data members to OpenGL.

    // Address of our shader program
    int mGlProgram;

    // Attrib handles
    int posHandle;
    int inputImposterCoordHandle;

    // Uniform Handles for MVP matrix
    int modelViewProjMatrixHandle;
    int orthographicMatrixHandle;
    int sphereRadiusHandle;
    int lightPositionHandle;
    int sphereColorHandle;

    //--------------------------------------------------
    // Account for what the Vertex Shader will require:
    // Will hold two buffers of (Vec3 atoms, and Vec2 imposter)
    int[] buffers;

    // Holding data to be loaded into the VBO.
    // BioJava to load a protein coordinates.
    ProteinStructure protein;
    int number_of_vertices;

    // Will be our client-side Vec3 data to upload.
    private FloatBuffer atomVerticesVec3;

    // Perhaps this could be a float[] instead.
    private FloatBuffer billboardData; // array of Vec2 positions
    //---------------------------------------------------

    // Uniforms used by both Vertex & Fragment shader
    private float[] modelViewProjMatrix = new float[16];
    private float[] orthographicMatrix = new float[16];
    private float sphereRadius;

    // Uniforms used by Fragment shader only
    private float[] lightPosition; // vec3
    private float[] sphereColor; // vec3

    // Matrices used to build the modelViewProjMatrix:
    private float[] mView = new float[16];
    private float[] mCamera = new float[16];
    private float[] mModelView = new float[16];
    private float[] mHeadView = new float[16];
    private float[] mModelProtein = new float[16];

    // Distance to the protein
    private float mObjectDistance = 12f;

    // Contains 2 subviews to do stereo viewport.
    private CardboardOverlayView mOverlayView;



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


    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Check if GLSL program is valid & use the program
        // Then get handles for the various attributes/uniforms
        if (!GLES20.glIsProgram(mGlProgram)) {
            Log.d(TAG, "onNewFrame: not a valid program");
        }

        GLES20.glUseProgram(mGlProgram);

        // Bind attributes
        GLES20.glBindAttribLocation(mGlProgram, 0, "pos");
        OpenGLHelper.checkGLError(TAG, "posHandle");

        GLES20.glBindAttribLocation(mGlProgram, 1, "inputImpostorCoord");
        OpenGLHelper.checkGLError(TAG, "inputImpostorCoordHandle");

        // Setup Uniform handles - sphere vertex shader
        modelViewProjMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "modelViewProjMatrix");
        orthographicMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "orthographicMatrix");
        sphereRadiusHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereRadius");

        // Setup Uniform handles - fragment shader
        lightPositionHandle = GLES20.glGetUniformLocation(mGlProgram, "lightPosition");
        sphereColorHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereColor");

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(mHeadView, 0);

        OpenGLHelper.checkGLError(TAG, "onReadyToDraw");
    }

    @Override
    public void onDrawEye(EyeTransform eyeTransform) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // TODO: update the implementation of the mMVP matrices
        // eventually, protein model shouldn't move from the view, but should only rotate.
        // ie, apply only a rotate of mModel?

        /*
        // Why is this handle set here, but uniform handle set elsewhere?
        mPositionHandle = GLES20.glGetAttribLocation(mGlProgram, "a_Position");
        Log.d(TAG, "mPositionHandle = " + mPositionHandle);
        */

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mView, 0, eyeTransform.getEyeView(), 0, mCamera, 0);

        // Set mModelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelProtein, 0);
        Matrix.multiplyMM(modelViewProjMatrix, 0, eyeTransform.getPerspective(), 0,
                mModelView, 0);

        drawProtein();
    }

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
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);

        // Check be the last of a set of handles
        OpenGLHelper.checkGLError(TAG, "mPositionHandle");

        // TODO: set the billboard handles

        // TODO: set ALL the uniform handles
        // Set the MVP matrix.
        GLES20.glUniformMatrix4fv(modelViewProjMatrixHandle, 1, false, modelViewProjMatrix, 0);
        // TODO: sphereRadius, color, etc...

        // In this case, glDrawArrays is the way to go, no vertex sharing = no use of glDrawElements.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, number_of_vertices);

        // At end of the drawing, unbind the buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        OpenGLHelper.checkGLError(TAG, "Drawing protein");
    }

    /**
     * Nothing that needs to update here.
     * @param viewport
     */
    @Override
    public void onFinishFrame(Viewport viewport) { }

    /**
     * Nothing that needs to update here.
     * @param i
     * @param i2
     */
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

        // One added step, creating inputStreams...
        InputStream vertShaderStream = getResources().openRawResource(R.raw.sphere_vertex);
        InputStream fragShaderStream = getResources().openRawResource(R.raw.sphere_fragment);

        int vertexShader = OpenGLHelper.loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, vertShaderStream);
        int fragmentShader = OpenGLHelper.loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, fragShaderStream);

        // Create our shader program
        mGlProgram = GLES20.glCreateProgram();

        // Attach shaders
        GLES20.glAttachShader(mGlProgram, vertexShader);
        GLES20.glAttachShader(mGlProgram, fragmentShader);

        // Link the program
        GLES20.glLinkProgram(mGlProgram);

        /**
         * Create a float buffer from ByteBuffer for
         *  representing a protein structure as a model.
         */
        protein = new ProteinStructure(getApplicationContext());

        // Allocates and copies in coordinates in a Vec3 of floats.
        atomVerticesVec3 = protein.getVertices("A");
        atomVerticesVec3.position(0);

        // 3 floats per vertex, want to know # of vertices.
        number_of_vertices = atomVerticesVec3.capacity() / 3;

        // Allocate memory & setup the FloatBuffer with coordinates.
        billboardData = setupBillboard();

        // Setup Object Matrices
        // Object first appears directly in front of user
        Matrix.setIdentityM(mModelProtein, 0);
        Matrix.translateM(mModelProtein, 0, 0, 0, -mObjectDistance);

        // Setup atomVertices as a VBO.
        buffers = new int[2];
        // Generate 2 buffers
        GLES20.glGenBuffers(2, buffers, 0);

        // These will be the handles for vertex attribs
        posHandle = 0;
        inputImposterCoordHandle = 1;

        // Bind to the buffer for atom vertices.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        // Transfer the client data into the gpu memory
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, atomVerticesVec3.capacity() * BYTES_PER_FLOAT,
                atomVerticesVec3, GLES20.GL_STATIC_DRAW);

        //  Unbind from the buffer when we are done setting up.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Bind to the buffer for impostor billboard
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);

        // Transfer the client data into the gpu memory (Vec2 of 4 billboard coordinates).
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 2 * BYTES_PER_FLOAT,
                billboardData, GLES20.GL_STATIC_DRAW);

        //  Unbind from the buffer when we are done setting up.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        /**
         * Non-uniform is the vertices of the atom coordinates
         * Set uniform handles, uniforms are for matrices applied to the vertices
         * and
         */
        lightPositionHandle = GLES20.glGetUniformLocation(mGlProgram, "lightPosition");
        Log.d(TAG, "lightPositionHandle = " + lightPositionHandle);

        sphereColorHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereColor");
        Log.d(TAG, "sphereColor");

        modelViewProjMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "modelViewProjMatrix");
        Log.d(TAG, "modelViewProjMatrix");

        orthographicMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "orthographicMatrix");
        Log.d(TAG, "orthographicMatrix");

        sphereRadiusHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereRadius");
        Log.d(TAG, "sphereRadius");

        // TODO: setup initial sphere color, radius, lightPosition.
        sphereColor = new float[]{1.0f, 0.0f, 0.0f};

        // orthographicMatrix = ..?  See: http://www.songho.ca/opengl/gl_projectionmatrix.html
        // TODO: setup the orthographicMatrix.

    }

    /**
     * Make a billboard like GPU gems 3 chapter 21, a four corner square.
     * @return a 4xVec2 billboard.
     */
    public FloatBuffer setupBillboard() {
        /**
         * Create a float buffer for representing the billboard.
         * 4 Vec2 vertices [(-1.0, -1.0), (1.0, -1.0), (-1.0, 1.0), (1.0, 1.0)]
         */
        int number_coords = 4;
        int floats_per = 2;

        FloatBuffer billboardData = ByteBuffer.allocateDirect(number_coords * BYTES_PER_FLOAT * floats_per)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Setup the billboard coordinates.
        billboardData.put(0, (float) -1.0);  billboardData.put(1, (float) 1.0);  // top-left
        billboardData.put(2, (float) 1.0);  billboardData.put(3, (float) 1.0); // top-right
        billboardData.put(4, (float) -1.0);  billboardData.put(5, (float) -1.0);  //bottom-left
        billboardData.put(6, (float) 1.0);  billboardData.put(7, (float) -1.0);  //bottom-right

        return billboardData;
    }

    /**
     * Put in calls that only need to be setup at the activity start.
     * @param eglConfig
     */
    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
        initOpenGL();
    }

    /**
     * Nothing needs to update here.
     */
    @Override
    public void onRendererShutdown() {  Log.i(TAG, "onRendererShutdown");  }
}
