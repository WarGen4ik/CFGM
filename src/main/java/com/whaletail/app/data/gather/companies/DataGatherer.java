package com.whaletail.app.data.gather.companies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.whaletail.app.data.gather.companies.exceptions.QuotaLimitException;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static com.whaletail.app.util.JsonUtil.getJson;

/**
 * @author Whaletail
 */
public class DataGatherer {

    private static final Logger logger = LoggerFactory.getLogger(DataGatherer.class);

    private static final String GMAPI_URL_GEOLOCATION = "https://maps.googleapis.com/maps/api/geocode/json?" +
            "address=%address%" +
            "&key=%api_key%";
    private static final String API_KEY_GEOLOCATION = "AIzaSyD3Hys-tXja70jAm5UMqRIRXHLosNVjSDM";
    private static final String API_KEY_PLACES = "AIzaSyCnxC5vVKTRzgLFJNa2bIauEvCyb4w8x0U";//"AIzaSyDPNi-vNBgN-1IB6wMsOi25Zk3sE8C8aYk";//
    private static final String GMAPI_URL_DETAILS =
            "https://maps.googleapis.com/maps/api/place/details/json?" +
                    "placeid=%place_id%" +
                    "&key=%api_key%";
    private static final String GMAPI_URL_RADAR =
            "https://maps.googleapis.com/maps/api/place/radarsearch/json?" +
                    "location=%lat%,%lng%" +
                    "&radius=%radius%" +
                    "&name=%name%" +
                    "&key=%api_key%";
    private static final String PAGE_TOKEN = "&pagetoken=%page_token%";
    private static final String OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";


    public Pair<Map<String, String>, QuotaLimitException> gather(double lat, double lng, String tag, int radius) throws IOException {
        Map<String, String> companies = new HashMap<>();
        String status = "";
        String pageToken = "";
        do {
            try {

                String nearByUrl = GMAPI_URL_RADAR
                        .replace("%lat%", Double.toString(lat))
                        .replace("%lng%", Double.toString(lng))
                        .replace("%radius%", Integer.toString(radius))
                        .replace("%name%", tag)
                        .replace("%api_key%", API_KEY_PLACES);

                if (!pageToken.equals("")) {
                    nearByUrl += PAGE_TOKEN.replace("%page_token%", pageToken);
                }

                logger.info("radar req = " + nearByUrl);
                JsonObject companiesJson = getJson(nearByUrl);

                logger.info("radar resp = " + companiesJson);

                status = companiesJson.get("status").getAsString();
                if (status.equals(OVER_QUERY_LIMIT)) {
                    return new Pair<>(companies, new QuotaLimitException());
                }


                JsonArray results = companiesJson.get("results").getAsJsonArray();
                for (int i = 0; i < results.size(); i++) {
                    try {
                        String place_id = ((JsonObject) results.get(i)).get("place_id").getAsString();
                        String detailsUrl = GMAPI_URL_DETAILS
                                .replace("%place_id%", place_id)
                                .replace("%api_key%", API_KEY_PLACES);
                        logger.info("details req = " + detailsUrl);
                        JsonObject json = getJson(detailsUrl);
                        logger.info("details resp = " + json);
                        if (json.get("status").getAsString().equals(OVER_QUERY_LIMIT)) {
                            return new Pair<>(companies, new QuotaLimitException());
                        }
                        JsonObject company = json.get("result").getAsJsonObject();
                        String name = company.get("name").getAsString();
                        String website;
                        if (company.get("website") != null) {
                            website = company.get("website").getAsString();
                        } else {
                            website = "None";
                        }
                        companies.put(name, website);
                        System.out.println(name + "     " + website);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (companiesJson.get("next_page_token") != null) {
                    pageToken = companiesJson.get("next_page_token").getAsString();
                    Thread.sleep(2500);
                } else {
                    pageToken = null;
                }

            } catch (InterruptedException e) {
                return new Pair<>(companies, null);
            }
        } while (pageToken != null);
        return new Pair<>(companies, null);
    }


}
