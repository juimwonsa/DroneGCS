package com.example.mygcs2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.MarkerIcons;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

public class GuideMode {
    static LatLng guidedPoint;
    static Marker markerGuide = new com.naver.maps.map.overlay.Marker();
    //static OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.guidemodemarker);

    static void GuideModeStart(final Drone drone, final LatLong point, final Context context, final NaverMap naverMap){
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(context);
        alt_bld.setMessage(DroneMSG.MSG_GUIDEMODEON).setCancelable(false).setPositiveButton(DroneMSG.MSG_YES, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener(){
                    @Override
                    public void onSuccess(){
                        ControlApi.getApi(drone).goTo(point,true, null);
                        markerGuide.setPosition(new LatLng(point.getLatitude(),point.getLongitude()));
                        markerGuide.setIcon(MarkerIcons.BLACK);
                        markerGuide.setMap(naverMap);
                    }

                    @Override
                    public void onError(int i){
                    }

                    @Override
                    public void onTimeout(){
                    }
                });
            }
        }).setNegativeButton(DroneMSG.MSG_NO, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alt_bld.create();
        alert.setTitle("Title");
        alert.show();
    }

    public static void GuideModeStart(final Drone drone, final LatLong point, final NaverMap naverMap){
        ControlApi.getApi(drone).goTo(point,true, null);
        markerGuide.setPosition(new LatLng(point.getLatitude(),point.getLongitude()));
        markerGuide.setIcon(MarkerIcons.BLACK);
        markerGuide.setMap(naverMap);
    }

    public static boolean CheckGoal(final Drone drone, LatLng recentLatLng){
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
        return  target.distanceTo(recentLatLng) <= 1;
    }
}
