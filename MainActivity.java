///=================================================================================================
// Class MainActivity
//      Author :  Jose Gilberto RESENDIZ FONSECA
// Modified by :  Antoine GRENIER - 2019/09/06
// Modified by :  Yazheng WEI     - 2019/??/??
//        Date :  2019/09/06
///=================================================================================================
/*
 * Copyright 2018(c) IFSTTAR - TeamGEOLOC
 *
 * This file is part of the GeolocPVT application.
 *
 * GeolocPVT is distributed as a free software in order to build a community of users, contributors,
 * developers who will contribute to the project and ensure the necessary means for its evolution.
 *
 * GeolocPVT is a free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version. Any modification of source code in this
 * LGPL software must also be published under the LGPL license.
 *
 * GeolocPVT is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Lesser General Public License along with GeolocPVT.
 * If not, see <https://www.gnu.org/licenses/lgpl.txt/>.
 */
///=================================================================================================
package fr.ifsttar.geoloc.geolocpvt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import fr.ifsttar.geoloc.geoloclib.FileLogger;
import fr.ifsttar.geoloc.geoloclib.IMU.IMUdata;
import fr.ifsttar.geoloc.geoloclib.Options;
import fr.ifsttar.geoloc.geoloclib.Utils;
import fr.ifsttar.geoloc.geoloclib.computations.GNSSPositioning;
import fr.ifsttar.geoloc.geoloclib.Coordinates;
import fr.ifsttar.geoloc.geoloclib.satellites.EphemerisGPS;
import fr.ifsttar.geoloc.geoloclib.satellites.SatellitePositionGNSS;
import fr.ifsttar.geoloc.geoloclib.streams.StreamEphemerisHandler;
import fr.ifsttar.geoloc.geoloclib.streams.Streams;
import fr.ifsttar.geoloc.geolocpvt.fragments.LoggerFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.MapFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.MonitorFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.OptionsFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.PseudorangeFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.ReferenceFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.SatelliteFragment;
import fr.ifsttar.geoloc.geolocpvt.fragments.StreamFragment;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.content.ContentValues.TAG;

import org.gogpsproject.ephemeris.PreciseCorrection;
import org.gogpsproject.ephemeris.GNSSEphemeris;
import org.gogpsproject.ephemeris.GNSSEphemerisCorrections;
import org.gogpsproject.ephemeris.SatelliteCodeBiases;
import org.gogpsproject.positioning.SatellitePosition;

/**
 * MainActivity
 * All of the process of application
 */
