package com.whaletail.app.view.controllers;

import com.whaletail.app.data.gather.companies.DataGatherer;
import com.whaletail.app.data.gather.companies.exceptions.QuotaLimitException;
import com.whaletail.app.Country;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;

import java.io.*;
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
    private static final String LOCALE_PROP = "languages.Locale";

    @FXML
    public TextField queryField;
    @FXML
    public Button initButton;
    @FXML
    public BorderPane pane;
    @FXML
    private ComboBox cbCountry;
    @FXML
    private TextField currCountry;
    @FXML
    private Button setBtn;

    private static int countCoords = 0;
    private Map<String, String> allcompanies = new HashMap<>();
    private static int numberCoords = 0;
    private static String last_coord;
    private static String last_country;
    private static boolean notEnoughQuota = false;
    public int countGatherers = 0;
    public int currentGatherers = 0;
    ExecutorService service;
    private ArrayList<ResourceBundle> bundles;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ArrayList<String> countries = initCountries();

        cbCountry.setItems(FXCollections.observableArrayList(countries));
        Properties countryProp = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(CONFIG_PROPERTIES);
            countryProp.load(is);
            last_coord = countryProp.getProperty("last_coord");
            last_country = countryProp.getProperty("last_country");
            //Enumeration<?> list = countryProp.propertyNames();
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
        bundles = new ArrayList<>();
        queryField.setText("web companies");
        currCountry.setEditable(false);

        List<Gatherer> gatherers = new ArrayList<>();

        initButton.setOnAction(event -> {
            Thread backThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    currentGatherers = 0;
                                    DataGatherer.Release();
                                    numberCoords = 0;
                                    ArrayList<Country> countryCoordsList = getCoords(last_country, Long.parseLong(last_coord));
                                    if (countryCoordsList.size() > 0) {
                                        last_country = countryCoordsList.get(0).getCountry();
                                        writeCountry(last_country);
                                    }
                                    else return;
                                    System.out.println("count country coords = " + countryCoordsList.size());
                                    allcompanies.clear();
                                    int radius = 2000;
                                    countGatherers = countryCoordsList.size() >= 200 ? 200 : countryCoordsList.size();
                                    service = Executors.newCachedThreadPool();
                                    System.out.println("count = " + countGatherers);
                                    String query = getQuery(queryField.getText(), last_country);

                                    for (int i = 0; i < countGatherers; i++) {
                                        Gatherer gatherer = new Gatherer();
                                        System.out.println(query);
                                        Thread.sleep(10000);
                                        gatherer.setList(getCountriesCoords(countryCoordsList));
                                        gatherer.setQuery(query);
                                        gatherer.setRadius(radius);
                                        gatherers.add(gatherer);
                                        System.out.println("size = " + gatherer.getList().size());
                                        System.out.println(gatherer.getName() + " has started. #" + i);
                                        service.submit(gatherer);
                                        Thread.sleep(countryCoordsList.size() / 10 + 300);
                                        if (notEnoughQuota) {
                                            break;
                                        }
                                    }
                                    System.out.println("COUNT GATHERERS = " + gatherers.size());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (IOException e){
                                    e.printStackTrace();
                                    System.out.println(e.getMessage());
                                }
                            }
                        });


                        thread.start();
                        boolean temp;
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        do {
                            temp = true;
                            try {
                                if (countGatherers == currentGatherers || !thread.isAlive()) {
                                    Thread.sleep(5000);
                                    service.shutdown();
                                    temp = false;
                                }

                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } while (temp);
                        if (!notEnoughQuota)
                            nextCountry();
                        saveProp(last_coord, last_country);
                    } while (!last_country.equals("END") && !notEnoughQuota);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("FINISHED");

                }
            });
            backThread.start();
        });

        setBtn.setOnAction(event -> {
            saveProp("0","AD");
            last_country = "AD";
            last_coord = "0";
        });
    }

    private String getQuery(String query, String country) throws IOException {
        int count = 0;
        System.out.println(country);
        do {
            Locale locale = new Locale(country.toLowerCase());
            for (int i = 0; i < bundles.size(); i++) {
                if (bundles.get(i).getLocale().equals(locale)) {
                    System.out.println(bundles.get(i).getString(query.replace(" ","")).replace(" ","+"));
                    return URLEncoder.encode(bundles.get(i).getString(query.replace(" ","")).replace(" ","+"),
                            "UTF-8");
                }
            }
            bundles.add(ResourceBundle.getBundle(LOCALE_PROP, locale));
            count++;
        } while (count < 2);
        throw new IOException("Unknown query");
    }
    private synchronized void writeCountry(String country){
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("memory.csv", true));

            writer.write(country);
            writer.newLine();
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
    private ArrayList<Country> getCountriesCoords(ArrayList<Country> list){
        ArrayList<Country> countriesCoords = new ArrayList<>();
        int size;
        if (list.size() >= 200)
            size = list.size() / 200;
        else size = list.size();
        for (int i = size * numberCoords; i < numberCoords*size+size && i < list.size(); i++){
            countriesCoords.add(list.get(i));
        }
        numberCoords++;
        return countriesCoords;
    }

    private ArrayList<Country> getCoords(String country, Long id){
        ArrayList<Country> countryList = new ArrayList<>();
        BufferedReader reader = null;
        try {
            String coordLine = "";
            reader = new BufferedReader(new FileReader(COUNTRIES_COORDS));
            countCoords = 0;
            while ((coordLine = reader.readLine()) != null) {
                String splitLine[] = coordLine.split(";");
                if (splitLine[1].equals(country) && (id < Long.parseLong(splitLine[0]))){
                    countryList.add(new Country(country,
                            splitLine[2].replace(",","."),
                            splitLine[3].replace(",","."),
                            Integer.parseInt(splitLine[0])));
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

    private ArrayList<String> initCountries(){
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
    }

    private synchronized void saveProp(String id, String country){
        Properties countryProp = new Properties();
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(CONFIG_PROPERTIES);
            countryProp.load(is);
            is.close();
            countryProp.setProperty("last_coord", id);
            countryProp.setProperty("last_country", country);

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

        Gatherer() {
        }

        private void setQuery(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            if (notEnoughQuota){
                currentGatherers++;
                return;
            }
            DataGatherer dataGatherer = new DataGatherer();
            for (int i = 0; i < list.size(); i++) {
                try {
                    Pair<Map<String, String>, QuotaLimitException> gather =
                            dataGatherer.gather(list.get(i).getCountry(), Double.parseDouble(list.get(i).getLat()),
                                    Double.parseDouble(list.get(i).getLng()), query, radius);
                    companies = gather.getKey();

                    if (gather.getValue() != null) {
                        if (!notEnoughQuota) {
                            if (Long.parseLong(last_coord) > list.get(i).getID()){
                                last_coord = Long.toString(list.get(i).getID());
                            }
                            saveProp(last_coord, last_country);
                        }
                        notEnoughQuota = true;
                        saveData();
                        break;
                    } else {
                        saveData();
                        saveCoord(list.get(i).getID());
                    }
                    //Thread.sleep(list.size());

                } catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("array index out of bounds exception");
                    e.printStackTrace();
                } /*catch (InterruptedException e){
                    e.printStackTrace();
                }*/ catch (IOException e) {
                    e.printStackTrace();
                }
            }
            currentGatherers++;
        }

        private synchronized void saveCoord(Long id){
            last_coord = Long.toString(id);
        }

        private synchronized void saveData() {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter("memory.csv", true));

                for(Map.Entry<String,String> entry : companies.entrySet()){
                    boolean temp = true;
                    for(Map.Entry<String,String> entry1 : allcompanies.entrySet()){
                        if (entry1.getValue().equals(entry.getValue())){
                            temp = false;
                        }
                    }
                    if(temp) {
                        writer.write(entry.getKey() + ";" + entry.getValue());
                        writer.newLine();
                        allcompanies.put(entry.getKey(), entry.getValue());
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


    }
}
