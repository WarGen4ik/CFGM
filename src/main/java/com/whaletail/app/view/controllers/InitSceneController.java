package com.whaletail.app.view.controllers;

import com.whaletail.app.DataBase.queryExcel;
import com.whaletail.app.data.gather.companies.DataGatherer;
import com.whaletail.app.data.gather.companies.exceptions.QuotaLimitException;
import com.whaletail.app.Country;
import com.whaletail.app.view.util.ViewUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

import static com.whaletail.app.view.util.ViewUtil.alert;

/**
 * @author Whaletail
 */
public class InitSceneController implements Initializable {
    private static final String COUNTRIES_COORDS = "countries_coords.csv";
    private static final String SHORT_COUNTIRES_NAMES = "ShortCountriesNames.txt";
    private static final String CONFIG_PROPERTIES = "config.properties";

    @FXML
    public TextField queryField;
    @FXML
    public Button initButton;
    @FXML
    public BorderPane pane;
    @FXML
    private TextField currCountry;
    @FXML
    private Button setBtn;

    private static ArrayList<String> allcompanies = new ArrayList<>();
    private static int numberCoords = 0;
    private static String last_coord;
    private static String last_country;
    private static String last_query;
    private static boolean notEnoughQuota = false;
    private static boolean isAlive[];
    private int countGatherers = 0;
    private long first_country_coord;
    private static int number_query = 0;
    private ExecutorService service;
    private ArrayList<String> queries;

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        Properties countryProp = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(CONFIG_PROPERTIES);
            countryProp.load(is);
            last_coord = countryProp.getProperty("last_coord");
            last_country = countryProp.getProperty("last_country");
            last_query = countryProp.getProperty("last_query");
            if (!last_query.equals("START"))
                DataGatherer.getIDs();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        queryField.setText(last_query);
        currCountry.setEditable(false);
        currCountry.setText(last_country);

        List<Gatherer> gatherers = new ArrayList<>();

