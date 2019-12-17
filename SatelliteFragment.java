///=================================================================================================
// Class MainActivity
//      Author :  Jose Gilberto RESENDIZ FONSECA
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
package fr.ifsttar.geoloc.geolocpvt.fragments;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import org.gogpsproject.positioning.TopocentricCoordinates;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import fr.ifsttar.geoloc.geoloclib.Coordinates;
import fr.ifsttar.geoloc.geoloclib.satellites.GNSSObservation;
import fr.ifsttar.geoloc.geolocpvt.R;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * the fragment of skyplot of the satellites
 */
public class SatelliteFragment extends Fragment implements SensorEventListener{


    private PointsGraphSeries<DataPoint> satellitePositionOnSkyplot ;
    private GraphView graph;
    private HashMap < Integer, SatelliteSkyPlot> satelliteSkyPlots;
    private Bundle bundle;
    @Nullable
    public SensorManager sensorManager;
    public Sensor mRotationVectorSensor;
    private ImageView dubBus;
    private Sensor accelerometer;
    private Sensor magnetometer;
    static public float OritationPortable;
    private View view;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private boolean isSatelliteDispalyed = false;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_satellite, null);

        this.dubBus = view.findViewById(R.id.back);

        this.graph = (GraphView) view.findViewById(R.id.graph);


        this.sensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
        this.mRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        this.view = view;

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //show dialog here
                if (isSatelliteDispalyed)
                {
                    stopDisplaySatelliteInfo();
                }
                return false;
            }
        });
       // onButtonShowPopupWindowClick();
        return view;

    }

    //setting fragment view
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshData();

    }

    public void refreshData(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                 refreshSatellitesData();
                 setGraphicDefault();
                  plotSkyplot ();
                if (satellitePositionOnSkyplot != null)
                {
                      List<Series> list = graph.getSeries();
                      for (Series s : list) {
                          s.setOnDataPointTapListener(new OnDataPointTapListener() {
                              @Override
                              public void onTap(Series series, DataPointInterface dataPoint) {
                                  Toast.makeText(SatelliteFragment.this.getActivity(), "The satellite position is : " + dataPoint, Toast.LENGTH_SHORT).show();
                                  try {
                                      SatelliteSkyPlot satelliteSkyPlot = findSatellitebyDataPoint(dataPoint);
                                      displaySatelliteInfo(satelliteSkyPlot, dataPoint);
                                  }
                                 catch (NullPointerException e){};
                              }
                          });
                      }
                }

            }
        });
    }


    /**
     * Initialize the graphic: one circle as the border and we change the range of the axis.
     * We may change the name of the axis and put some legends and design
     */
    public void setGraphicDefault () {

        this.graph.getViewport().setYAxisBoundsManual(true);
        this.graph.getViewport().setXAxisBoundsManual(true);

        this.graph.getViewport().setMinX(-100);
        this.graph.getViewport().setMaxX(100);
        this.graph.getViewport().setMinY(-100);
        this.graph.getViewport().setMaxY(100);

        //  this.graph.getViewport().s
/*
        //Draw the circle border, we need two fonctions:
        LineGraphSeries<DataPoint> seriesBorder1 = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> seriesBorder2 = new LineGraphSeries<DataPoint>();
        double x = -90, y;
        for (int i = 0; i< 1801; i++)
        {
            y =  Math.sqrt(8100 - Math.pow(x,2));
            seriesBorder1.appendData(new DataPoint(x,y), true, 4000);
            seriesBorder2.appendData(new DataPoint(x,-y),true, 4000);
            x += 0.1;
        }
        this.graph.addSeries(seriesBorder1);
        this.graph.addSeries(seriesBorder2);

 */

        this.satellitePositionOnSkyplot = new PointsGraphSeries<DataPoint> ();
        graph.setScaleX(graph.getScaleX()-0.02f);
        graph.setScaleY(graph.getScaleX()/1.3f);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);

    }


    public void refreshSatellitesData () {

     this.satelliteSkyPlots = new HashMap<Integer, SatelliteSkyPlot>();

        this.bundle = getArguments();

        if (getArguments() != null) {

            if (bundle.getSerializable("GnssObservations") != null) {

                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("GnssObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {

                    GNSSObservation current = entry.getValue();

                    GNSSObservationToSatelliteSkyplot(current);

                }
            }


            if (bundle.getSerializable("TrackedObservations") != null) {

                Log.i("entrei", "sim");
                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("TrackedObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {
                    Log.i("entrei2", "sim");
                    GNSSObservation current = entry.getValue();
                    GNSSObservationToSatelliteSkyplot (current);
                    try {
                        double a = current.getSatellitePosition().getSatElevation();
                        Log.i("entrei3", Double.toString(a));
                    }catch (NullPointerException e) {};
                }
            }


            if (bundle.getParcelable("GnssMeasurementsEvent") != null) {
                //recuperating object in a local variable
                GnssMeasurementsEvent mGnssMeasurementsEvent = bundle.getParcelable("GnssMeasurementsEvent");
                Coordinates mCoordinates = (Coordinates) bundle.getSerializable("ComputedPosition");
                double tod = bundle.getDouble("Time");
                double gdop = bundle.getDouble("GDOP");
                //recuperating gnss clock from measurement
                GnssClock mGnssClock = mGnssMeasurementsEvent.getClock();
                //creating list to save all satellites measurements
                List<GnssMeasurement> mListMeasurements = new ArrayList<GnssMeasurement>();
                //adding all satellite measurements to our list
                mListMeasurements.addAll(mGnssMeasurementsEvent.getMeasurements());
                //we create an iterator to our list
                ListIterator<GnssMeasurement> itListMeasures = mListMeasurements.listIterator();

                //counting satellite number by constellation
                while (itListMeasures.hasNext()) {
                    GnssMeasurement current = itListMeasures.next();
                    //Log.i(">> FREQ", "" + current.getCarrierFrequencyHz());
                    switch (current.getConstellationType()) {
                        case GnssStatus.CONSTELLATION_GPS:
                            Log.i("visible.GPS","sim");
                            break;

                        case GnssStatus.CONSTELLATION_GALILEO:
                            Log.i("visible.GALILEO","sim");

                            break;

                        case GnssStatus.CONSTELLATION_BEIDOU:

                            Log.i("visible.BEIDOU","sim");
                            break;

                    }
                }
            }
        }
    }

    public SatelliteSkyPlot findSatellitebyDataPoint (DataPointInterface dataPoint)
    {
        SatelliteSkyPlot satelliteSkyPlot = null;
        for (HashMap.Entry<Integer, SatelliteSkyPlot> satelliteSkyPlotsObs : this.satelliteSkyPlots.entrySet()) {
            SatelliteSkyPlot current = satelliteSkyPlotsObs.getValue();
            if (Math.abs(current.getDataPoint().getX() - dataPoint.getX())<1 && Math.abs(current.getDataPoint().getY() - dataPoint.getY())<1)
            {
                satelliteSkyPlot = current;
                break;
            }
        }
        return satelliteSkyPlot;
    }
    public void plotSkyplot ()
    {

        if (!this.satelliteSkyPlots.isEmpty())
        {
            graph.removeAllSeries();
            for (HashMap.Entry<Integer, SatelliteSkyPlot> satelliteSkyPlotsObs : this.satelliteSkyPlots.entrySet()){
                SatelliteSkyPlot current = satelliteSkyPlotsObs.getValue();
                graph.addSeries (new PointsGraphSeries<DataPoint> (new DataPoint[]{current.getDataPoint()}));
            }

        }


    }
    public void GNSSObservationToSatelliteSkyplot (GNSSObservation observation)
    {
        if (this.bundle.getSerializable("ComputedPosition") != null) {


            try {
                Coordinates userCord = (Coordinates) bundle.getSerializable("ComputedPosition");
                // Converting coordinates to coordinates of gogpsproject
                org.gogpsproject.positioning.Coordinates userCordtransformed = org.gogpsproject.positioning.Coordinates.globalXYZInstance(userCord.getX(), userCord.getY(), userCord.getZ());
                org.gogpsproject.positioning.Coordinates satelliteTransformed = org.gogpsproject.positioning.Coordinates.globalXYZInstance(observation.getSatellitePosition().getSatCoordinates().getX(), observation.getSatellitePosition().getSatCoordinates().getY(), observation.getSatellitePosition().getSatCoordinates().getZ());
                TopocentricCoordinates topocentricCoordinates = new TopocentricCoordinates();
                topocentricCoordinates.computeTopocentric(userCordtransformed, satelliteTransformed);


                SatelliteSkyPlot s = new SatelliteSkyPlot(observation.getId(),topocentricCoordinates.getAzimuth(),topocentricCoordinates.getElevation(),observation.getConstellation());

                if (!this.satelliteSkyPlots.containsKey(observation.getId()))
                {
                    this.satelliteSkyPlots.put(observation.getId(),s);
                }

                Log.i("allinfo", s.toString());


            } catch (NullPointerException e)
            {
                Log.e("SkyplotMissingData","Missing Data to get satellite information");
            }

        }

    }
    /*
    public void displaySatelliteInfo (SatelliteSkyPlot satelliteSkyPlot, DataPointInterface dataPoint)
    {
        try {
            satelliteSkyPlot = findSatellitebyDataPoint(dataPoint);
            TextView satelliteID = getView().findViewById(R.id.SatelliteID);
            TextView satelliteAzimuth = getView().findViewById(R.id.SatelliteAzimuth);
            TextView satelliteElevation = getView().findViewById(R.id.SatelliteElevattion);
            TextView satelliteConstelation = getView().findViewById(R.id.SatelliteConstelation);

            satelliteID.setVisibility(View.VISIBLE);
            satelliteAzimuth.setVisibility(View.VISIBLE);
            satelliteElevation.setVisibility(View.VISIBLE);
            satelliteConstelation.setVisibility(View.VISIBLE);

            satelliteID.setText( "Id: " + satelliteSkyPlot.getId());
            satelliteAzimuth.setText("Azimuth:" + ((Double)satelliteSkyPlot.getAzimuth()).toString().substring(0,4)); //ToString("#0.000").Substring(0, 5);
            satelliteElevation.setText("Elevation: "+ satelliteSkyPlot.getElevation() );
            satelliteConstelation.setText( "Constelation: " + satelliteSkyPlot.getConstelation());

            isSatelliteDispalyed = true;

        }
        catch (NullPointerException e){};
    }

     */
    public void stopDisplaySatelliteInfo ()
    {
        TextView satelliteID = getView().findViewById(R.id.SatelliteID);
        TextView satelliteAzimuth = getView().findViewById(R.id.SatelliteAzimuth);
        TextView satelliteElevation = getView().findViewById(R.id.SatelliteElevattion);
        TextView satelliteConstelation = getView().findViewById(R.id.SatelliteConstelation);

        satelliteID.setVisibility(View.GONE);
        satelliteAzimuth.setVisibility(View.GONE);
        satelliteElevation.setVisibility(View.GONE);
        satelliteConstelation.setVisibility(View.GONE);

        isSatelliteDispalyed =  false;
    }
    public void onResume() {
        super.onResume();
       // this.sensorManager.registerListener(this, this.mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        sensorManager.registerListener(this,accelerometer , SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }
    public void onPause() {
        super.onPause();
        this.sensorManager.unregisterListener(this);
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == this.accelerometer) {

           // System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            System.arraycopy(lowPass(event.values.clone(), mLastAccelerometer), 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == this.magnetometer) {
           // System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            System.arraycopy(lowPass(event.values.clone(), mLastMagnetometer), 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            /*
            Log.i("OrientationTestActivity", String.format("Orientation: %f, %f, %f",
                    mOrientation[0], mOrientation[1], mOrientation[2]));

             */
            SatelliteFragment.OritationPortable = mOrientation[0];
            dubBus.setRotation(-(float)Math.toDegrees((double)OritationPortable));
            // mOrientation[2] = Roll, angle of rotation about the y axis. This value represents the angle between a plane perpendicular to the device's screen and a plane perpendicular to the ground. Assuming that the bottom edge of the device faces the user and that the screen is face-up, tilting the left edge of the device toward the ground creates a positive roll angle. The range of values is -π/2 to π/2.
        }
    }
    public void displaySatelliteInfo (SatelliteSkyPlot satelliteSkyPlot, DataPointInterface dataPoint) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        PopupWindow pw = new PopupWindow(inflater.inflate(R.layout.fragment_satellite_popupinfo, null, false),800,500, true);

        pw.showAtLocation(view, Gravity.NO_GRAVITY, 100, 1100);

        TextView satelliteID = pw.getContentView().findViewById(R.id.SatelliteID);
        TextView satelliteAzimuth = pw.getContentView().findViewById(R.id.SatelliteAzimuth);
        TextView satelliteElevation =  pw.getContentView().findViewById(R.id.SatelliteElevattion);
        TextView satelliteConstelation = pw.getContentView().findViewById(R.id.SatelliteConstelation);

        satelliteID.setText( "Id: " + satelliteSkyPlot.getId());
        satelliteAzimuth.setText("Azimuth:" + satelliteSkyPlot.getAzimuth()); //ToString("#0.000").Substring(0, 5);
        satelliteElevation.setText("Elevation: "+ satelliteSkyPlot.getElevation() );
        satelliteConstelation.setText( "Constelation: " + satelliteSkyPlot.getConstelation());

    }

    static final float ALPHA = 0.1f;
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
}


