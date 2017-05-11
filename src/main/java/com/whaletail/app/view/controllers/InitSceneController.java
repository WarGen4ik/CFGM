package com.whaletail.app.view.controllers;

import com.whaletail.app.data.gather.companies.DataGatherer;
import com.whaletail.app.data.gather.companies.exceptions.QuotaLimitException;
import com.whaletail.app.Country;
import com.whaletail.app.data.gather.Countries.CoordsGatherer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import static com.whaletail.app.view.util.ViewUtil.alert;

/**
 * @author Whaletail
 */
public class InitSceneController implements Initializable {
    private static final String COUNTRIES_PROPERTIES = "countries.properties";
    private static final String OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";
    private static final String COUNTRIES_COORDS = "countries_coords.csv";
    private static final String GATHERED_COUNTRIES = "gathered_countries.csv";

    @FXML
    public TextField locationField;
    @FXML
    public TextField queryField;
    @FXML
    public Button initButton;
    @FXML
    public TextArea outputArea;
    @FXML
    public Button saveButton;
    @FXML
    public BorderPane pane;
    @FXML
    private TextField intervalInput;
    @FXML
    private ComboBox cbCountry;

    private static int countCoords = 0;
    private Map<String, String> allcompanies = new HashMap<>();
    private static int numberCoords = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ArrayList<String> countries = initCountries();

        cbCountry.setItems(FXCollections.observableArrayList(countries));
        Properties countryProp = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(COUNTRIES_PROPERTIES);
            countryProp.load(is);

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

        queryField.setText("web companies");
        locationField.setEditable(false);
        outputArea.setEditable(false);


        List<Gatherer> gatherers = new ArrayList<>();

