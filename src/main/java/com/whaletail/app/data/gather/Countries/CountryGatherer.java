package com.whaletail.app.data.gather.Countries;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.whaletail.app.data.gather.companies.exceptions.QuotaLimitException;
import com.whaletail.app.util.JsonUtil;
import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;

import static com.whaletail.app.util.JsonUtil.getJson;

/**
 * Created by Admin on 28.04.2017.
 */
public class CountryGatherer {
    private static final String API_KEY_PLACES = "AIzaSyDPNi-vNBgN-1IB6wMsOi25Zk3sE8C8aYk";

    private static final String GMAPI_URL_RADAR =
            "https://maps.googleapis.com/maps/api/place/radarsearch/json?" +
                    "location=%lat%,%lng%" +
                    "&radius=%radius%" +
                    "&name=%name%" +
                    "&key=%api_key%";
    private static final String GMAPI_URL_DETAILS =
            "https://maps.googleapis.com/maps/api/place/details/json?" +
                    "placeid=%place_id%" +
                    "&key=%api_key%";

    private static final String OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";
    private static final String ZERO_RESULTS = "ZERO_RESULTS";
    private int Operation_number = 0;



    public Pair<String, ArrayList<String>> gather(double lat, double lng, String query, String countryName) throws Exception{
        ArrayList<String> list = new ArrayList<>();
        double newLat = lat;
        double newLng = lng;
        switch (Operation_number){
            case 0: Operation_number++; newLat += 0.7; break;
            case 1: Operation_number++; newLat -= 0.7; break;
            case 2: Operation_number++; newLng += 0.7; break;
            case 3: Operation_number++; newLng -= 0.7; break;
            default: break;
        }
        System.out.println(newLat + " " + newLng + "     " + Operation_number);
        String url = GMAPI_URL_RADAR
                .replace("%lat%", Double.toString(newLat))
                .replace("%lng%",Double.toString(newLng))
                .replace("%radius%",Integer.toString(30000))
                .replace("%name%",query)
                .replace("%api_key%", API_KEY_PLACES);
        System.out.println("radar - " + url);
        try {
            JsonObject jsonObject = JsonUtil.getJson(url);
            if (jsonObject.get("status").getAsString().equals(ZERO_RESULTS)){
                return new Pair<String, ArrayList<String>>(ZERO_RESULTS,null);
            }
            JsonArray results = jsonObject.get("results").getAsJsonArray();
            String place_id = ((JsonObject) results.get(0)).get("place_id").getAsString();

            String detailsUrl = GMAPI_URL_DETAILS
                    .replace("%place_id%", place_id)
                    .replace("%api_key%", API_KEY_PLACES);
            System.out.println("details - " + detailsUrl);
            JsonObject json = getJson(detailsUrl);

            if (json.get("status").getAsString().equals(OVER_QUERY_LIMIT)) {
                throw new Exception(OVER_QUERY_LIMIT);
            }
            JsonArray address = json.get("result").getAsJsonObject().get("address_components").getAsJsonArray();
            String short_name = "";
            for (int i = 0; i < address.size(); i++){
                if (address.get(i).getAsJsonObject().get("types").getAsJsonArray().get(0).getAsString().equals("country")){
                    short_name = address.get(i).getAsJsonObject().get("short_name").getAsString();
                }
            }
            System.out.println(short_name);
            if (short_name.equals(countryName)){
                JsonObject coords = json.get("result").getAsJsonObject().get("geometry").getAsJsonObject().get("location").getAsJsonObject();
                newLat = coords.get("lat").getAsDouble();
                newLng = coords.get("lng").getAsDouble();

                String s = countryName + ";" +
                        Double.toString(newLat).replace(".",",") + ";" +
                        Double.toString(newLng).replace(".",",");
                System.out.println("result - " + s);
                list.add(s);
            }

            if (Operation_number == 4){
                Operation_number = 0;
                return new Pair<String, ArrayList<String>>("OK",list);
            }
            else {
                Pair<String, ArrayList<String>> p = gather(lat,lng,query,countryName);
                if (p.getValue() != null)
                    list.addAll(p.getValue());

                return new Pair<String, ArrayList<String>>("OK", list);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Pair<String, ArrayList<String>>("OK", list);
    }
}
