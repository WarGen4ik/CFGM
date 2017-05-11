package com.whaletail.app.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Whaletail
 */
public class JsonUtil {

    public static JsonObject getJson(String url) throws IOException {
        try {
            URL url_api = new URL(url);
            HttpURLConnection conn = ((HttpURLConnection) url_api.openConnection());
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JsonParser parser = new JsonParser();
            return ((JsonObject) parser.parse(reader));
        }
        catch (ConnectException e){
            e.printStackTrace();
            System.out.println("Problems with connectiong");
        }
        return null;
    }
}