public class MainActivity extends AppCompatActivity
        implements  BottomNavigationView.OnNavigationItemSelectedListener, Button.OnClickListener
{
    private int PERMISSIONS_CODE = 8964;

    //the 5 fragments or views of the application
    private MonitorFragment mMonitorFragment;
    private StreamFragment mStreamFragment;
    private PseudorangeFragment mPseudorangeFragment;
    private SatelliteFragment mSatelliteFragment;
    private MapFragment mMapFragment;
    private LoggerFragment mLoggerFragment;
    private OptionsFragment mOptionsFragment;
    private ReferenceFragment mReferenceFragment;

    // For GNSS positioning
    private GnssMeasurementsEvent.Callback mGnssMeasurementsEventCallback;
    private GnssNavigationMessage.Callback mGnssNavigationMessageCallback;
    private OnNmeaMessageListener nmeaMessageListener;
    private GnssMeasurementsEvent mGnssMeasurementsEvent;
    private Map<Integer, Map<Integer, Map<Integer,GnssNavigationMessage>>> mMapMessages;
    private HashMap<String, GNSSEphemeris> satelliteEphemeris;
    private LocationManager mLocationManager;
    private IMUdata imuData;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationListener locationListener;

    // GUI
    private BottomNavigationView navigation;
    private Button btnStart;
    private Button btnStop;
    private CheckBox checkPseudo;
    private CheckBox checkEphem;
    private CheckBox checkPos;
    private CheckBox checkIMU;

    // Other
    private int currentFragment;
    private double timeOfDay;
    private double timeOfWeek;

    FileLogger fileLogger;

    public Streams streams;
    public HashMap<String,Boolean> mountpointsMap;

    private Options processingOptions;

    GNSSPositioning gnssPositioning;

    private Coordinates approxUserCoord;

    Coordinates userCoord;

    //----------------------------------------------------------------------------------------------

    /**
     * method to get item selected on NavigationView
     **/
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //getting fragment to load
        currentFragment = item.getItemId();
        return refreshingFragment(true);
    }

    //----------------------------------------------------------------------------------------------

    /**
     * function to make the transaction of fragments in the main fragment
     * @param fragment Fragment to be loaded
     * @return Success
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    //replace fragment in fragment_main, by fragment
                    .replace(R.id.fragment_main, fragment)
                    .commitAllowingStateLoss();

            return true;
        }
        return false;
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Refresh displayed data.
     * @param switching Switch current fragment
     * @return Success
     */
    private boolean refreshingFragment(boolean switching){
        Fragment fragment = null;
        Bundle bundle = new Bundle();
        Coordinates co;
        switch (currentFragment) {
            case R.id.navigation_monitor:

                //if item equals to monitor, fragment to load is equal to MonitorFragment
                fragment = mMonitorFragment;
                bundle.putParcelable("GnssMeasurementsEvent", mGnssMeasurementsEvent);
                try
                {
                    bundle.putSerializable("ComputedPosition", userCoord);
                    bundle.putDouble("GDOP", gnssPositioning.getGdop());
                }
                catch(Exception e)
                {
                    //e.printStackTrace();
                }

                bundle.putDouble("Time",timeOfDay);

                if(gnssPositioning != null)
                {
                    bundle.putSerializable("TrackedObservations", gnssPositioning.getGnssObservationTrackedSats());
                    bundle.putSerializable("GnssObservations", gnssPositioning.getGnssObservationAllSats());
                }
                break;
            case R.id.navigation_stream:
                fragment = mStreamFragment;

                bundle.putSerializable("Mountpoints", mountpointsMap);

                break;
                /*
            case R.id.navigation_pseudorange:
                //if item selected equals to pseudorange, fragment to load is equal to PseudorangeFragment
                fragment = mPseudorangeFragment;
0                bundle.putParcelable("GnssMeasurementsEvent", mGnssMeasurementsEvent);
                bundle.putSerializable("SatelliteEphemeris", satelliteEphemeris);
                break;*/

            case R.id.navigation_satellite:
                //if item selected equals to satellite, fragment to load is equal to SatelliteFragment
                // to get all the satellite id
                try {
                    //MONGI
                    bundle.putSerializable("GnssObservations", gnssPositioning.getGnssObservationAllSats());
                    bundle.putSerializable("TrackedObservations", gnssPositioning.getGnssObservationTrackedSats());
                    bundle.putSerializable("ComputedPosition", userCoord);
                }  catch(Exception e) { }
                fragment = mSatelliteFragment;
                break;

            case R.id.navigation_map:
                //if item selected equals to map, fragment to load is equal to MapFragment
                fragment = mMapFragment;
                try {
                    bundle.putDouble("Lat", userCoord.getLatLngAlt().getLatitude());
                    bundle.putDouble("Lon", userCoord.getLatLngAlt().getLongitude());

                    if(mReferenceFragment.getReference() != null)
                    {
                        bundle.putSerializable("ReferencePosition", mReferenceFragment.getReference());
                    }
                }
                catch (Exception e)
                {e.printStackTrace();}

                break;

                /*
            case R.id.navigation_logger:
                //if item selected equals to logger, fragment to load is equal to LoggerFragment
                fragment = mLoggerFragment;
                break;
                 */
/*
            case R.id.navigation_reference:
                fragment = mReferenceFragment;

                try
                {
                    if(userCoord.getX() != 0)
                    {
                        bundle.putSerializable("ComputedPosition", userCoord);
                    }
                }
                catch (Exception e)
                {e.printStackTrace();}

                break;
*/
            case R.id.navigation_options:
                fragment = mOptionsFragment;
                break;
        }

        //if any data have been added to bundle
        if(!bundle.isEmpty()){
            //we don't set arguments
            fragment.setArguments(bundle);
        }

        //if we are switching fragments
        if(switching){
            //we change fragment to display
            return loadFragment(fragment);
        }else{
            //we only refresh new arguments set
            switch (currentFragment){
                case R.id.navigation_monitor:
                    mMonitorFragment.refreshData();
                    break;

                case R.id.navigation_stream:
                    mStreamFragment.refreshData();
                    /*
                case R.id.navigation_pseudorange:
                    mPseudorangeFragment.refreshData();
                    break;
                     */

                case R.id.navigation_satellite:
                    mSatelliteFragment.refreshData();
                    break;


                case R.id.navigation_map:
                    mMapFragment.refreshData();
                    break;
/*
                case R.id.navigation_reference:
                    mReferenceFragment.refreshData();
                    break;
*/
                    /*
                case R.id.navigation_logger:
                    mLoggerFragment.refreshData();
                    break;
                     */

                case R.id.navigation_options:
                    mOptionsFragment.refreshData();
                    break;
            }
            return true;
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * @param savedInstanceState Previous saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initializing the manager for the location services
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //initializing a BottomNavigationView
        navigation = findViewById(R.id.navigation);

        //setting a OnNavigationItemSelectedListener to the BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(this);

        //initializing fragments
        mMonitorFragment = new MonitorFragment();
        mPseudorangeFragment = new PseudorangeFragment();
        mSatelliteFragment = new SatelliteFragment();
        mMapFragment = new MapFragment();
        mLoggerFragment = new LoggerFragment();
        mOptionsFragment = new OptionsFragment();
        mStreamFragment = new StreamFragment();
        mReferenceFragment = new ReferenceFragment();
        imuData = new IMUdata();

        //some graphic objects
        btnStart = findViewById(R.id.buttonStart);
        btnStart.setOnClickListener(this);
        btnStop = findViewById(R.id.buttonStop);
        btnStop.setOnClickListener(this);
        btnStop.setEnabled(false);
        checkPseudo = findViewById(R.id.checkBoxPseudorange);
        checkEphem = findViewById(R.id.checkBoxEphemeris);
        checkPos = findViewById(R.id.checkBoxPosition);
        checkIMU = findViewById(R.id.checkBoxImu);

        processingOptions = new Options();
        // Small trick to load the options fragment in the app, otherwise error on start up
        // TODO: find a better way or don't touch it...
        currentFragment = R.id.navigation_options;
        refreshingFragment(true);

        // Same trick to get the mountpoints
        currentFragment = R.id.navigation_stream;
        mountpointsMap = new HashMap<>();
        mountpointsMap.put("RTCM3EPH", false);
        mountpointsMap.put("CLK93", false);
        refreshingFragment(true);

        //initializing time
        timeOfDay = 0;
        timeOfWeek = 0;

        //setting fragment by default
        currentFragment = R.id.navigation_monitor;
        refreshingFragment(true);

        //stabilising permissions
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.INTERNET,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_COARSE_LOCATION};

        //setting callbacks for raw measurements
        settingCallbacks();

        //checking permissions
        if(!checkingPermissions(permissions)){
            requestPermission(permissions);
        }else{
            try
            {
                mLocationManager.registerGnssMeasurementsCallback(mGnssMeasurementsEventCallback);
                mLocationManager.registerGnssNavigationMessageCallback(mGnssNavigationMessageCallback);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
                mLocationManager.addNmeaListener(nmeaMessageListener);

                // Get approx position
                getApproxPosition();

            }catch (SecurityException e){
                e.printStackTrace();
            }

        }


    }

    //----------------------------------------------------------------------------------------------

    public void connectToMountpoints()
    {
        //setting the streams gathering
        //Log.e("STREAMS", "Trying to connect the streams...");

        if(streams == null)
        {
            mStreamFragment.refreshStreamOptions();
            this.streams = new Streams(mStreamFragment.getConnectionParameters());
        }

        for(HashMap.Entry<String,Boolean> entry: mountpointsMap.entrySet())
        {
            if(!(entry.getValue()))
            {
                boolean success = this.streams.registerCorrectionStream(entry.getKey());

                entry.setValue(success);
            }
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Get approximate position from the fusedLocationClient (Google API)
     */
    private void getApproxPosition()
    {
        try
        {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                approxUserCoord = new Coordinates(location);
                            }
                        }
                    });
        }
        catch (SecurityException e)
        {
            Log.e("ERR", "No location from fusedLocationClient.");
            e.printStackTrace();
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Handler of new GNSS measurement event
     */
    private void gnssMeasurementsHandler()
    {
        // Checking if first event
        if(gnssPositioning == null)
        {
            gnssPositioning = new GNSSPositioning();
        }

        if(gnssPositioning.getPosition() == null)
        {
            if(approxUserCoord == null)
            {
                getApproxPosition();
            }

            gnssPositioning.setApproxPosition(approxUserCoord);
        }

        // Refreshing the computations option
        processingOptions = mOptionsFragment.getProcessingOptions();
        gnssPositioning.refreshOptions(processingOptions);

        // Get satellites positions, either by navigation message or by streams
        if(processingOptions.isStreamsEnabled())
        {
           // getSatPositionFromStreams();
            getSatPositionFromNavigationMessage();
        }
        else
        {
            getSatPositionFromNavigationMessage();
        }

        // Refreshing with last satellite ephemeris and measurements
        gnssPositioning.refreshEphemeris(satelliteEphemeris);
        gnssPositioning.refreshMeasurements(mGnssMeasurementsEvent);
        userCoord = gnssPositioning.computeUserPosition();

        // Compute a position with current data
        /*
        Iterator hmIterator = satelliteEphemeris.entrySet().iterator();
        Map.Entry teste = (Map.Entry)hmIterator.next();
        Log.i("igor aqui",teste.getValue().toString());
        Log.i("igor 2",teste.getValue().toString());
         */

        /*
        while (hmIterator.hasNext()) {
            Map.Entry teste = (Map.Entry)hmIterator.next();
            Log.i("igor aqui",teste.toString());
        }

         */
        /*
        double tow = 383400;
        Options options = new Options();
        SatellitePositionGNSS satellitePosition = new SatellitePositionGNSS(satelliteEphemeris.get(0), tow, approxUserCoord, options);
        satellitePosition.computeSatellitePosition(tow);
        Log.i("igor Satelliteposss", satellitePosition.getSatCoordinates().toString());
        // If results null, something wrong happened and we are discarding the previous measurements

         */
        if(userCoord == null)
        {
            gnssPositioning = new GNSSPositioning();
        }
        // Otherwise, we update our time variable
        else
        {
            if(gnssPositioning.getGnssObservationAllSats().isEmpty())
            {
                return;
            }

            // Refresh epoch timestamp
            String firstKey = gnssPositioning.getGnssObservationAllSats().keySet().stream().findFirst().get();
            timeOfDay = gnssPositioning.getGnssObservationAllSats().get(firstKey).getTod();
            timeOfWeek = gnssPositioning.getGnssObservationAllSats().get(firstKey).getTow();
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Get the satellites positions from the RTCM streams
     */
    private void getSatPositionFromStreams()
    {
        HashMap<String, GNSSEphemeris> eph = new HashMap<>();
        GNSSEphemerisCorrections ephCorr = null;
        SatelliteCodeBiases satelliteCodeBiases = null;

        HashMap< String, GNSSEphemeris> updatedEph = new HashMap<>();

        // Looking the streams contents...
        try
        {
            for(StreamEphemerisHandler stream : this.streams.listSEH)
            {
                if(!stream.getCurrentEphemeris().isEmpty())
                {
                    eph.putAll(stream.getCurrentEphemeris());
                }

                if(!stream.getEphemerisCorrections().getCorrections().isEmpty())
                {
                    ephCorr = stream.getEphemerisCorrections();
                }

                if(!stream.getSatelliteCodeBiases().getCb().isEmpty())
                {
                    satelliteCodeBiases = stream.getSatelliteCodeBiases();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // We retrieve the last corrections

            SatelliteCodeBiases.CodeBias codeBias;
            PreciseCorrection preciseCorr;

            // We merge put the corrections in the ephemeris data
            for(HashMap.Entry<String, GNSSEphemeris> entry : eph.entrySet())
            {
                GNSSEphemeris currentEph = (GNSSEphemeris) entry.getValue().copy();

                if(ephCorr != null)
                {
                    preciseCorr = ephCorr.getSatCorrectionById(entry.getValue().getGnssSystem(), entry.getValue().getPrn());
                    if(preciseCorr != null)
                    {
                        currentEph.setEphCorrections(preciseCorr);
                    }
                    else
                    {
                        //Log.i("CORR", "Missing correction for satellite " + entry.getKey());
                    }
                }

                if(satelliteCodeBiases != null)
                {
                    codeBias = satelliteCodeBiases.getSatBiasById(entry.getValue().getGnssSystem(), entry.getValue().getPrn());
                    if(codeBias != null)
                    {
                        currentEph.setCodeBias(codeBias);
                    }
                    else
                    {
                        //Log.i("CORR", "Missing DCB for satellite " + entry.getKey());
                    }
                }

                updatedEph.put(entry.getKey(), (GNSSEphemeris) currentEph.copy());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        satelliteEphemeris = updatedEph;
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Get the satellite positions from the navigation message. Only work for GPS satellites.
     */
    private void getSatPositionFromNavigationMessage()
    {
        double ttx = 0;

        if(satelliteEphemeris == null)
        {
            satelliteEphemeris = new HashMap<>();
        }

        //getting GPS satellite positions from the navigation message
        if(!mMapMessages.isEmpty()){
            //we are going to use only navigation messages from GPS, because
            //we have only decoded this type of message
            //get the message of satellites of type L1
            if(mMapMessages.containsKey(GnssNavigationMessage.TYPE_GPS_L1CA)){
                //we recuperate satellite id, subFrameId and navigation message from GPS in another map
                Map<Integer, Map<Integer, GnssNavigationMessage>> mMapMessagesGPS =
                        new HashMap<>(mMapMessages.get(GnssNavigationMessage.TYPE_GPS_L1CA));

                //we iterate this new map by satellite id
                for(Map.Entry<Integer,Map<Integer,GnssNavigationMessage>> entry : mMapMessagesGPS.entrySet()){
                    //if we have already all subframes to reconstruct data
                    if(entry.getValue().containsKey(1) && entry.getValue().containsKey(2) && entry.getValue().containsKey(3)){
                        //we save subframes needed for reconstructing in local variables
                        GnssNavigationMessage subframe1 = entry.getValue().get(1);
                        GnssNavigationMessage subframe2 = entry.getValue().get(2);
                        GnssNavigationMessage subframe3 = entry.getValue().get(3);
                        //we reconstruct ephemeris add we save then in our vector
                        EphemerisGPS ephemerisGPSL1 = new EphemerisGPS(subframe1,subframe2,subframe3);

                        //we initialize object to calculate satellite position ans we add it to our SatellitePositionGPS vector
                        satelliteEphemeris.put(Utils.getFormattedSatIndex(1, entry.getKey()), ephemerisGPSL1.toEphemerisGNSS());
                    }
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * setting Callbacks to receive satellite measurements and navigation messages
     */
    private void settingCallbacks(){
        //initializing hasmap to save navigation messages from all satellites
        mMapMessages = new HashMap<>();

        //callback to receive data from gnss clock and satellite clock
        mGnssMeasurementsEventCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                super.onGnssMeasurementsReceived(eventArgs);
                //this object contains clock data from the gnss chipset and satellite
                mGnssMeasurementsEvent = eventArgs;
                connectToMountpoints();
                gnssMeasurementsHandler();
                writeToFile();
                /*
                if(imuData.GetImuObservation() != null){
                    imuData.IMUdataFusion();
                }*/

                refreshingFragment(false);
            }
        };

        //callback to receive navigation messages from satellite
        mGnssNavigationMessageCallback = new GnssNavigationMessage.Callback() {
            @Override
            public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                super.onGnssNavigationMessageReceived(event);
                //Log.i(TAG, "navigation message type : "+event.toString());

                //adding navigation messages to our map for reconstructing data
                //map structure : Map<Type(GPS_L1) ,Map<svid(5) ,Map<subframe(2), GnssNavigationMessage(data)>>>

                //if map is empty
                if(!mMapMessages.isEmpty()){
                    //if we already have a message of the same type
                    if(mMapMessages.containsKey(event.getType())){
                        //if we already have a message from the same satellite
                        if(mMapMessages.get(event.getType()).containsKey(event.getSvid())){
                            //if we already have a message with the same id
                            Log.i("sateliteaqui",Integer.toString(event.getSvid()));
                            if(mMapMessages.get(event.getType()).get(event.getSvid()).containsKey(event.getSubmessageId())){
                                //we replace by new message
                                mMapMessages.get(event.getType()).get(event.getSvid()).replace(event.getSubmessageId(),event);
                            }else{
                                //else we add subframe id and new message
                                mMapMessages.get(event.getType()).get(event.getSvid()).put(event.getSubmessageId(),event);
                            }
                        //else we add satellite id, subframe id and message
                        }else{
                            mMapMessages.get(event.getType()).put(event.getSvid(), new HashMap<Integer, GnssNavigationMessage>());
                            mMapMessages.get(event.getType()).get(event.getSvid()).put(event.getSubmessageId(), event);
                        }
                    //else we add type, satellite id, subframe id and message
                    }else{
                        mMapMessages.put(event.getType(), new HashMap<Integer, Map<Integer, GnssNavigationMessage>>());
                        mMapMessages.get(event.getType()).put(event.getSvid(), new HashMap<Integer, GnssNavigationMessage>());
                        mMapMessages.get(event.getType()).get(event.getSvid()).put(event.getSubmessageId(),event);
                    }
                //else we add type, satellite id, subframe id and message
                }else{
                    mMapMessages.put(event.getType(), new HashMap<Integer, Map<Integer, GnssNavigationMessage>>());
                    mMapMessages.get(event.getType()).put(event.getSvid(), new HashMap<Integer, GnssNavigationMessage>());
                    mMapMessages.get(event.getType()).get(event.getSvid()).put(event.getSubmessageId(),event);
                }
            }

        };

        // Callback for phone computed position, retrieved for approx coordinates
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        // Listener for NMEA message
        nmeaMessageListener = new OnNmeaMessageListener() {
            @Override
            public void onNmeaMessage(String message, long timestamp) {
                //Log.i("NMEA", message);
            }
        };
    }

    //----------------------------------------------------------------------------------------------

    /**
     * checking location permission status
     * @param permissions
     * @return
     */
    private boolean checkingPermissions(String[] permissions){

        for(String permission : permissions){
            if(ActivityCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Requesting permission
     * @param permissions Permissions requested
     */
    private void requestPermission(String[] permissions){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //we explain to user why we need this permission
                Toast.makeText(this, "Application can not work without this permission", Toast.LENGTH_LONG).show();
            }

        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)){
                //we explain to user why we need this permission
                Toast.makeText(this, "Application can not load maps without internet", Toast.LENGTH_LONG).show();
            }

        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                //we explain to user why we need this permission
                Toast.makeText(this, "Application can not write log files without this permission", Toast.LENGTH_LONG).show();
            }
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
        {
            Toast.makeText(this, "Application can not work without this permission", Toast.LENGTH_LONG).show();
        }
        //ask for permission
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_CODE);
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Checking request permission result
     * @param requestCode Code for request
     * @param permissions Permissions requested
     * @param grantResults Results of previous request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //checking the request code of our request
        if (requestCode == PERMISSIONS_CODE) {
            //if permission granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //displaying that permission has been granted
                //and then we can access location
                Log.i(TAG, "Location access allow");
                try {
                mLocationManager.registerGnssMeasurementsCallback(mGnssMeasurementsEventCallback);
                }catch (SecurityException e){
                    e.printStackTrace();
                }
                mLocationManager.registerGnssNavigationMessageCallback(mGnssNavigationMessageCallback);
                Toast.makeText(this, "Permission granted now you can access location", Toast.LENGTH_LONG).show();
            }else{
                //displaying that permission has not been granted
                Toast.makeText(this,"You just denied Location permission",Toast.LENGTH_LONG).show();
            }

            //if permission granted
            if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //displaying that permission has been granted
                Log.i(TAG, "Internet access allow");
                Toast.makeText(this, "Permission granted now you can access location", Toast.LENGTH_LONG).show();
            }else{
                //displaying that permission has not been granted
                Toast.makeText(this,"You just denied Internet permission",Toast.LENGTH_LONG).show();
            }

            //if permission granted
            if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                //displaying that permission has been granted
                Log.i(TAG, "Storage access allow");
                Toast.makeText(this, "Permission granted now you can access storage", Toast.LENGTH_LONG).show();
            }else{
                //displaying that permission has not been granted
                Toast.makeText(this,"You just denied storage access permission",Toast.LENGTH_LONG).show();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Managing buttons for the logger interface
     * @param v Current view
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonStart:

                if(!checkPos.isChecked() && !checkPseudo.isChecked() && !checkEphem.isChecked() && !checkIMU.isChecked()) {
                    Toast.makeText(getApplicationContext(), "You have to check at least one checkbox", Toast.LENGTH_SHORT).show();
                }
                if(checkIMU.isChecked()) {
                        btnStop.setEnabled(true);
                        btnStart.setEnabled(false);
                        imuData.CreateFile(this);
                        imuData.GetIMUcallback();
                    }
                if(timeOfDay != 0 && gnssPositioning.getGnssObservationAllSats() != null && (checkPos.isChecked() || checkPseudo.isChecked() || checkEphem.isChecked())) {
                        File mDir = new File(Environment.getExternalStorageDirectory(), "IFSTTAR_GNSS_IMU_logger");

                        fileLogger = new FileLogger(mDir, processingOptions);
                        if(!btnStop.isEnabled())
                        {
                            btnStop.setEnabled(true);
                            btnStart.setEnabled(false);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "There is no GNSS data to log", Toast.LENGTH_SHORT).show();
                    }
                break;

            case R.id.buttonStop:
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                imuData.onDestroy();
                fileLogger = null;

                break;
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Write data into files
     */
    private void writeToFile()
    {
        if(fileLogger != null)
        {
            fileLogger.refreshData(gnssPositioning.getGnssObservationAllSats(), mGnssMeasurementsEvent, satelliteEphemeris, userCoord, timeOfWeek);
            fileLogger.logData(checkPseudo.isChecked(), checkPos.isChecked(), checkEphem.isChecked(),
                    checkEphem.isChecked());
        }
    }

    //----------------------------------------------------------------------------------------------
}