        initButton.setOnAction(event -> {
            Thread backThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        allcompanies.clear();
                        DataGatherer.Release();
                        number_query = 0;
                        queries = getQuery(last_country);

                        Gatherer g = new Gatherer();
                        g.saveData(1);
                        do {
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        gatherers.clear();
                                        ArrayList<Country> countryCoordsList = getCoords(last_country, Long.parseLong(last_coord));
                                        last_country = countryCoordsList.get(0).getCountry();
                                        numberCoords = 0;
                                        int radius = 15000;

                                        if (countryCoordsList.size() > 200){
                                            if (Math.ceil((double)countryCoordsList.size() / 200) > ((double)countryCoordsList.size()/200)){
                                                countGatherers = (int)(countryCoordsList.size() / Math.ceil((double)countryCoordsList.size() / 200));
                                            } else countGatherers = 200;
                                        } else countGatherers = countryCoordsList.size();

                                        service = Executors.newCachedThreadPool();
                                        System.out.println("COUNT = " + countGatherers);
                                        isAlive = new boolean[countGatherers];
                                        System.out.println(queries.get(number_query));

                                        for (int i = 0; i < countGatherers; i++) {
                                            isAlive[i] = true;
                                            Gatherer gatherer = new Gatherer();
                                            gatherer.setList(getCountriesCoords(countryCoordsList));
                                            gatherer.setQuery(last_query.replace(" ","+"));
                                            gatherer.setNumber(i);
                                            gatherer.setRadius(radius);
                                            gatherers.add(gatherer);
                                            System.out.println(gatherer.getName() + " has started. #" + i);
                                            service.submit(gatherer);
                                            Thread.sleep(countryCoordsList.size() / 8 + 200);
                                            if (notEnoughQuota || DataGatherer.internetProblem) {
                                                break;
                                            }
                                        }
                                        number_query++;
                                        System.out.println("COUNT GATHERERS = " + gatherers.size());

                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            if (!last_query.equals(""))
                                thread.start();
                            boolean temp;

                            do {
                                temp = true;
                                try {
                                    if (DataGatherer.internetProblem || notEnoughQuota) {
                                        break;
                                    }
                                    if (!thread.isAlive() && !isAnyAlive()) {
                                        service.shutdown();
                                        temp = false;
                                        isAlive = null;
                                    }
                                    if (checkInternetConnection()){
                                        DataGatherer.internetProblem = true;
                                    }

                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(last_coord);
                            } while (temp);
                            if (!(DataGatherer.internetProblem || notEnoughQuota)) {
                                nextQuery(queries);
                                DataGatherer.toFileId();
                                last_coord = Long.toString(first_country_coord);
                            }
                            saveProp(last_coord,null,last_query);
                            System.out.println(last_query);
                        } while (!last_query.equals("END") && !notEnoughQuota && !DataGatherer.internetProblem);

                        if (!(DataGatherer.internetProblem || notEnoughQuota)) {
                            nextCountry();
                            saveProp(null,null, "START");
                            last_query = "START";
                        }
                        saveProp(null , last_country, null);
                    } while (!last_country.equals("END") && !notEnoughQuota && !DataGatherer.internetProblem);

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (notEnoughQuota){
                                alert(new Alert(Alert.AlertType.INFORMATION),
                                        "Quota limit",
                                        null,
                                        "Your quota limit have been ended. Pls, continue tomorrow.",
                                        pane.getScene(),
                                        new ButtonType("ОК", ButtonBar.ButtonData.CANCEL_CLOSE)).showAndWait();
                            }
                            else if (DataGatherer.internetProblem){
                                alert(new Alert(Alert.AlertType.INFORMATION),
                                        "Internet problems",
                                        null,
                                        "You have internet problems, please fix it and try again",
                                        pane.getScene(),
                                        new ButtonType("ОК", ButtonBar.ButtonData.CANCEL_CLOSE)).showAndWait();
                            }
                        }
                    });
                    service.shutdownNow();

                    System.out.println("FINISHED");

                }
            });
            backThread.start();
        });

        setBtn.setOnAction(event -> {
            saveProp("0","AD", "START");
            last_country = "AD";
            last_coord = "0";
            last_query = "START";
        });
    }

    private static boolean checkInternetConnection() {
        Boolean result = true;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL("http://ru.stackoverflow.com/").openConnection();
            con.setRequestMethod("HEAD");
            result = (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            result = true;
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private boolean isAnyAlive(){
        for (int i = 0; i < countGatherers; i++){
            try {
                if (isAlive[i]) {
                    return true;
                }
            } catch (NullPointerException e){
                return false;
            }
        }
        return false;
    }
    private ArrayList<String> getQuery(String country) {
        return new queryExcel(country, last_query).getList();
    }

    private void nextQuery(ArrayList<String> queries){
        if (!(number_query >= queries.size()) && !last_query.equals(""))
            last_query = queries.get(number_query);
        else {
            last_query = "END";
        }
    }

    private void nextCountry(){
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new FileReader(SHORT_COUNTIRES_NAMES));
            String countryLine;
            while ((countryLine = reader.readLine()) != null) {
                if (countryLine.equals(last_country)){
                    last_country = reader.readLine();
                    break;
                }
            }
            currCountry.setText(last_country);
        } catch (IOException e){
            e.printStackTrace();
        }
        finally {
            if (reader != null)
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private ArrayList<Country> getCountriesCoords(ArrayList<Country> list) {
        ArrayList<Country> countriesCoords = new ArrayList<>();
        int size;
        if (list.size() >= 200) {
            size = (int)Math.ceil((double)list.size() / 200);
        } else size = list.size();

        for (int i = size * numberCoords; i < numberCoords * size + size && i < list.size(); i++) {
            countriesCoords.add(list.get(i));
        }

        numberCoords++;
        return countriesCoords;
    }

    private ArrayList<Country> getCoords(String country, Long id){
        ArrayList<Country> countryList = new ArrayList<>();
        BufferedReader reader = null;
        try {
            String coordLine;
            reader = new BufferedReader(new FileReader(COUNTRIES_COORDS));
            boolean temp = false;
            while ((coordLine = reader.readLine()) != null) {
                String splitLine[] = coordLine.split(";");
                if (splitLine[1].equals(country) && !temp) {
                    first_country_coord = Long.parseLong(splitLine[0]);
                    temp = true;
                }
                if (splitLine[1].equals(country) && (id < Long.parseLong(splitLine[0]))){
                    countryList.add(new Country(country,
                            splitLine[2].replace(",","."),
                            splitLine[3].replace(",","."),
                            Integer.parseInt(splitLine[0])));
                }
            }
        } catch (NullPointerException e) {
            alert(new Alert(Alert.AlertType.INFORMATION),
                    "Can not find file",
                    null,
                    "Can not find country.csv file. Please check if it exists!",
                    pane.getScene(),
                    new ButtonType("ОК", ButtonBar.ButtonData.CANCEL_CLOSE)).showAndWait();
        }
        catch (IOException e){
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return countryList;
    }

    /*private ArrayList<String> initCountries(){
        BufferedReader reader = null;
        try {
            ArrayList<String> country_list = new ArrayList<>();
            reader = new BufferedReader(new FileReader("countries_list.txt"));
            String str;
            while ((str=reader.readLine())!=null){
                country_list.add(str);
            }
            return country_list;
        } catch(IOException e){
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }*/

    private synchronized void saveProp(String id, String country, String query){
        Properties countryProp = new Properties();
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(CONFIG_PROPERTIES);
            countryProp.load(is);
            is.close();

            if (id != null)
                countryProp.setProperty("last_coord", id);
            if (country != null)
                countryProp.setProperty("last_country", country);
            if (query != null)
                countryProp.setProperty("last_query", query);

            os = new FileOutputStream(CONFIG_PROPERTIES);
            countryProp.store(os,null);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class Gatherer extends Thread {

        private String query;
        private Map<String, String> companies;
        private ArrayList<Country> list;
        private int radius;
        private int number;

        Gatherer() {
        }

        private void setQuery(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            if (notEnoughQuota || DataGatherer.internetProblem){
                return;
            }
            DataGatherer dataGatherer = new DataGatherer();
            for (int i = 0; i < list.size(); i++) {
                try {
                    Pair<Map<String, String>, QuotaLimitException> gather =
                            dataGatherer.gather(list.get(i).getCountry(),
                                    Double.parseDouble(list.get(i).getLat()),
                                    Double.parseDouble(list.get(i).getLng()),
                                    query,
                                    radius);
                    companies = gather.getKey();

                    if (gather.getValue() != null) {
                        if (!notEnoughQuota) {
                            last_coord = Long.toString(list.get(i).getID());
                            saveProp(last_coord, last_country, null);
                        }
                        notEnoughQuota = true;
                        saveData(0);
                        break;
                    } else {
                        saveData(0);
                        saveCoord(list.get(i).getID());
                    }
                    if (DataGatherer.internetProblem)
                        break;

                } catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("array index out of bounds exception");
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            isAlive[number] = false;
        }

        private synchronized void saveCoord(Long id){
            if (id > Long.parseLong(last_coord)) {
                last_coord = Long.toString(id);
                saveProp(last_coord, null, null);
            }
        }

        public synchronized void saveData(int k) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter("memory.csv", true));
                if (k == 1){
                    writer.write(last_country);
                    writer.newLine();
                    writer.close();
                    return;
                }

                for(Map.Entry<String,String> entry : companies.entrySet()){
                    boolean temp = true;
                    for(String str : allcompanies){
                        if ((str.equals(entry.getValue()) && !entry.getValue().equals("None"))){
                            temp = false;
                        }
                    }

                    int countCopies = 0;
                    for (Map.Entry<String, String> entry1 : companies.entrySet()){
                        if ((entry1.getValue().equals(entry.getValue()) && !entry.getValue().equals("None"))){
                            countCopies++;
                        }
                    }
                    if (countCopies > 1)
                        temp = false;

                    if(temp) {
                        writer.write(entry.getKey() + ";" + entry.getValue());
                        writer.newLine();
                        allcompanies.add(entry.getValue());
                    }
                }
                companies.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        }

        public void setRadius(int radius) {
            this.radius = radius;
        }

        public void setList(ArrayList<Country> list) {
            this.list = list;
        }

        public ArrayList<Country> getList() {
            return list;
        }


        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
}
