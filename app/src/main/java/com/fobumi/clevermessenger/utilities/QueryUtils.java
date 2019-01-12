package com.fobumi.clevermessenger.utilities;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

public final class QueryUtils {
    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    // Key for Cleverbot API
    private static final String API_KEY = "API_KEY";

    public static URL createRequestUrl(String cleverbotState, String input) {
        // Add +'s between each word in input string
        StringTokenizer tokenizer = new StringTokenizer(input);
        StringBuilder inputBuilder = new StringBuilder();
        inputBuilder.append(tokenizer.nextToken());
        while (tokenizer.hasMoreTokens()) {
            inputBuilder.append("+").append(tokenizer.nextToken());
        }

        // Build request UI
        Uri.Builder urlBuilder = new Uri.Builder();
        urlBuilder.scheme("https")
                .authority("www.cleverbot.com")
                .appendPath("getreply")
                .appendQueryParameter("key", API_KEY)
                .appendQueryParameter("input", inputBuilder.toString());

        if (cleverbotState != null) {
            urlBuilder.appendQueryParameter("cs", cleverbotState);
        }

        URL requestUrl = null;
        try {
            requestUrl = new URL(urlBuilder.build().toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error creating request URL");
        }

        return requestUrl;
    }

    // Return cleverbot state and reply from JSON response
    public static String[] getResponse(URL requestUrl) {
        String jsonResponse = "";

        try {
            // Get JSON response as String
            jsonResponse = makeHttpRequest(requestUrl);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error making HTTP request");
        }

        return extractResponseFromJson(jsonResponse);
    }

    // Make HTTP request to Cleverbot API for JSON response String
    private static String makeHttpRequest(URL requestUrl) throws IOException {
        String jsonResponse = "";
        HttpURLConnection urlConnection = null;

        InputStream inputStream = null;

        try {
            // Initiate URL connection
            urlConnection = (HttpURLConnection) requestUrl.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If connection was successful (OK)
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                StringBuilder responseBuilder = new StringBuilder();
                if (inputStream != null) {
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    // Read in response
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }
                jsonResponse = responseBuilder.toString();
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error making HTTP request");
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return jsonResponse;
    }

    // Extract cleverbot state and Cleverbot's reply from JSON String
    private static String[] extractResponseFromJson(String jsonResponse) {
        String[] response = new String[2];

        try {
            JSONObject botObject = new JSONObject(jsonResponse);
            response[0] = botObject.getString("cs");
            response[1] = botObject.getString("output");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing JSON response");
        }

        return response;
    }
}
