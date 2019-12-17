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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import org.gogpsproject.positioning.TopocentricCoordinates;
import java.util.HashMap;
import java.util.List;
import fr.ifsttar.geoloc.geoloclib.Coordinates;
import fr.ifsttar.geoloc.geoloclib.satellites.GNSSObservation;
import fr.ifsttar.geoloc.geoloclib.satellites.SatelliteSkyPlot;
import fr.ifsttar.geoloc.geolocpvt.R;

/**
 * the fragment of skyplot of the satellites
 */

public class SatelliteFragment extends Fragment implements SensorEventListener {

    /** Hash Map to organize all the Satellites information by id*/
    private HashMap<Integer, SatelliteSkyPlot> satelliteSkyPlots;
    /** List with all data point (x,y) of all Satellites founded */
    private PointsGraphSeries<DataPoint> satellitePositionOnSkyplot;
    /** Graphic reference. graph calls all the methods from GraphView library*/
    private GraphView graph;
    /** Picture that represents the polar coordinates*/
    private ImageView polarGraphic;
    /** Bundle with data that we get from Main Activity*/
    private Bundle bundle;
    /** skyplot view*/
    private View view;
    /** Variables used to get sensor data*/
    @Nullable
    public SensorManager sensorManager;
    public Sensor mRotationVectorSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    /** Variables used to stock sensor data*/
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    /** Modify rotation sensibility */
    static final float ALPHA = 0.1f;
    /** Rotation Value used to rotate the skyplot picture and satellites data points*/
    static public float orientationPortable;
    /** Manage pop-up (open/close)*/
    private boolean isSatelliteDisplayed = false;

