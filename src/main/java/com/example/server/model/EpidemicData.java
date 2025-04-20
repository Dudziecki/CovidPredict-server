package com.example.server.model;

public class EpidemicData {
    private int id;
    private String username;
    private String region;
    private String date;
    private int infected;

    public EpidemicData() {
    }

    public EpidemicData(String region, String date, int infected) {
        this.region = region;
        this.date = date;
        this.infected = infected;
    }

    public EpidemicData(int id, String username, String region, String date, int infected) {
        this.id = id;
        this.username = username;
        this.region = region;
        this.date = date;
        this.infected = infected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getInfected() {
        return infected;
    }

    public void setInfected(int infected) {
        this.infected = infected;
    }
}