class SatelliteSkyPlot {
    private int id;
    private double azimuth, elevation, constelation;
    private DataPoint dataPoint;
    private double orientationPortable;

    public void setDataPointByOrientationPortable() {
        this.orientationPortable =  SatelliteFragment.OritationPortable;
        this.dataPoint = new DataPoint(elevation*Math.cos(Math.toRadians(azimuth)-orientationPortable), elevation*Math.sin(Math.toRadians(azimuth)-orientationPortable));
    }

    public SatelliteSkyPlot(int id, double azimuth, double elevation, double constelation) {
        this.id = id;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.constelation = constelation;
        this.orientationPortable = SatelliteFragment.OritationPortable;
        // Azimuth is the angle and Elevation is the radius. Polar -> Cartesian : x = r.cos = Elevation. cos(Azimuth)// y = r.sen = Elevation.sin(Azimuth)
        this.dataPoint = new DataPoint(elevation*Math.cos(Math.toRadians(azimuth)-orientationPortable), elevation*Math.sin(Math.toRadians(azimuth)-orientationPortable));
    }

    public DataPoint getDataPoint() {
        setDataPointByOrientationPortable();
        return this.dataPoint;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public double getConstelation() {
        return constelation;
    }

    public void setConstelation(double constelation) {
        this.constelation = constelation;
    }
    public String toString ()
    {
        return "satellite id: " + this.id + " azimuth: " + this.azimuth + " elevation: " + this.elevation + " Constelation: " + this.constelation + "In skyplot, x = " + this.getDataPoint().getX() + " y = " + this.getDataPoint().getY();
    }
}


