package com.whaletail.app;

/**
 * Created by Admin on 26.04.2017.
 */
public class Country {
    private String country;
    private String lat;
    private String lng;
    private int ID;

    public Country(){
        country = "";
        lat = "";
        lng = "";
        ID = 0;
    }
    public Country(String country, String lat, String lng, int ID){
        this.country = country;
        this.lat = lat;
        this.lng = lng;
        this.ID = ID;
    }
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public long getID() {
        return ID;
    }
}
