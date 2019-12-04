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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.RotateDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import org.gogpsproject.ephemeris.GNSSEphemeris;
import org.gogpsproject.positioning.TopocentricCoordinates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import fr.ifsttar.geoloc.geoloclib.Coordinates;
import fr.ifsttar.geoloc.geoloclib.Utils;
import fr.ifsttar.geoloc.geoloclib.satellites.GNSSObservation;
import fr.ifsttar.geoloc.geolocpvt.R;

import static android.content.Context.SENSOR_SERVICE;

//import org.gogpsproject.positioning.Coordinates;
/**
 * the fragment of skyplot of the satellites
 */
public class SatelliteFragment extends Fragment implements SensorEventListener{

    //From Main activity:
    private HashMap<String, GNSSEphemeris> satelliteEphemeris;

    private PointsGraphSeries<DataPoint> satellitePositionOnSyplot ;
    private GraphView graph;
    private HashMap < Integer, SatelliteSkyPlot> satelliteSkyPlots;

    private Bundle bundle;

   // private ArrayList <SatelliteSkyPlot> satelliteSkyPlots; // faire un hashmap
    //defining the xml for the fragment
    @Nullable

    public SensorManager sensorManager;
    public Sensor mRotationVectorSensor;
    private float rotationYvalue;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_satellite, null);

        this.graph = (GraphView) view.findViewById(R.id.graph);

        defaultGraphic();

        sensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
        mRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

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
                //changeGraphic();
                 refreshSatellitesInformation();
                // refreshPointsToPlot();
                  plotSkyplot ();

                // If the user wants to know about one satellite, they click and it displays some information:
                if (satellitePositionOnSyplot != null)
                {
                    satellitePositionOnSyplot.setOnDataPointTapListener(new OnDataPointTapListener() {
                        @Override
                        //bug: If we try to tap while the graph is changing with new data, we dont recive the message, so we must stop the graph for few seconds
                        public void onTap(Series series, DataPointInterface dataPoint) {
                            Toast.makeText(SatelliteFragment.this.getActivity(), "The satellite position is : "+dataPoint, Toast.LENGTH_SHORT).show();
                        }
                    });

                }

            }
        });
    }
    
    /**
     * Initialize the graphic: one circle as the border and we change the range of the axis.
     * We may change the name of the axis and put some legends and design
     */
    public void defaultGraphic () {


        // Scale it to 50 x 50
     //   Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,50,100,false));
/*
        RotateDrawable rotate =  new RotateDrawable();
        rotate.createFromPath("C:\\Users\\igorl\\AndroidStudioProjects\\geolocpvt-master\\app\\src\\main\\res\\drawable\\skyplot.png");

*/


        //this.graph.setBackground();
        this.graph.getViewport().setYAxisBoundsManual(true);
        this.graph.getViewport().setXAxisBoundsManual(true);

        this.graph.getViewport().setMinX(-100);
        this.graph.getViewport().setMaxX(100);
        this.graph.getViewport().setMinY(-100);
        this.graph.getViewport().setMaxY(100);

        //  this.graph.getViewport().s

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

        this.satellitePositionOnSyplot = new PointsGraphSeries<DataPoint> ();
    }

    public void changeGraphic ()
    {
        Drawable graph_background = ContextCompat.getDrawable(getContext(),R.drawable.skyplot);
        Bitmap bitmap = ((BitmapDrawable) graph_background).getBitmap();

        Drawable d = new BitmapDrawable(getResources(), bitmap);

        Matrix matrix = new Matrix();
        matrix.postRotate(this.rotationYvalue);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        Drawable dNew = new BitmapDrawable(getResources(), rotated);

        this.graph.setBackground(dNew);
    }

    public void refreshSatellitesInformation () {

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

                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("TrackedObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {

                    GNSSObservation current = entry.getValue();
                    GNSSObservationToSatelliteSkyplot (current);
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



    public void plotSkyplot ()
    {

        if (!this.satelliteSkyPlots.isEmpty())
        {

            for (HashMap.Entry<Integer, SatelliteSkyPlot> satelliteSkyPlotsObs : this.satelliteSkyPlots.entrySet())
            {
                SatelliteSkyPlot current = satelliteSkyPlotsObs.getValue();
                this.satellitePositionOnSyplot.appendData(current.getDataPoint(),false,50);
            }
            satellitePositionOnSyplot.setShape(PointsGraphSeries.Shape.POINT);
            graph.addSeries(satellitePositionOnSyplot);
        }
        else // to avoid IllegalArgumentException
        {
            satellitePositionOnSyplot.resetData(new DataPoint[] {});
            graph.addSeries(satellitePositionOnSyplot);
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

    public void onResume() {
        super.onResume();
        this.sensorManager.registerListener(this, this.mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onPause() {
        super.onPause();
        this.sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        float xValue = event.values[0];
        this.rotationYvalue = event.values[1];
        float zValue = event.values[2];
        Log.i("sensor", "x:"+xValue +";y:"+this.rotationYvalue+";z:"+zValue);
    }
}

class SatelliteSkyPlot {
    private int id;
    private double azimuth, elevation, constelation;
    private DataPoint dataPoint;

    public SatelliteSkyPlot(int id, double azimuth, double elevation, double constelation) {
        this.id = id;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.constelation = constelation;
        // Azimuth is the angle and Elevation is the radius. Polar -> Cartesian : x = r.cos = Elevation. cos(Azimuth)// y = r.sen = Elevation.sin(Azimuth)
        this.dataPoint = new DataPoint(elevation*Math.cos(Math.toRadians(azimuth)), elevation*Math.sin(Math.toRadians(azimuth)));
    }

    public DataPoint getDataPoint() {
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


