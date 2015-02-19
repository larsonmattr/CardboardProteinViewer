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

import org.biojava.bio.structure.Atom;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;

/**
 *
 * Will implement billboard / impostors to render the spheres.
 *
 * Created by larsonmattr on 1/10/2015.
 */
public class FancyProteinActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    /** Turn-on / Turn-off the logging */
    public static final boolean DEBUG = true;

    private static final int BYTES_PER_FLOAT = 4;
    private static final String TAG = "FancyProteinActivity";
    private static final float CAMERA_Z = 0.01f;

    // Where I can, I will use same names for data members as what the shaders use.
    // This activity will have to build each of these data members, build a shader program
    // and supply the relevant data members to OpenGL.

    // Address of our shader program
    private int mGlProgram;

    // Trying to use a VAO
    private int vaoHandle;

    // Attrib handles
    private int posHandle;
    private int inputImpostorCoordHandle;

    // Uniform Handles for MVP matrix
    private int modelViewProjMatrixHandle;
    private int orthographicMatrixHandle;
    private int sphereRadiusHandle;
    private int lightPositionHandle;
    private int sphereColorHandle;

    //--------------------------------------------------
    // Account for what the Vertex Shader will require:
    // Will hold two buffers of (Vec3 atoms, and Vec2 impostor)
    private int[] buffers;

    // Holding data to be loaded into the VBO.
    // BioJava to load a protein coordinates.
    private ProteinStructure protein;
    private int number_of_vertices;

    // Will be our client-side Vec3 data to upload.
    private FloatBuffer atomVerticesVec3;

    // Perhaps this could be a float[] instead.
    private final int billboard_pts = 6; // together will make a rectangle of two triangles.
    private FloatBuffer billboardData; // array of Vec2 positions

    //---------------------------------------------------

    // Uniforms used by both Vertex & Fragment shader
    private float[] modelViewProjMatrix = new float[16];
    private float sphereRadius;

    // Values to which we are mapping viewport
    private final float left = -1.0f;
    private final float right = 1.0f;
    private final float top = 1.0f;
    private final float bottom = -1.0f;
    private final float near = -1.0f; // value to map the near plane
    private final float far = 1.0f; // value to map the far plane
    private final float orthographicMatrix[] =
            {
                    -1.0f/right,0.0f,0.0f,-(right+left)/(right-left),
                    0.0f,1.0f/top,0.0f,-(top+bottom)/(top-bottom),
                    0.0f,0.0f,-2.0f/(far-near),-(far+near)/(far-near),
                    0.0f,0.0f,0.0f,1.0f
            };  // The matrix can be simplified further, but nice to have it all laid out, and valid if viewport not symmetrical.


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
            if (DEBUG) Log.d(TAG, "onNewFrame: not a valid program");
        }

        GLES20.glUseProgram(mGlProgram);
        //OpenGLHelper.checkGLError(TAG, "glUseProgram");

        // Setup Uniform handles - sphere vertex shader
        modelViewProjMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "modelViewProjMatrix");
        orthographicMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "orthographicMatrix");
        sphereRadiusHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereRadius");

        // Setup Uniform handles - fragment shader
        lightPositionHandle = GLES20.glGetUniformLocation(mGlProgram, "lightPosition");
        sphereColorHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereColor");

        // Build the camera matrix and apply it to the ModelView.

        float center_x = 0.0f;
        float center_y = 0.0f;
        float center_z = 0.0f;

        // If at least one point, make that the view center at start.

        if (atomVerticesVec3.capacity() > 2) {
            center_x = atomVerticesVec3.get(0);
            center_y = atomVerticesVec3.get(1);
            center_z = atomVerticesVec3.get(2);
        }


        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        //Matrix.setLookAtM(mCamera, 0, center_x, center_y, center_z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);


        headTransform.getHeadView(mHeadView, 0);

        OpenGLHelper.checkGLError(TAG, "onReadyToDraw");
    }

    @Override
    public void onDrawEye(EyeTransform eyeTransform) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // TODO: update the implementation of the mMVP matrices
        // eventually, protein model shouldn't move from the view, but should only rotate.
        // ie, apply only a rotate of mModel?
        GLES20.glUseProgram(mGlProgram);
        // Get the handles needed.
        posHandle = GLES20.glGetAttribLocation(mGlProgram, "pos");
        // Log.d(TAG, "posHandle=" + posHandle);
        OpenGLHelper.checkGLError(TAG, "posHandle");

        inputImpostorCoordHandle = GLES20.glGetAttribLocation(mGlProgram, "inputImpostorCoord");
        // Log.d(TAG, "inputImpostorCoordHandle=" + inputImpostorCoordHandle);
        OpenGLHelper.checkGLError(TAG, "inputImposterCoord");

        /**
         * Non-uniform is the vertices of the atom coordinates
         * Set uniform handles, uniforms are for matrices applied to the vertices
         *** Do not need to use glGetProgram before glGetUniformLocation.
         */
        lightPositionHandle = GLES20.glGetUniformLocation(mGlProgram, "lightPosition");
