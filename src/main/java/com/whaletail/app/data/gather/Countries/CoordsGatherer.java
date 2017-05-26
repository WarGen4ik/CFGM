package com.whaletail.app.data.gather.Countries;

import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import com.whaletail.app.Country;

/**
 * @author Whaletail
 * @email silentem1113@gmail.com
 */
public class CoordsGatherer {
    private ArrayList<String> allNewCoords = new ArrayList<>();
    BufferedWriter writer = null;


    private static int number = 0;
    
    private class Point{
        public double x;
        public double y;
    }

    public void gatherLocations(double lat, double lng, String country, int count) {

        int first, sec;
        Point p = new Point();
        p.x = lat;
        p.y = lng;

        //System.out.println(count);
        if (count < 10000){
            String str = country + ";" + Double.toString(p.x).replace(".", ",") + ";" +
                    Double.toString(p.y).replace(".", ",");
            write(str);
            if (country.equals("VA")){
                System.out.println(str);
            }
            //System.out.println(1);
            return;
        }
        else if (count < 500000){
            p.x += 0.0368;
            p.y -= 0.0368;
            first = 5;
            sec = 5;
            //System.out.println(2);
        }
        else {
            p.x += 0.0736;
            p.y -= 0.0736;
            first = 10;
            sec = 10;
            //System.out.println(3);
        }

        double savey = p.y;

        for (int k = 0; k < first; k++) {
            for (int j = 0; j < sec; j++) {
                write(country + ";" + Double.toString(p.x).replace(".", ",") + ";" +
                        Double.toString(p.y).replace(".", ","));
                p.y += 0.01472;
            }
            p.x -= 0.01472;
            p.y = savey;
        }
        /*try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        /*try {
            reader = new BufferedReader(new FileReader(COUNTRIES_COORDS));
            String line;
            while ((line = reader.readLine()) != null){

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }*/

        //return allNewCoords;
    }
    void write(String str) {
        try {
            writer = new BufferedWriter(new FileWriter("countries_coords.csv", true));
            String str1 = Integer.toString(number) + ";" + str;
            writer.write(str1);
            writer.newLine();
            number++;
        } catch (IOException e) {
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
}
