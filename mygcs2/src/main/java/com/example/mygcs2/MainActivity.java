package com.example.mygcs2;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.ArrowheadPathOverlay;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.DecoderListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.o3dr.android.client.apis.ExperimentalApi.getApi;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ArrowheadPathOverlay arrowheadPath = new ArrowheadPathOverlay();
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private LocationOverlay locationOverlay;
    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;
    private PolylineOverlay polyline = new PolylineOverlay();
    private Spinner modeSelector;
    private double initAltitude = 3.0;
    private Button startVideoStream;
    private Button stopVideoStream;
    private Button startVideoStreamUsingObserver;
    private Button stopVideoStreamUsingObserver;

    private LatLng pointForGuideMode;

    private MediaCodecManager mediaCodecManager;

    private TextureView videoView;

    private String videoTag = "testvideotag";
    private Marker droneMarker = new Marker();
    Handler mainHandler;

    private ArrayList<LatLng> polyLineCoords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideNavigationBar();
        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                TextView textView = ((TextView) parent.getChildAt(0));
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Initialize media codec manager to decode video stream packets.
        HandlerThread mediaCodecHandlerThread = new HandlerThread("MediaCodecHandlerThread");
        mediaCodecHandlerThread.start();
        Handler mediaCodecHandler = new Handler(mediaCodecHandlerThread.getLooper());
        mediaCodecManager = new MediaCodecManager(mediaCodecHandler);

        mainHandler = new Handler(getApplicationContext().getMainLooper());


        // ↓아래로 원본 onCreate

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void onClick_Main(View view){
        hideNavigationBar();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // DroneKit-Android Listener
    // ==========================================================

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Drone Listener
    // ==========================================================

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateVoltage();
                break;

            case AttributeEvent.GPS_POSITION:
                updateDroneMarker();
                break;

            case AttributeEvent.GPS_COUNT:
                updateSattle();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYAW();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    // UI Events
    // ==========================================================

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected())
        {
            this.drone.disconnect();
        }
        else
        {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    public void onBtnAltitudeTextTap(View view){
        Button btnAltitudeUp = (Button) findViewById(R.id.btnAltitudeUp);
        Button btnAltitudeDown= (Button) findViewById(R.id.btnAltitudeDown);
        if(btnAltitudeDown.getVisibility() == View.INVISIBLE) {
            btnAltitudeDown.setVisibility(View.VISIBLE);
            btnAltitudeUp.setVisibility(View.VISIBLE);
        }
        else{
            btnAltitudeDown.setVisibility(View.INVISIBLE);
            btnAltitudeUp.setVisibility(View.INVISIBLE);
        }
    }

    public void onBtnAltitudeUpTap(View view){
        Button btnAltitudeText = (Button)findViewById(R.id.btnAltitudeText);
        if(initAltitude < 10.0) {
            initAltitude += 0.5;
            btnAltitudeText.setText(String.format("%3.1fm", initAltitude));
        }
        else    alertUser(DroneMSG.MSG_OVERNUMBER);

    }

    public void onBtnAltitudeDownTap(View view){
        Button btnAltitudeText = (Button)findViewById(R.id.btnAltitudeText);

        if(initAltitude > 3) {
            initAltitude -= 0.5;
            btnAltitudeText.setText(String.format("%3.1fm", initAltitude));
        }
        else    alertUser(DroneMSG.MSG_UNDERNUMBER);
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser(DroneMSG.ERR_UNABLELANDING);
                }

                @Override
                public void onTimeout() {
                    alertUser(DroneMSG.ERR_UNABLELANDING);
                }

                @Override
                public void onSuccess(){
                    alertUser("Landing...");
                    Button button = (Button)findViewById(R.id.btnArmTakeOff);
                    button.setText("LAND");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("경고");
            builder.setMessage(DroneMSG.MSG_TAKINGOFF);

            builder.setPositiveButton(DroneMSG.MSG_YES, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ControlApi.getApi(MainActivity.this.drone).takeoff(initAltitude, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            alertUser("Taking off...");
                            Button button = (Button)findViewById(R.id.btnArmTakeOff);
                            button.setText("LAND");
                        }

                        @Override
                        public void onError(int i) {
                            alertUser(DroneMSG.ERR_UNABLETAKEOFF);
                        }

                        @Override
                        public void onTimeout() {
                            alertUser(DroneMSG.ERR_UNABLETAKEOFF);
                        }
                    });
                }
            });

            builder.setNegativeButton(DroneMSG.MSG_NO, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertUser(DroneMSG.MSG_CANCLED);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();


        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser(DroneMSG.ERR_UNCONNECTED);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("경고");
            builder.setMessage(DroneMSG.MSG_LAUNCHMOTER);

            builder.setPositiveButton(DroneMSG.MSG_YES, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VehicleApi.getApi(MainActivity.this.drone).arm(true, false, new SimpleCommandListener() {
                        @Override
                        public void onError(int executionError) {
                            alertUser(DroneMSG.ERR_UNABLEARM);
                        }

                        @Override
                        public void onTimeout() {
                            alertUser(DroneMSG.ERR_ARMING_TIMEOUT);
                        }

                        @Override
                        public void onSuccess() {
                            super.onSuccess();
                            Button button = (Button)findViewById(R.id.btnArmTakeOff);
                            button.setText("Take-Off");
                        }
                    });
                }
            });

            builder.setNegativeButton(DroneMSG.MSG_NO, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertUser(DroneMSG.MSG_CANCLED);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            // Connected but not Armed
        }
    }

    // UI updating
    // ==========================================================

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }


    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateYAW(){
        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        yawTextView.setText(String.format("%3.1f", attitude.getYaw()) + "deg");
    }

    protected void updateSattle(){
        TextView sattleCountTextView = (TextView) findViewById(R.id.settleCountTextView);
        Gps gps = this.drone.getAttribute(AttributeType.GPS);
        sattleCountTextView.setText((String.format("%d", gps.getSatellitesCount())));
    }

    protected void updateVoltage(){
        TextView voltageValueTextView = (TextView) findViewById(R.id.voltageValueTextView);
        Battery battery = this.drone.getAttribute(AttributeType.BATTERY);
        voltageValueTextView.setText(String.format("%3.1f", battery.getBatteryVoltage()));
    }


    protected void updateDroneMarker(){
        LatLong currentLatlongLocation = getCurrentLocation();
        LatLng currentLatlngLocation = new LatLng(currentLatlongLocation.getLatitude(), currentLatlongLocation.getLongitude());
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);

        this.locationOverlay = naverMap.getLocationOverlay();
        this.locationOverlay.setPosition(currentLatlngLocation);
        this.locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.droneicon));
        this.locationOverlay.setBearing((float)attitude.getYaw());

        polyLineCoords.add(currentLatlngLocation);
        polyline.setCoords(polyLineCoords);
        polyline.setMap(naverMap);

        ArrayList<LatLng> arrowheads = new ArrayList<LatLng>();
        arrowheads.add(currentLatlngLocation);
        arrowheads.add(Utils.headPointer(currentLatlngLocation, attitude.getYaw()));
        this.arrowheadPath.setCoords(arrowheads);
        this.locationOverlay.setVisible(true);
        this.arrowheadPath.setMap(naverMap);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatlngLocation);
        naverMap.moveCamera(cameraUpdate);

        Gps gps = this.drone.getAttribute(AttributeType.GPS);

        if((GuideMode.markerGuide.getMap()!=null) && (GuideMode.CheckGoal(this.drone, new LatLng(gps.getPosition().getLatitude(), gps.getPosition().getLongitude())))){
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_ALT_HOLD);
            GuideMode.markerGuide.setMap(null);
            alertUser(DroneMSG.MSG_GUIDEMODEEND);
        }
    }

    protected LatLong getCurrentLocation(){
        Gps gps = this.drone.getAttribute(AttributeType.GPS);
        return gps.getPosition();
    }


