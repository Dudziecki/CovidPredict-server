package com.example.server.model;

import java.sql.Date;

public class Case {
    private int id;
    private int regionId;
    private Date date;
    private int confirmed;
    private int deaths;
    private int recovered;

    public Case(int id, int regionId, Date date, int confirmed, int deaths, int recovered) {
        this.id = id;
        this.regionId = regionId;
        this.date = date;
        this.confirmed = confirmed;
        this.deaths = deaths;
        this.recovered = recovered;
    }

    public int getId() {
        return id;
    }

    public int getRegionId() {
        return regionId;
    }

    public Date getDate() {
        return date;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getRecovered() {
        return recovered;
    }

    @Override
    public String toString() {
        return "Case{id=" + id + ", regionId=" + regionId + ", date=" + date + ", confirmed=" + confirmed + ", deaths=" + deaths + ", recovered=" + recovered + "}";
    }
}