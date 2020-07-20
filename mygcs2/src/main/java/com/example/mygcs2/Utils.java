package com.example.mygcs2;

import com.naver.maps.geometry.LatLng;

public class Utils {
    public static LatLng headPointer(LatLng currentLatLngPosition, double currentYAW){
        if(currentYAW < 0){
            currentYAW += 180.0;
        }
        currentYAW = currentYAW - 90.0;

        double x = 0.01*Math.cos(Math.toRadians(currentYAW));
        double y = 0.01*Math.sin(Math.toRadians(currentYAW));
        return new LatLng(currentLatLngPosition.latitude - y, currentLatLngPosition.longitude - x);
    }
}
