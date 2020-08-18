package com.example.mygcs2;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.MathUtils;
import com.naver.maps.map.NaverMap;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.o3dr.services.android.lib.util.MathUtils.addDistance;
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

        double left = 400;
        int index = 0;
        int indexOfLeftCoord = 0;
        for(LatLng coord : points){
            if(coord.longitude < left){
                left = coord.longitude;
                indexOfLeftCoord = index;
            }
            index++;
        }

        ArrayList<LatLng> copyArray = new ArrayList<>();

        for(int i = 0  ; i < indexOfLeftCoord; i++ ){
            copyArray.add(points.get(0));
            points.remove(0);

        }
        for(int i = 0 ; i < indexOfLeftCoord ; i++){
            points.add(copyArray.get(i));
        }

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

    public static ArrayList<LatLng> sortArrayForMission(ArrayList<LatLng> missionArray){
        int sizeOfArrayForSort = missionArray.size() / 4;
        for(int i = 0 ; i < sizeOfArrayForSort ; i++){
            Collections.swap(missionArray, ((4*i)+2), ((4*i)+3));
        }
        return missionArray;
    }

    public static LatLng getCrossPoint(LatLng first, LatLng second, LatLng third, LatLng fourth) {
        double x = (((first.longitude*second.latitude - first.latitude*second.longitude)*(third.longitude - fourth.longitude) - (first.longitude - second.longitude)*(third.longitude*fourth.latitude-third.latitude*fourth.longitude))/((first.longitude - second.longitude)*(third.latitude - fourth.latitude) - (first.latitude - second.latitude)*(third.longitude - fourth.longitude)));
        double y = (((first.longitude*second.latitude - first.latitude*second.longitude)*(third.latitude - fourth.latitude) - (first.latitude - second.latitude)*(third.longitude*fourth.latitude-third.latitude*fourth.longitude))/((first.longitude - second.longitude)*(third.latitude - fourth.latitude) - (first.latitude - second.latitude)*(third.longitude - fourth.longitude)));
        return new LatLng(y, x);
    }
}