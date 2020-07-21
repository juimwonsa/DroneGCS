package com.example.mygcs2;

import com.naver.maps.geometry.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Utils {
    public static LatLng headPointer(LatLng currentLatLngPosition, double currentYAW){
        currentYAW = (-1.0) * currentYAW;
        currentYAW = currentYAW - 90.0;

        double x = 0.003*Math.cos(Math.toRadians(currentYAW));
        double y = 0.003*Math.sin(Math.toRadians(currentYAW));
        return new LatLng(currentLatLngPosition.latitude - y, currentLatLngPosition.longitude - x);
    }


    //마커 PolyGon 정렬
    public static ArrayList<LatLng> sortLatLngArray(ArrayList<LatLng> mPolyCoord){
        float averageX = 0;
        float averageY = 0;
        for(LatLng coord : mPolyCoord){
            averageX += coord.latitude;
            averageY += coord.longitude;
        }

        final float finalAverageX = averageX / mPolyCoord.size();
        final float finalAverageY = averageY / mPolyCoord.size();

        Comparator<LatLng> comparator = (lhs, rhs) ->{
            double lhsAngle = Math.atan2(lhs.longitude - finalAverageY, lhs.latitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.longitude - finalAverageY, rhs.latitude - finalAverageX);

            if(lhsAngle < rhsAngle) return -1;
            if(lhsAngle > rhsAngle) return 1;

            return 0;
        };

        Collections.sort(mPolyCoord, comparator);
        return mPolyCoord;
    }
}