/*
    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }
*/

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);

    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }



    // Helper methods
    // ==========================================================

    private void hideNavigationBar() {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.d(TAG, "Turning immersive mode mode off. ");
        } else {
            Log.d(TAG, "Turning immersive mode mode on.");
        }
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void takePhoto() {
        SoloCameraApi.getApi(drone).takePhoto(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Photo taken.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while trying to take the photo: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timeout while trying to take the photo.");
            }
        });
    }

    private void toggleVideoRecording() {
        SoloCameraApi.getApi(drone).toggleVideoRecording(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Video recording toggled.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while trying to toggle video recording: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timeout while trying to toggle video recording.");
            }
        });
    }

    private void startVideoStream(Surface videoSurface) {
        SoloCameraApi.getApi(drone).startVideoStream(videoSurface, videoTag, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Successfully started the video stream. ");

                if (stopVideoStream != null)
                    stopVideoStream.setEnabled(true);

                if (startVideoStream != null)
                    startVideoStream.setEnabled(false);

                if (startVideoStreamUsingObserver != null)
                    startVideoStreamUsingObserver.setEnabled(false);

                if (stopVideoStreamUsingObserver != null)
                    stopVideoStreamUsingObserver.setEnabled(false);
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while starting the video stream: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timed out while attempting to start the video stream.");
            }
        });
    }

    DecoderListener decoderListener = new DecoderListener() {
        @Override
        public void onDecodingStarted() {
            alertUser("MediaCodecManager: video decoding started...");
        }

        @Override
        public void onDecodingError() {
            alertUser("MediaCodecManager: video decoding error...");
        }

        @Override
        public void onDecodingEnded() {
            alertUser("MediaCodecManager: video decoding ended...");
        }
    };

    private void startVideoStreamForObserver() {
        getApi(drone).startVideoStream(videoTag, new ExperimentalApi.IVideoStreamCallback() {
            @Override
            public void onVideoStreamConnecting() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully obtained lock for drone video stream.");
                    }
                });
            }

            @Override
            public void onVideoStreamConnected() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully opened drone video connection.");

                        if (stopVideoStreamUsingObserver != null)
                            stopVideoStreamUsingObserver.setEnabled(true);

                        if (startVideoStreamUsingObserver != null)
                            startVideoStreamUsingObserver.setEnabled(false);

                        if (stopVideoStream != null)
                            stopVideoStream.setEnabled(false);

                        if (startVideoStream != null)
                            startVideoStream.setEnabled(false);
                    }
                });

                mediaCodecManager.stopDecoding(new DecoderListener() {
                    @Override
                    public void onDecodingStarted() {
                    }

                    @Override
                    public void onDecodingError() {
                    }

                    @Override
                    public void onDecodingEnded() {
                        try {
                            mediaCodecManager.startDecoding(new Surface(videoView.getSurfaceTexture()),
                                    decoderListener);
                        } catch (IOException | IllegalStateException e) {
                            Log.e(TAG, "Unable to create media codec.", e);
                            if (decoderListener != null)
                                decoderListener.onDecodingError();
                        }
                    }
                });
            }

            @Override
            public void onVideoStreamDisconnecting() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully released lock for drone video stream.");
                    }
                });
            }

            @Override
            public void onVideoStreamDisconnected() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully closed drone video connection.");

                        if (stopVideoStreamUsingObserver != null)
                            stopVideoStreamUsingObserver.setEnabled(false);

                        if (startVideoStreamUsingObserver != null)
                            startVideoStreamUsingObserver.setEnabled(true);

                        if (stopVideoStream != null)
                            stopVideoStream.setEnabled(false);

                        if (startVideoStream != null)
                            startVideoStream.setEnabled(true);
                    }
                });

                mediaCodecManager.stopDecoding(decoderListener);
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while getting lock to vehicle video stream: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timed out while attempting to get lock for vehicle video stream.");
            }

            @Override
            public void onAsyncVideoStreamPacketReceived(byte[] data, int dataSize) {
                mediaCodecManager.onInputDataReceived(data, dataSize);
            }
        });
    }

    private void stopVideoStream() {
        SoloCameraApi.getApi(drone).stopVideoStream(videoTag, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (stopVideoStream != null)
                    stopVideoStream.setEnabled(false);

                if (startVideoStream != null)
                    startVideoStream.setEnabled(true);

                if (stopVideoStreamUsingObserver != null)
                    stopVideoStreamUsingObserver.setEnabled(false);

                if (startVideoStreamUsingObserver != null)
                    startVideoStreamUsingObserver.setEnabled(true);
            }

            @Override
            public void onError(int executionError) {
            }

            @Override
            public void onTimeout() {
            }
        });
    }

    private void stopVideoStreamForObserver() {
        getApi(drone).stopVideoStream(videoTag);
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.getUiSettings().setZoomControlEnabled(false);

        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                //makeMarker(naverMap,latLng);
                //makeInfoWindow("test",naverMap);
            }
        });

        naverMap.setOnMapLongClickListener((point, coord) ->{
            this.pointForGuideMode = new LatLng(coord.latitude, coord.longitude);
            if (this.modeSelector.getSelectedItem().toString().equals("Guided")) {
                GuideMode.GuideModeStart(this.drone, new LatLong(coord.latitude, coord.longitude), naverMap);
            } else {
                GuideMode.GuideModeStart(this.drone, new LatLong(coord.latitude, coord.longitude), MainActivity.this, naverMap);
            }
        });

        //naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void test_btn(View view) {
    }

    public void btn_hybrid(View view) {
        naverMap.setMapType(NaverMap.MapType.Hybrid);
    }

    public void btn_basic(View view) {
        naverMap.setMapType(NaverMap.MapType.Basic);
    }

    private void makeMarker(@NonNull NaverMap naverMap, @NonNull LatLng latLng){
        Marker marker = new Marker();
        marker.setPosition(latLng);
        marker.setMap(naverMap);
    }

    //TODO 앱 종료됨
    public void makeInfoWindow(String msg, NaverMap targetMap){
        InfoWindow infoWindow = new InfoWindow();
        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getBaseContext()) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return msg;
            }
        });
        infoWindow.open(targetMap);
    }
}

