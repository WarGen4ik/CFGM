package com.whaletail.app.data.gather.companies;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whaletail.app.data.gather.Countries.CoordsGatherer;
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
    private static final String OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";
    private static ArrayList<String> placeidList = new ArrayList<>();
    public static boolean internetProblem = false;

    public static void Release(){
        placeidList.clear();
    }

    public static Pair<Map<String, String>, QuotaLimitException> gather(String country, double lat, double lng, String tag, int radius) throws IOException {
        Map<String, String> companies = new HashMap<>();
        String status = "";

            String nearByUrl = GMAPI_URL_RADAR
                    .replace("%lat%", Double.toString(lat))
                    .replace("%lng%", Double.toString(lng))
                    .replace("%radius%", Integer.toString(radius))
                    .replace("%name%", tag)
                    .replace("%api_key%", API_KEY_PLACES);


            //logger.info("radar req = " + nearByUrl);
            JsonObject companiesJson = getJson(nearByUrl);

            //logger.info("radar resp = " + companiesJson);

            status = companiesJson.get("status").getAsString();
            if (status.equals(OVER_QUERY_LIMIT)) {
                return new Pair<>(companies, new QuotaLimitException());
            }
            if (companiesJson == null){
                internetProblem = true;
                return new Pair<>(companies, null);
            }

            JsonArray results = companiesJson.get("results").getAsJsonArray();

            if (results.size() >= 160){
                ArrayList<String> coordsList = CoordsGatherer.gatherLocations(lat,lng,radius);
                for (int i = 0; i < coordsList.size(); i++){
                    String split[] = coordsList.get(i).split(";");
                    Pair<Map<String,String>,QuotaLimitException> p = gather(country, Double.parseDouble(split[0]), Double.parseDouble(split[1]), tag, radius/4);
                    if (p.getValue() == null){
                        companies.putAll(p.getKey());
                    }
                    else {
                        if (p.getKey().size() != 0)
                            companies.putAll(p.getKey());
                        return new Pair<>(companies, p.getValue());
                    }
                }
                return new Pair<>(companies, null);
            }

            for (int i = 0; i < results.size(); i++) {
                try {
                    String place_id = ((JsonObject) results.get(i)).get("place_id").getAsString();

                    if (!isNew(place_id)) {
                        continue;
                    }

                    String detailsUrl = GMAPI_URL_DETAILS
                            .replace("%place_id%", place_id)
                            .replace("%api_key%", API_KEY_PLACES);
                    JsonObject json = getJson(detailsUrl);
                    if (json == null){
                        internetProblem = true;
                        return new Pair<>(companies, null);
                    }

                    if (json.get("status").getAsString().equals(OVER_QUERY_LIMIT)) {
                        return new Pair<>(companies, new QuotaLimitException());
                    }
                    JsonObject company = json.get("result").getAsJsonObject();


                    JsonArray address = company.getAsJsonArray("address_components");
                    boolean isThisCountry = false;
                    for (int j = 0; j < address.size(); j++){
                        JsonObject address_obj = (JsonObject)address.get(j);
                        JsonArray types = (JsonArray)address_obj.get("types");
                        for (int k = 0; k < types.size(); k++){
                            if (types.get(k).getAsString().equals("country")){
                                if (address_obj.get("short_name").getAsString().equals(country))
                                    isThisCountry = true;
                                logger.info("details req = " + detailsUrl);
                                //logger.info("details resp = " + json);
                            }
                        }
                    }
                    if (!isThisCountry) {
                        continue;
                    }


                    String name = company.get("name").getAsString();
                    String website;
                    String tel_number;
                    String company_address;

                    if (company.get("international_phone_number") != null){
                        tel_number = company.get("international_phone_number").getAsString();
                    } else tel_number = "None";
                    if (company.get("website") != null) {
                        website = company.get("website").getAsString();
                    } else website = "None";
                    if (company.get("formatted_address") != null){
                        company_address = company.get("formatted_address").getAsString();
                    } else company_address = "None";
                    String fCompany = name + ";" + company_address + ";" + tel_number;

                    companies.put(fCompany, website);
                    System.out.println(name + "     " + website);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        return new Pair<>(companies, null);
    }

    private synchronized static boolean isNew(String place_id){
        for (String ID : placeidList) {
            if (ID.equals(place_id)){
                return false;
            }
        }

        placeidList.add(place_id);
        return true;
    }
    public synchronized static void toFileId(){
        BufferedWriter writer = null;
        try{
            writer = new BufferedWriter(new FileWriter("places_id.txt"));
            for (String str : placeidList){
                writer.write(str);
                writer.newLine();
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static void getIDs()  {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("places_id.txt"));
            String str = null;
            while ((str = reader.readLine()) != null){
                placeidList.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

}
