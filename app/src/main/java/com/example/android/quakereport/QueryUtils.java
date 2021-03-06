package com.example.android.quakereport;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Zark on 12/5/2016.
 */

public final class QueryUtils {

    /**
     * Used for log tags
     */
    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    public static final int MIN_MAGNITUDE_FOR_QUERY = 9;
    public static final int NUMBER_OF_DAYS_TO_QUERY = 5;

    /**
     * Create a private constructor because no one should ever create a QueryUtils object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    public static String createUrlString() {
        // Create a string to be converted into a URL

        String usgsBaseQuery = "http://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson";

        // Date format for converting dates into the URL query format
        // (i.e. "2016-12-01")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // String builder to build the final URL string
        StringBuilder urlString = new StringBuilder();

        // Get Start date
        String startDate;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -NUMBER_OF_DAYS_TO_QUERY);
        startDate = dateFormat.format(calendar.getTime());

        // Get today's date
        String todayDate;
        Calendar calendarToday = Calendar.getInstance();
        todayDate = dateFormat.format(calendarToday.getTime());

        // Complete URL query with start date, end date, and a minimum
        // magnitude value
        urlString.append(usgsBaseQuery);
        urlString.append("&starttime=" + startDate);
        urlString.append("&endtime=" + todayDate);
        urlString.append("&minmagnitude=" + MIN_MAGNITUDE_FOR_QUERY);
        Log.i(LOG_TAG, "Current URL string from createUrlString: " + urlString.toString());

        return urlString.toString();
    }

    /**
     * Method to create a new Quake object with data parsed from the JSONResponse
     *
     * @param JSONResponse
     * @return (if successful), a new Quake object
     */
    public static ArrayList<Quake> extractFeaturesFromJson(String JSONResponse) {

        // TEST make the thread sleep for 2 seconds
        try {
            Log.i(LOG_TAG, "Sleeping........");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        if (TextUtils.isEmpty(JSONResponse)) {
            return null;
        }

        // Create an empty ArrayList to which we can add Quake objects
        ArrayList<Quake> earthquakes = new ArrayList<>();

        try {
            // Convert String into a JSONObject
            JSONObject baseJsonObject = new JSONObject(JSONResponse);
            // Get an array of returned earthquake events
            JSONArray featureArray = baseJsonObject.getJSONArray("features");

            for (int i=0; i < featureArray.length(); i++) {
                // For each earthquake event, get the properties object and extract info
                JSONObject currentQuake = featureArray.getJSONObject(i);
                JSONObject currentFeatures = currentQuake.getJSONObject("properties");
                // Extract values of interest
                double mag = currentFeatures.getDouble("mag");
                String place = currentFeatures.getString("place");
                long time = currentFeatures.getLong("time");
                String url = currentFeatures.getString("url");

                earthquakes.add(new Quake(place, mag, time, url));
            }
            // When all earthquake events have been added, return the array
            return earthquakes;

        } catch (JSONException e) {
            // IF an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log.
            Log.e(LOG_TAG, "Problem parsing the earthquake JSON results: ", e);
        }

        return earthquakes; // Temporarily changed this from "null" to "earthquakes"
    }

    /**
     * Method to create a URL object from a string
     *
     * @param aUrlString
     * @return
     */
    public static URL createURL(String aUrlString) {
        URL url = null;
        try {
            url = new URL(aUrlString);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error creating URL: " + e);
        }
        return url;
    }

    /**
     * Method to make an HTTP request from the provided URL object
     *
     * @param url
     * @return
     */
    public static String makeHttpRequest(URL url) throws IOException {

        String JSONresponse = "";

        // If the URL is empty then return early instead of crashing the program
        if (url == null) {
            return JSONresponse;
        }

        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;

        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.connect();

            // If the connection is successful, get the incoming data
            if (httpURLConnection.getResponseCode() == 200) {
                // Get the data and store it in the inputStream Object
                inputStream = httpURLConnection.getInputStream();
                JSONresponse = readFromStream(inputStream);
                // // TODO: 12/7/2016 Get this parsed into a String
            } else {
                JSONresponse = "";
                Log.v(LOG_TAG, "Error response code: " + httpURLConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error creating URL connection: " + e);
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return JSONresponse;
    }

    /**
     * Method to take an InputStream object and convert it into a String
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output  = new StringBuilder();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();

    }


}