/*

final Button takePic = (Button) findViewById(R.id.take_photo_button);
        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

 final Button toggleVideo = (Button) findViewById(R.id.toggle_video_recording);
        toggleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVideoRecording();
            }
        });
 startVideoStream = (Button) findViewById(R.id.start_video_stream);
        startVideoStream.setEnabled(false);
        startVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Starting video stream.");
                startVideoStream(new Surface(videoView.getSurfaceTexture()));
            }
        });

        stopVideoStream = (Button) findViewById(R.id.stop_video_stream);
        stopVideoStream.setEnabled(false);
        stopVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Stopping video stream.");
                stopVideoStream();
            }
        });

        startVideoStreamUsingObserver = (Button) findViewById(R.id.start_video_stream_using_observer);
        startVideoStreamUsingObserver.setEnabled(false);
        startVideoStreamUsingObserver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Starting video stream using observer for video stream packets.");
                startVideoStreamForObserver();
            }
        });

        stopVideoStreamUsingObserver = (Button) findViewById(R.id.stop_video_stream_using_observer);
        stopVideoStreamUsingObserver.setEnabled(false);
        stopVideoStreamUsingObserver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Stopping video stream using observer for video stream packets.");
                stopVideoStreamForObserver();
            }
        });


        videoView = (TextureView) findViewById(R.id.video_content);
        videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                alertUser("Video display is available.");
                startVideoStream.setEnabled(true);
                startVideoStreamUsingObserver.setEnabled(true);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                startVideoStream.setEnabled(false);
                startVideoStreamUsingObserver.setEnabled(false);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

































 */