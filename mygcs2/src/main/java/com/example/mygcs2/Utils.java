package com.example.mygcs2;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.MathUtils;
import com.naver.maps.map.NaverMap;
import com.o3dr.services.android.lib.coordinate.LatLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.o3dr.services.android.lib.util.MathUtils.getDistance2D;

public class Utils {
    public static LatLng headPointer(LatLng currentLatLngPosition, double currentYAW){
        currentYAW = (-1.0) * currentYAW;
        currentYAW = currentYAW - 90.0;

        double x = 0.003*Math.cos(Math.toRadians(currentYAW));
        double y = 0.003*Math.sin(Math.toRadians(currentYAW));
        return new LatLng(currentLatLngPosition.latitude - y, currentLatLngPosition.longitude - x);
    }


    //마커 PolyGon 정렬
    public static ArrayList<LatLng> sortLatLngArray(ArrayList<LatLng> points){
        float averageX = 0;
        float averageY = 0;
        for(LatLng coord : points){
            averageX += coord.latitude;
            averageY += coord.longitude;
        }

        final float finalAverageX = averageX / points.size();
        final float finalAverageY = averageY / points.size();

        Comparator<LatLng> comparator = (lhs, rhs) ->{
            double lhsAngle = Math.atan2(lhs.longitude - finalAverageY, lhs.latitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.longitude - finalAverageY, rhs.latitude - finalAverageX);

            if(lhsAngle < rhsAngle) return -1;
            if(lhsAngle > rhsAngle) return 1;

            return 0;
        };

        Collections.sort(points, comparator);
        return points;
    }

    public static LatLong latLngToLatLong(LatLng point) {
        return new LatLong(point.latitude, point.longitude);
    }

    public static LatLng latLongToLatLng(LatLong point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    public static double angleOfTwoPoint(LatLng pointA, LatLng pointB){
        double dx = pointB.longitude - pointB.longitude;
        double dy = pointB.latitude - pointA.latitude;

        double rad= Math.atan2(dx, dy);
        double degree = (rad*180)/Math.PI ;

        return degree;
    }

    public static void missionAtoB(ArrayList<LatLng> polygonPointArray, int distanceAtoB, double takeoffLatitude){
        polygonPointArray = sortLatLngArray(polygonPointArray);
        int maxLengthArrayNum = 0;
        double maxLength = 0;
        double angle = 0.0;
        double high = polygonPointArray.get(0).latitude;
        double low = polygonPointArray.get(0).latitude;
        double left = polygonPointArray.get(0).longitude;
        double right = polygonPointArray.get(0).longitude;

        for(int i = 1 ; i < polygonPointArray.size() ; i++){
            if(getDistance2D(Utils.latLngToLatLong(polygonPointArray.get(i-1)), Utils.latLngToLatLong(polygonPointArray.get(i)))>maxLength){
                if(high < polygonPointArray.get(i).latitude){
                    high = polygonPointArray.get(i).latitude;
                }

                if(low > polygonPointArray.get(i).latitude){
                    low = polygonPointArray.get(i).latitude;
                }

                if(left > polygonPointArray.get(i).longitude){
                    left = polygonPointArray.get(i).longitude;
                }

                if(right < polygonPointArray.get(i).longitude){
                    right = polygonPointArray.get(i).longitude;
                }

                maxLength = getDistance2D(Utils.latLngToLatLong(polygonPointArray.get(i-1)), Utils.latLngToLatLong(polygonPointArray.get(i)));
                maxLengthArrayNum = i;
            }
        }
        angle = angleOfTwoPoint(polygonPointArray.get(maxLengthArrayNum), polygonPointArray.get(maxLengthArrayNum + 1));


    }
}
