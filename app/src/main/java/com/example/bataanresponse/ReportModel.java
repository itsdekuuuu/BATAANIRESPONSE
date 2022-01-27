package com.example.bataanresponse;

public class ReportModel {
    private String title;
    private String location;
    private String time;
    private String date;
    private String people;
    private String inclass;
    private String indes;
    private String resid;
    private String readd;

    public ReportModel() {
    }

    public ReportModel(String title, String location, String time, String date, String people, String inclass, String indes, String resid, String readd) {
        this.title = title;
        this.location = location;
        this.time = time;
        this.date = date;
        this.people = people;
        this.inclass = inclass;
        this.indes = indes;
        this.resid = resid;
        this.readd = readd;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPeople() {
        return people;
    }

    public void setPeople(String people) {
        this.people = people;
    }

    public String getInclass() {
        return inclass;
    }

    public void setInclass(String inclass) {
        this.inclass = inclass;
    }

    public String getIndes() {
        return indes;
    }

    public void setIndes(String indes) {
        this.indes = indes;
    }

    public String getResid() {
        return resid;
    }

    public void setResid(String resid) {
        this.resid = resid;
    }

    public String getReadd() {
        return readd;
    }

    public void setReadd(String readd) {
        this.readd = readd;
    }
}
