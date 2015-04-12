package org.mlarson.phoneapi;

import android.util.Log;

import org.mlarson.proteinviewer.data.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mlarson on 3/9/2015.
 */
public class PhoneApi {
    /**
     * Handles the actual HTTP GET request.
     * @param aparms : string of parameters that make up the query.
     * @return
     */
    public static String sendGetRequest(String aparms, String TAG) throws PhoneApiException
    {
        StringBuilder resp = new StringBuilder(2048);

        URL url = null;
        HttpURLConnection myconn = null;

        try
        {
            String urlString = AppConfig.getLink() + "?" + aparms;
            url = new URL(urlString);
            if (AppConfig.DEBUG) Log.d(TAG, "Trying: " + urlString);

            myconn = (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException ex) {
            // Package and send this up to be handled.
            throw new PhoneApiException(TAG + ".sendGetRequest: Malformed URL exception");
        } catch (IOException ex) {
            throw new PhoneApiException(TAG + ".sendGetRequest: Couldn't open URL connection.");
        }

        // Made a separate block so that the finally statement below to close connection
        // will always be working with an open connection.
        try {

            myconn.setRequestMethod("GET");
            myconn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            myconn.setRequestProperty("Accept", "*/*");

            myconn.setDoInput(true);

            // myconn.setReadTimeout(80000);
            myconn.connect();

            // this opens a connection, then sends GET & headers
            InputStream in = myconn.getInputStream();

            // after getInputStream, we can check the status.
            int http_status = myconn.getResponseCode();

            // Check to make sure it was an OK response - anything in the 200's is OK
            if (http_status / 100 != 2) {
                throw new PhoneApiException(TAG + ".sendGetRequest: received status " + http_status);
            }

            java.io.BufferedReader rd = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
            String line;

            while ((line = rd.readLine()) != null)
            {
                resp.append(line);
            }
            rd.close();
            return(resp.toString());
        } catch (MalformedURLException ex) {
            // Package and send this up to be handled.
            throw new PhoneApiException(TAG + ".sendGetRequest: Malformed URL exception");
        }
        catch (IOException ex)
        {
            // Probably something bad, like a network error during IO
            throw new PhoneApiException(TAG + ".sendGetRequest: error while handling IO stream - " + ex.getMessage());
        } finally {
            myconn.disconnect();
        }
    }

    /**
     * Handles the actual HTTP POST request.
     * @param aparms : string of parameters that make up the query.
     * @return
     */
    public static List<String> sendPostRequest(String aparms, String TAG) throws PhoneApiException
    {
        final String methodName = ".sendPostRequest";

        URL url = null;
        HttpURLConnection myconn = null;

        // send parameters via POST in body instead how with GET was passed with url.
        try
        {
            String urlString = AppConfig.getLink();
            url = new URL(urlString);
            if (AppConfig.DEBUG) Log.d(TAG, "Trying: " + urlString);

            myconn = (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException ex) {
            // Package and send this up to be handled.
            throw new PhoneApiException(TAG + methodName + ": Malformed URL exception");
        } catch (IOException ex) {
            throw new PhoneApiException(TAG + methodName + ": Couldn't open URL connection.");
        }

        // Made a separate block so that the finally statement below to close connection
        // will always be working with an open connection.
        try {
            // send the parameters in the request body.
            byte[] parameterData = aparms.getBytes("UTF-8");

            myconn.setRequestMethod("POST");
            myconn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            myconn.setRequestProperty("Accept", "*/*");
            myconn.setRequestProperty("Content-Length", Integer.toString( parameterData.length ));

            myconn.setDoOutput(true); // we will send parameters.
            myconn.setDoInput(true);

            // myconn.setReadTimeout(80000);
            myconn.connect();

            OutputStream out = myconn.getOutputStream();

            out.write(parameterData);
            out.flush();
            out.close();

            // this opens a connection, then sends GET & headers
            InputStream in = myconn.getInputStream();

            // after getInputStream, we can check the status.
            int http_status = myconn.getResponseCode();

            // Check to make sure it was an OK response - anything in the 200's is OK
            if (http_status / 100 != 2) {
                throw new PhoneApiException(TAG + methodName + ": received status " + http_status);
            }

            java.io.BufferedReader rd = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
            String line;

            ArrayList<String> results = new ArrayList<String>();

            while ((line = rd.readLine()) != null)
            {
                results.add(line);
            }
            rd.close();
            return(results);
        } catch (MalformedURLException ex) {
            // Package and send this up to be handled.
            throw new PhoneApiException(TAG + methodName + ": Malformed URL exception");
        }
        catch (IOException ex)
        {
            // Probably something bad, like a network error during IO
            throw new PhoneApiException(TAG + methodName + ": error while handling IO stream - " + ex.getMessage());
        } finally {
            myconn.disconnect();
        }
    }
}