//        Log.d(TAG, "lightPositionHandle = " + lightPositionHandle);

        sphereColorHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereColor");
//        Log.d(TAG, "sphereColorHandle = " + sphereColorHandle);

        modelViewProjMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "modelViewProjMatrix");
//        Log.d(TAG, "modelViewProjMatrixHandle = " + modelViewProjMatrixHandle);

        orthographicMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, "orthographicMatrix");
//        Log.d(TAG, "orthographicMatrixHandle = " + orthographicMatrixHandle);

        sphereRadiusHandle = GLES20.glGetUniformLocation(mGlProgram, "sphereRadius");
        OpenGLHelper.checkGLError(TAG, "sphereRadius");
//        Log.d(TAG, "sphereRadiusHandle = " + sphereRadiusHandle);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mView, 0, eyeTransform.getEyeView(), 0, mCamera, 0);

        // Set mModelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelProtein, 0);
        Matrix.multiplyMM(modelViewProjMatrix, 0, eyeTransform.getPerspective(), 0,
                mModelView, 0);

        // Log.d(TAG, "modelViewProjMatrix: " + modelViewProjMatrix.toString());

        drawProtein();
    }

    /**
     * draw calls to display the protein.
     */
    public void drawProtein() {
        GLES20.glUseProgram(mGlProgram);
        OpenGLHelper.checkGLError(TAG, "glUseProgram");

        // How many floats describe a position (Vec3 = 3).
        int POSITION_DATA_SIZE = 3; // vec3
        int BILLBOARD_DATA_SIZE = 2; // vec2

        // Both the pos and impostorCoord arrays need to have equal # vertices.

        // Draw should look something like this.
        // glBindBuffer, then glEnableVertexAttribArray, then glVertexAttribPointer..
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glVertexAttribPointer(posHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(posHandle);
        if (DEBUG) OpenGLHelper.checkGLError(TAG, "posHandle");

        // set the billboard handles
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]); // second param describes the buffer index.
        if (DEBUG) OpenGLHelper.checkGLError(TAG, "inputImpostorCoordHandle-bindBuffer");
        GLES20.glVertexAttribPointer(inputImpostorCoordHandle, BILLBOARD_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);
        if (DEBUG) OpenGLHelper.checkGLError(TAG, "inputImpostorCoordHandle-attribPointer");
        GLES20.glEnableVertexAttribArray(inputImpostorCoordHandle);
        if (DEBUG) OpenGLHelper.checkGLError(TAG, "inputImpostorCoordHandle-enablevertexattribarray");

        // Set ALL the uniform handles
        // Set the MVP matrix.
        GLES20.glUniformMatrix4fv(modelViewProjMatrixHandle, 1, false, modelViewProjMatrix, 0);
        GLES20.glUniformMatrix4fv(orthographicMatrixHandle, 1, false, orthographicMatrix, 0);

        // Set uniforms for sphereRadius, color, lightPosition, etc...
        GLES20.glUniform1f(sphereRadiusHandle, sphereRadius);
        GLES20.glUniform3f(sphereColorHandle, sphereColor[0], sphereColor[1], sphereColor[2]);
        GLES20.glUniform3f(lightPositionHandle, lightPosition[0], lightPosition[1], lightPosition[2]);

        if (DEBUG) OpenGLHelper.checkGLError(TAG, "Drawing, set uniforms.");

        // Draw the list of spheres using indices with glDrawElements
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, number_of_vertices);
        // GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, number_of_vertices);
        // Getting error 1282!
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, number_of_vertices);

        // At end of the drawing, unbind the buffer
        // GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        if (DEBUG) OpenGLHelper.checkGLError(TAG, "Drawing protein");
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
    void setupShader() {
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

        // Load and compile shaders.
        int vertexShader = OpenGLHelper.loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, vertShaderStream);
        int fragmentShader = OpenGLHelper.loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, fragShaderStream);

        // Create our shader program
        mGlProgram = GLES20.glCreateProgram();

        // Attach shaders
        GLES20.glAttachShader(mGlProgram, vertexShader);
        GLES20.glAttachShader(mGlProgram, fragmentShader);

        // Bind attribute location we will use before wrapping up with linking the shaders.
        /*
        GLES20.glBindAttribLocation(mGlProgram, 0, "pos");
        OpenGLHelper.checkGLError(TAG, "glBindAttribLocation");
        GLES20.glBindAttribLocation(mGlProgram, 1, "inputImpostorCoord");
        OpenGLHelper.checkGLError(TAG, "glBindAttribLocation");
        */

        // Link the program
        GLES20.glLinkProgram(mGlProgram);

        // Check if programs compiled and linked successfully.
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(mGlProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(mGlProgram));
            GLES20.glDeleteProgram(mGlProgram);
            mGlProgram = 0;
        }

        if (DEBUG) Log.d(TAG, "Created and linked the shaders, mGlProgram=" + mGlProgram);
    }

    /**
     * Setup the OpenGL shader program and buffers.
     */
    void initOpenGL() {

        setupShader();

        /**
         * Create a float buffer from ByteBuffer for
         *  representing a protein structure as a model.
         *  Right now, just using my test PDB. TODO: make general.
         */
        protein = new ProteinStructure(getApplicationContext());

        // Allocates and copies in coordinates in a Vec3 of floats.
        // Get the atoms from chain A
        List<Atom> atoms = protein.getAtoms(protein.getAtomGroups("A"));
        atomVerticesVec3 = setupCoordinateVertices(atoms);

        // 3 floats per vertex, want to know # of vertices.
        int number_of_atoms = atoms.size();
        number_of_vertices = number_of_atoms * billboard_pts;

        // Allocate memory & setup the FloatBuffer with coordinates.
        billboardData = setupBillboard(number_of_atoms);

        describeTriangles(12, atomVerticesVec3, billboardData);

        // Setup Object Matrices
        // Object first appears directly in front of user
        Matrix.setIdentityM(mModelProtein, 0);
        Matrix.translateM(mModelProtein, 0, 0, 0, -mObjectDistance);

        // Setup atomVertices as a VBO.
        buffers = new int[2];
        // Generate 2 buffers
        GLES20.glGenBuffers(2, buffers, 0);

        // Bind to the buffer for atom vertices.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        if (DEBUG) Log.d(TAG, "client buffer size: " + atomVerticesVec3.capacity() + ", glBuffer size: " + (number_of_vertices * 3));
        // Transfer the client data into the gpu memory

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, atomVerticesVec3.capacity() * BYTES_PER_FLOAT,
                atomVerticesVec3, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Bind to the buffer for impostor billboard
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);

        Log.d(TAG, "client buffer size: " + billboardData.capacity() + ", glBuffer size: " + (number_of_vertices * 2));
        billboardData.position(0);

        // Transfer the client data into the gpu memory (Vec2 of 4 billboard coordinates).
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, billboardData.capacity() * BYTES_PER_FLOAT,
                billboardData, GLES20.GL_STATIC_DRAW);

        //  Unbind from the buffer when we are done setting up.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // setup initial sphere color, radius, lightPosition.
        sphereColor = new float[]{1.0f, 0.0f, 0.0f};
        sphereRadius = 1.5f; // Not sure yet what to put for this : TODO  1.5angstrom might be ok
        //lightPosition = new float[]{right, top, near}; // Put at some interesting angle in front.
        lightPosition = new float[]{0.5f, 0.5f, 0.5f};
    }

    /**
     * Create billboarding coordinates, 4 coordinates * N spheres.
     *
     * Make a billboard like GPU gems 3 chapter 21, a four corner square.
     * @return a 4xVec2 billboard.
     */
    public FloatBuffer setupBillboard(int spheres) {
        /**
         * Create a float buffer for representing the billboard.
         * 4 Vec2 vertices [(-1.0, -1.0), (1.0, -1.0), (-1.0, 1.0), (1.0, 1.0)]
         */
        int floats_per = 2; // vec2

        FloatBuffer billboardData = ByteBuffer.allocateDirect(spheres * billboard_pts * BYTES_PER_FLOAT * floats_per)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Move to start.
        int currentPosition = 0;

        // Setup the billboard coordinates.
        for (int i = 0; i < spheres; i++) {
            // Provide the faces in counter-clockwise order.
            /*
            billboardData.put(currentPosition++, -1.0f); billboardData.put(currentPosition++, 1.0f);  // top-left
            billboardData.put(currentPosition++, -1.0f); billboardData.put(currentPosition++, -1.0f);  //bottom-left
            billboardData.put(currentPosition++, 1.0f);  billboardData.put(currentPosition++, 1.0f); // top-right
            billboardData.put(currentPosition++, 1.0f);  billboardData.put(currentPosition++, -1.0f);  //bottom-right
            billboardData.put(currentPosition++, 1.0f);  billboardData.put(currentPosition++, 1.0f); // top-right
            billboardData.put(currentPosition++, -1.0f); billboardData.put(currentPosition++, -1.0f);  //bottom-left
            */
            billboardData.put(currentPosition++, -1.0f); billboardData.put(currentPosition++, -1.0f);  //bottom-left
            billboardData.put(currentPosition++, -1.0f); billboardData.put(currentPosition++, 1.0f);  // top-left
            billboardData.put(currentPosition++, 1.0f);  billboardData.put(currentPosition++, 1.0f); // top-right
            billboardData.put(currentPosition++, 1.0f);  billboardData.put(currentPosition++, 1.0f); // top-right
            billboardData.put(currentPosition++, 1.0f);  billboardData.put(currentPosition++, -1.0f);  //bottom-right
            billboardData.put(currentPosition++, -1.0f); billboardData.put(currentPosition++, -1.0f);  //bottom-left
        }
        Log.d(TAG, "setupBillboard: final value " + currentPosition);

        billboardData.position(0);
        return billboardData;
    }

    /**
     * Builds a repeated
     * @param atoms
     * @return
     */
    public FloatBuffer setupCoordinateVertices(List<Atom> atoms) {
        int floats_per = 3; // vec3
        FloatBuffer vertexData = ByteBuffer.allocateDirect(atoms.size() * billboard_pts * BYTES_PER_FLOAT * floats_per)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        int current_Position = 0;

        for (int i = 0; i < atoms.size(); i++) {
            for (int j = 0; j < billboard_pts; j++) {
                Atom atom = atoms.get(i);
                vertexData.put(current_Position++, (float) atom.getX());
                vertexData.put(current_Position++, (float) atom.getY());
                vertexData.put(current_Position++, (float) atom.getZ());
            }
        }

        vertexData.position(0);

        return vertexData;
    }

    /**
     * For debugging my FloatBuffer vectors;
     * @param vertices
     * @param vector_type
     * @param buffer
     */
    public void describeFloatBuffer(int vertices, int vector_type, FloatBuffer buffer) {
        int current_position = 0;
        for (int i = 0; i < vertices; i++) {
            StringBuilder str = new StringBuilder();
            str.append("Vector " + i + ": (");
            boolean comma = false;
            for (int j = 0; j < vector_type; j++) {
                if (comma) str.append(", ");
                Float current = buffer.get(current_position);
                str.append(current.toString());
                comma = true;
                current_position++;
            }
            str.append(")");
            Log.d(TAG, str.toString());
        }
    }

    public void describeTriangles(int vertices, FloatBuffer positions, FloatBuffer billBoard) {
        int current_pos = 0;
        int current_billboard = 0;
        for (int i = 0; i < vertices; i++) {
            StringBuilder str = new StringBuilder();
            str.append("Vector " + i + ": (");
            float x = positions.get(current_pos);
            float y = positions.get(current_pos+1);
            float z = positions.get(current_pos+2);
            current_pos += 3;
            float dx = billBoard.get(current_billboard);
            float dy = billBoard.get(current_billboard+1);
            current_billboard+=2;
            x+=dx;
            y+=dy;
            str.append(x + ", " + y + ", " + z + ")");
            Log.d(TAG, str.toString());
        }
    }

    /**
     * Put in calls that only need to be setup at the activity start.
     * @param eglConfig
     */
    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
        initOpenGL();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // GLES20.glDepthFunc(GLES20.GL_NEVER);

    }

    /**
     * Nothing needs to update here.
     */
    @Override
    public void onRendererShutdown() {  Log.i(TAG, "onRendererShutdown");  }
}
