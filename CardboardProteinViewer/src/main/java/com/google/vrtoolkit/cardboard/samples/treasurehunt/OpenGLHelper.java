package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Some various helper routines for loading activities and checking OpenGL errors.
 * Created by larsonmattr on 1/10/2015.
 */
public class OpenGLHelper {

    // Helper functions below:

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader
     * @param aTAG A string activity name that we can provide for logging.
     * @param type The type of shader we will be creating.
     * @param fileStream The raw text file about to be turned into a shader.
     * @return
     */
    public static int loadGLShader(String aTAG, int type, InputStream fileStream) {
        String code = readRawTextFile(fileStream);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(aTAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
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
     * @param fileStream An InputStream to the resource containing the shader code.
     * @return
     */
    public static String readRawTextFile(InputStream fileStream) {
        //InputStream inputStream = getResources().openRawResource(resId);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));
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
    public static void checkGLError(String aTAG, String func) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(aTAG, func + ": glError " + error);
            throw new RuntimeException(func + ": glError " + error);
        }
    }
}
