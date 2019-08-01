package com.example.busstation.until;

import com.amap.api.services.busline.BusLineResult;
import com.amap.api.services.busline.BusStationItem;

import java.util.HashMap;
import java.util.List;

public class MessageEvent{
    private HashMap message;
    private List<BusStationItem> busStations;
    private BusLineResult busLineResult;
    public  MessageEvent(HashMap message){
        this.message=message;
    }
    public  MessageEvent(){
    }

    public HashMap getMessage() {
        return message;
    }

    public void setMessage(HashMap message) {
        this.message = message;
    }

    public List<BusStationItem> getBusStations() {
        return busStations;
    }

    public void setBusStations(List<BusStationItem> busStations) {
        this.busStations = busStations;
    }

    public BusLineResult getBusLineResult() {
        return busLineResult;
    }

    public void setBusLineResult(BusLineResult busLineResult) {
        this.busLineResult = busLineResult;
    }
}