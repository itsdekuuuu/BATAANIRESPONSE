package com.example.bataanresponse.Resident;

public class EventsModel {
    private String adminadd;
    private String what;
    private String when;
    private String where;
    private String des;

    public EventsModel() {
    }


    public EventsModel(String adminadd, String what, String when, String where, String des) {
        this.adminadd = adminadd;
        this.what = what;
        this.when = when;
        this.where = where;
        this.des = des;
    }

    public String getAdminadd() {
        return adminadd;
    }

    public void setAdminadd(String adminadd) {
        this.adminadd = adminadd;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }
}