        initButton.setOnAction(event -> {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        numberCoords = 0;
                        String country = countryProp.getProperty(cbCountry.getValue().toString());
                        ArrayList<Country> countryList = getCoords(country);
                        allcompanies.clear();
                        int radius = countryList.size() <= 10000 ? 50000 : 20000;
                        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
                        ExecutorService service = new ThreadPoolExecutor(countCoords, countCoords, 0L, TimeUnit.MILLISECONDS, queue);

                        for (int i = 0; i < 200 && i < countryList.size(); i++) {
                            Gatherer gatherer = new Gatherer();
                            String query = queryField.getText().replace(" ", "+");
                            gatherer.setList(getCountriesCoords(countryList));
                            gatherer.setQuery(query);
                            gatherer.setRadius(radius);
                            gatherers.add(gatherer);
                            System.out.println(gatherer.getName() + " has started.");
                            service.submit(gatherer);
                            Thread.sleep(2000);
                        }
                    } catch (NumberFormatException e) {
                        alert(new Alert(Alert.AlertType.INFORMATION),
                                "Wrong data type!",
                                null,
                                "Input must be integer type!",
                                pane.getScene(),
                                new ButtonType("ОК", ButtonBar.ButtonData.CANCEL_CLOSE)).showAndWait();
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            });
            thread.start();

        });

       // saveButton.setOnAction(event -> service.shutdownNow());

    }
    private ArrayList<Country> getCountriesCoords(ArrayList<Country> list){
        ArrayList<Country> countriesCoords = new ArrayList<>();
        int size;
        if (list.size() / 200 > 1)
            size = list.size() / 200;
        else size = list.size();
        System.out.println("Size = " + size);
        for (int i = size * numberCoords; i < numberCoords*size+size && i < list.size(); i++){
            countriesCoords.add(list.get(i));
        }
        numberCoords++;
        return countriesCoords;
    }

    private ArrayList<Country> getCoords(String country){
        ArrayList<Country> countryList = new ArrayList<>();
        try {
            String coordLine = "";
            BufferedReader reader =
                    new BufferedReader(new FileReader(COUNTRIES_COORDS));
            countCoords = 0;
            while ((coordLine = reader.readLine()) != null) {
                String splitLine[] = coordLine.split(";");
                if (splitLine[1].equals(country)){
                    countryList.add(new Country(country,
                            splitLine[2].replace(",","."),
                            splitLine[3].replace(",",".")));
                    countCoords++;
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
        }
        return countryList;
    }

    private void checkCoords(String country, String query){
        System.out.println("Cheking coords...");
        BufferedWriter writer = null;
        BufferedReader reader = null;
        String line;
        String split[];
        ArrayList<String> list = new ArrayList<>();
        ArrayList<String> newCoords;
        int i = 0;

        try {
            reader = new BufferedReader(new FileReader(GATHERED_COUNTRIES));

            while ((line = reader.readLine()) != null) {
                if (line.equals(country)) {
                    System.out.println("Cheking is finished. Country has been found.");
                    return;
                }
            }

            reader.close();
            System.out.println("Country has not been found. Creating new coords...");
            reader = new BufferedReader(new FileReader(COUNTRIES_COORDS));

            while ((line = reader.readLine()) != null) {
                split = line.split(";");
                if (split[1].equals(country)){
                    list.add(line);
                }
                i++;
            }
            reader.close();

            CoordsGatherer gatherer = new CoordsGatherer();
            newCoords = gatherer.gatherLocations(list, query);

            System.out.printf("Coords have been created.");
            writer = new BufferedWriter(new FileWriter(COUNTRIES_COORDS,true));
            for (String str : newCoords){
                String finalLine = Integer.toString(i) + ";" + str;
                writer.write(finalLine);
                writer.newLine();
                i++;
            }
            writer.close();

            writer = new BufferedWriter(new FileWriter(GATHERED_COUNTRIES,true));
            writer.write(country);
            writer.newLine();
            writer.close();
            System.out.println("Country with new coords has been saved.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e1){
                e1.printStackTrace();
            }
            try {
                if (writer != null) {
                    reader.close();
                }
            } catch (IOException e1){
                e1.printStackTrace();
            }
        }
    }

    private ArrayList<String> initCountries(){
        try {
            ArrayList<String> country_list = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader("countries_list.txt"));
            String str;
            while ((str=reader.readLine())!=null){
                country_list.add(str);
            }
            return country_list;
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }


    private class Gatherer extends Thread {

        private String query;
        private Map<String, String> companies;
        private boolean gather = true;
        private ArrayList<Country> list;
        private int radius;

        Gatherer() {
        }


        public boolean isGather() {
            return gather;
        }

        public void setGather(boolean gather) {
            this.gather = gather;
        }

        private void setQuery(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            DataGatherer dataGatherer = new DataGatherer();
            for (int i = 0; i < list.size(); i++) {
                try {
                    Pair<Map<String, String>, QuotaLimitException> gather =
                            dataGatherer.gather(Double.parseDouble(list.get(i).getLat()),
                                    Double.parseDouble(list.get(i).getLng()), query, radius);
                    companies = gather.getKey();

                    if (gather.getValue() != null) {
                        outputArea.setText("Request limit is exhausted. Please continue next day from 10:00 ");
                        saveData();
                        break;
                    } else {
                        StringBuilder sb = new StringBuilder();
                        //companies.forEach((s, s2) ->
                        //       sb.append(s).append(": ").append(s2).append("\r\n"));
                        saveData();
                        //outputArea.setText(sb.toString());
                    }
                    Thread.sleep(list.size());

                } catch (InterruptedException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private synchronized void saveData() {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter("memory.csv", true));

                for(Map.Entry<String,String> entry : companies.entrySet()){
                    boolean temp = true;
                    for(Map.Entry<String,String> entry1 : allcompanies.entrySet()){
                        if (entry1.getKey().equals(entry.getKey())){
                            temp = false;
                        }
                    }
                    if(temp) {
                        writer.write(entry.getKey() + " " + entry.getValue());
                        writer.newLine();
                    }
                }
                allcompanies.putAll(companies);
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
    }
}
