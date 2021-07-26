package com.example.racecarevents.model;

public class RacecarRecord {

    private Integer raceId;
    private Integer driverId;
    private Integer lap;
    private Integer position;
    private String time;
    private Integer milliseconds;
    private String driverRef;
    private String forename;
    private String surname;
    private String dob;
    private String nationality;
    private String url;


    public Integer getraceId() {
        return raceId;
    }

    public void setRaceId(Integer raceId) {
        this.raceId = raceId;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public Integer getLap() {
        return lap;
    }

    public void setLap(Integer lap) {
        this.lap = lap;
    }

    public Integer getPosition() { return position; }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(Integer Milliseconds) {
        this.milliseconds = milliseconds;
    }

    public String getDriverRef() {
        return driverRef;
    }

    public void setDriverRef(String driverRef) {
        this.driverRef = driverRef;
    }

    public String getForename() {
        return forename;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "RacecarRecord{" +
                "raceId=" + raceId +
                ", driverId=" + driverId +
                ", lap=" + lap +
                ", time=" + time +
                ", milliseconds='" + milliseconds +
                ", driverRef='" + driverRef +
                ", forename='" + forename +
                ", surname='" + surname +
                ", dob='" + dob +
                ", nationality='" + nationality +
                ", url='" + url + '\'' +
                '}';
    }
}