    /**
     * Define some variables such as the graphic and the sensor management.
     * Closes the pop-up.
     * @param inflater
     * @param container
     * @param savedInstanceState Get the Bundle from main
     * @return the current view of the Skyplot
     */
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.fragment_satellite, null);
        this.view = view;
        this.polarGraphic = view.findViewById(R.id.polargraphic);
        this.graph =  view.findViewById(R.id.graph);
        this.sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        this.mRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Closes the pop-up
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isSatelliteDisplayed) {
                    stopDisplaySatelliteInfo();
                }
                return false;
            }
        });

        return view;
    }
    /**
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshData();
    }
    /**
     * Call all methods to create the skyplot (graphic and picture).
     */
    public void refreshData() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshSatelliteData();
                setGraphicDefault();
                plotSkyplot();
                if (satellitePositionOnSkyplot != null) {
                    List<Series> list = graph.getSeries();
                    for (Series s : list) {
                        s.setOnDataPointTapListener(new OnDataPointTapListener() {
                            @Override
                            public void onTap(Series series, DataPointInterface dataPoint) {
                                // Toast.makeText(SatelliteFragment.this.getActivity(), "The satellite position is : " + dataPoint, Toast.LENGTH_SHORT).show();
                                try {
                                    SatelliteSkyPlot satelliteSkyPlot = findSatelliteByDataPoint(dataPoint);
                                    displaySatelliteInfo(satelliteSkyPlot, dataPoint);
                                } catch (NullPointerException e) {
                                    Log.e("satelliteFragment touch","Satellite not found on touch");
                                }
                                ;
                            }
                        });
                    }
                }

            }
        });
    }
    /**
     * Define graphic's scale, range of the axis and label design. If necessary, plot a circle as a bord.
     */
    public void setGraphicDefault() {
        this.graph.getViewport().setYAxisBoundsManual(true);
        this.graph.getViewport().setXAxisBoundsManual(true);
        this.graph.getViewport().setMinX(-100);
        this.graph.getViewport().setMaxX(100);
        this.graph.getViewport().setMinY(-100);
        this.graph.getViewport().setMaxY(100);
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
        this.satellitePositionOnSkyplot = new PointsGraphSeries<>(); // <DataPoints>
       // graph.setScaleX(graph.getScaleX() - 0.02f);
        graph.setScaleY(graph.getScaleX() / 1.3f);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);

    }
    /**
     * Receive Bundle from main activity to get satellite information.
     */
    public void refreshSatelliteData() {

        this.satelliteSkyPlots = new HashMap<Integer, SatelliteSkyPlot>();

        this.bundle = getArguments();

        if (getArguments() != null) {
        // There is two ways to get the satellite information. It can be from GnssObservations (all cases) or TrackedObservations (only tracked)
           // but GnssObservations gets only used and TrackedObservations doesn't work as we want.
            // we couldn't find information about visible satellite. We tried using the variable GnssMeasurementsEvent, but it doesn't have the information we need.
            if (bundle.getSerializable("GnssObservations") != null) {

                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("GnssObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {

                    GNSSObservation current = entry.getValue();

                    GNSSObservationToSatelliteSkyplot(current);

                }
            }


            if (bundle.getSerializable("TrackedObservations") != null) {

                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("TrackedObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {
                    GNSSObservation current = entry.getValue();
                    GNSSObservationToSatelliteSkyplot(current);
                    try {
                        double a = current.getSatellitePosition().getSatElevation();
                    } catch (NullPointerException e) {
                        Log.e ("satelliteFragment","satellite tracked not founded");
                    }
                }
            }
            if (bundle.getParcelable("GnssMeasurementsEvent") != null) {
                // if we want information about visible satellites.
            }

        }
    }
    /**
     * Find a satellite by the point that represents it. This method is important to find the satellite and open the pop-up.
     * @param dataPoint Data point x,y where the point is placed in the graphic
     * @return the satellite associated to the data point
     */
    public SatelliteSkyPlot findSatelliteByDataPoint(DataPointInterface dataPoint) {
        float precision = 1.0f;
        SatelliteSkyPlot satelliteSkyPlot = null;
        for (HashMap.Entry<Integer, SatelliteSkyPlot> satelliteSkyPlotsObs : this.satelliteSkyPlots.entrySet()) {
            SatelliteSkyPlot current = satelliteSkyPlotsObs.getValue();
            if (Math.abs(current.getDataPoint(SatelliteFragment.orientationPortable).getX() - dataPoint.getX()) < precision && Math.abs(current.getDataPoint(SatelliteFragment.orientationPortable).getY() - dataPoint.getY()) < precision) {
                satelliteSkyPlot = current;
                break;
            }
        }
        return satelliteSkyPlot;
    }
    /**
     * Plot the graphic using satelliteSkyPlots with all data point founded.
     */
    public void plotSkyplot() {
        if (!this.satelliteSkyPlots.isEmpty()) {
            graph.removeAllSeries();
            for (HashMap.Entry<Integer, SatelliteSkyPlot> satelliteSkyPlotsObs : this.satelliteSkyPlots.entrySet()) {
                SatelliteSkyPlot current = satelliteSkyPlotsObs.getValue();
                graph.addSeries(new PointsGraphSeries<DataPoint>(new DataPoint[]{current.getDataPoint(SatelliteFragment.orientationPortable)}));
            }

        }
    }
    /**
     * Get information from GNSSObservation class such as x,y,z coordinates from satellites. Then, we calculate azimuth/elevation using user coordinates (topocentricCoordinates class)
     * @param observation
     */
    public void GNSSObservationToSatelliteSkyplot(GNSSObservation observation) {
        if (this.bundle.getSerializable("ComputedPosition") != null) {


            try {
                Coordinates userCord = (Coordinates) bundle.getSerializable("ComputedPosition");
                // Converting coordinates to coordinates of gogpsproject
                org.gogpsproject.positioning.Coordinates userCordtransformed = org.gogpsproject.positioning.Coordinates.globalXYZInstance(userCord.getX(), userCord.getY(), userCord.getZ());
                org.gogpsproject.positioning.Coordinates satelliteTransformed = org.gogpsproject.positioning.Coordinates.globalXYZInstance(observation.getSatellitePosition().getSatCoordinates().getX(), observation.getSatellitePosition().getSatCoordinates().getY(), observation.getSatellitePosition().getSatCoordinates().getZ());
                TopocentricCoordinates topocentricCoordinates = new TopocentricCoordinates();
                topocentricCoordinates.computeTopocentric(userCordtransformed, satelliteTransformed);
                SatelliteSkyPlot s = new SatelliteSkyPlot(observation.getId(), topocentricCoordinates.getAzimuth(), topocentricCoordinates.getElevation(), observation.getConstellation(), SatelliteFragment.orientationPortable);

                if (!this.satelliteSkyPlots.containsKey(observation.getId())) {
                    this.satelliteSkyPlots.put(observation.getId(), s);
                }

            } catch (NullPointerException e) {
                Log.e("SkyplotMissingData", "Missing Data to get satellite information");
            }

        }

    }
    /**
     * Open the pop-up after user touch a point in the graphic.
     * @param satelliteSkyPlot satellite related to data point
     * @param dataPoint point touched by the user.
     */
    public void displaySatelliteInfo(SatelliteSkyPlot satelliteSkyPlot, DataPointInterface dataPoint) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        PopupWindow pw = new PopupWindow(inflater.inflate(R.layout.fragment_satellite_popupinfo, null, false), 800, 500, true);

        pw.showAtLocation(view, Gravity.NO_GRAVITY, 100, 1100);

        TextView satelliteID = pw.getContentView().findViewById(R.id.SatelliteID);
        TextView satelliteAzimuth = pw.getContentView().findViewById(R.id.SatelliteAzimuth);
        TextView satelliteElevation = pw.getContentView().findViewById(R.id.SatelliteElevation);
        TextView satelliteConstelation = pw.getContentView().findViewById(R.id.SatelliteConstellation);

        satelliteID.setText("Id: " + satelliteSkyPlot.getId());
        satelliteAzimuth.setText("Azimuth:" +  ((Double)satelliteSkyPlot.getAzimuth()).toString().substring(0,6)); //ToString("#0.000").Substring(0, 5);
        satelliteElevation.setText("Elevation: " + ((Double)satelliteSkyPlot.getElevation()).toString().substring(0,6));
        satelliteConstelation.setText("Constelation: " + satelliteSkyPlot.getConstellation());
    }
    /**
     * Close the current pop-up
     */
    public void stopDisplaySatelliteInfo() {
        TextView satelliteID = getView().findViewById(R.id.SatelliteID);
        TextView satelliteAzimuth = getView().findViewById(R.id.SatelliteAzimuth);
        TextView satelliteElevation = getView().findViewById(R.id.SatelliteElevation);
        TextView satelliteConstellation = getView().findViewById(R.id.SatelliteConstellation);

        satelliteID.setVisibility(View.GONE);
        satelliteAzimuth.setVisibility(View.GONE);
        satelliteElevation.setVisibility(View.GONE);
        satelliteConstellation.setVisibility(View.GONE);

        isSatelliteDisplayed = false;
    }

    /**
     * Register sensor data
     */
    public void onResume() {
        super.onResume();
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Stop registering sensor data
     */
    public void onPause() {
        super.onPause();
        this.sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * callback to get data when sensor value has changed.
     * @param event sensor event
     */
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == this.accelerometer) {
            System.arraycopy(lowPass(event.values.clone(), mLastAccelerometer), 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == this.magnetometer) {
            System.arraycopy(lowPass(event.values.clone(), mLastMagnetometer), 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            SatelliteFragment.orientationPortable = mOrientation[0];
            polarGraphic.setRotation(-(float) Math.toDegrees((double) orientationPortable));
            // mOrientation[2] = Roll, angle of rotation about the y axis. This value represents the angle between a plane perpendicular to the device's screen and a plane perpendicular to the ground. Assuming that the bottom edge of the device faces the user and that the screen is face-up, tilting the left edge of the device toward the ground creates a positive roll angle. The range of values is -π/2 to π/2.
        }
    }

    /**
     * Filter sensor data, so the rotation of the skyplot is less sensitivity. (Use Alpha value)
     * @param input data without filter
     * @param output data with filter
     * @return data with filter
     */
    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
}





