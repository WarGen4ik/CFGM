package com.whaletail.app.data.gather.Countries;

import javafx.util.Pair;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Whaletail
 * @email silentem1113@gmail.com
 */
public class CoordsGatherer {

    private ArrayList<String> allNewCoords = new ArrayList<>();

    public ArrayList<String> gatherLocations(ArrayList<String> countries, String query) throws IOException {

        LocationGatherer locationGatherer1 = new LocationGatherer(countries, query) {

            @Override
            boolean selection(int i) {
                return i % 2 == 0;
            }
        };

        LocationGatherer locationGatherer2 = new LocationGatherer(countries, query) {

            @Override
            boolean selection(int i) {
                return i % 2 != 0;
            }
        };

        locationGatherer1.start();
        locationGatherer2.start();

        try {
            locationGatherer1.join();
            locationGatherer2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return allNewCoords;
    }

    private abstract class LocationGatherer extends Thread {

        private ArrayList<String> list;
        private String query;

        public LocationGatherer(ArrayList<String> countries, String query) {
            this.list = countries;
            this.query = query;
        }


        @Override
        public void run() {
            for (int i = 0; i < list.size(); i++){
                if (selection(i))
                    continue;
                String coords[] = list.get(i).split(";");
                try {
                    CountryGatherer countryGatherer = new CountryGatherer();
                    System.out.println(list.get(i));
                    Pair<String, ArrayList<String>> status = countryGatherer.gather(Double.parseDouble(coords[2].replace(",", ".")),
                            Double.parseDouble(coords[3].replace(",", ".")),
                            query,
                            coords[1]);

                    if (status.getKey().equals("ZERO_RESULTS")) {
                        System.out.println("ZERO_RESULTS");
                    } else {
                        allNewCoords.addAll(status.getValue());
                    }

                } catch (IOException e){
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        abstract boolean selection(int i);
    }
}
