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

import android.graphics.drawable.Drawable;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
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
import java.util.Map;
import java.util.Objects;

import fr.ifsttar.geoloc.geoloclib.Coordinates;
import fr.ifsttar.geoloc.geoloclib.Utils;
import fr.ifsttar.geoloc.geoloclib.satellites.GNSSObservation;
import fr.ifsttar.geoloc.geolocpvt.R;

//import org.gogpsproject.positioning.Coordinates;
/**
 * the fragment of skyplot of the satellites
 */
public class SatelliteFragment extends Fragment {

    //From Main activity:
    private HashMap<String, GNSSEphemeris> satelliteEphemeris;

    private PointsGraphSeries<DataPoint> satellitePositionOnSyplot ;
    private GraphView graph;
    private HashMap < Integer, SatelliteSkyPlot> satelliteSkyPlots;
   // private ArrayList <SatelliteSkyPlot> satelliteSkyPlots; // faire un hashmap
    //defining the xml for the fragment
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_satellite, null);

        this.graph = (GraphView) view.findViewById(R.id.graph);

        defaultGraphic();

   //    refreshData();

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
     * refresh the data points to plot in the graph through the data we get about satellite position
     * x is the elevation
     * y is the azimuth
     */
    public void refreshPointsToPlot()
    {
        graph.removeSeries(this.satellitePositionOnSyplot); // we refresh the graph if we have new points, otherwise we keep it ( TODO)

        try {
            // Make a URL to the web page                                       Sat ID/ Observer Position /     Personal key used to access data
           URL url = new URL("https://www.n2yo.com/rest/v1/satellite/positions/44231/41.702/-76.014/0/1/&apiKey=39HSPE-NK2D2G-QT3PTS-48IH");
            // Get the input stream through URL Connection
            //URLConnection con = url.openConnection();
            Downloader d = new Downloader();
            d.execute("https://www.n2yo.com/rest/v1/satellite/positions/44231/41.702/-76.014/0/1/&apiKey=39HSPE-NK2D2G-QT3PTS-48IH");
            try
            {
                String s = (String)d.get();
                String aS, bS;
                aS = s.substring(s.indexOf("a:") + 2,s.indexOf("a:")+ 7);
                bS = s.substring(s.indexOf("b:") + 2,s.indexOf("b:") + 7);
                Log.i("heree sat",bS);
              //  this.azimuth = Double.parseDouble(aS);
               //  this.elevation = Double.parseDouble(bS);
               // Log.i("heree sat",a);


            }catch (Exception e)
            {
                Log.i("error", "erroreer");
            }
/*
            this.satellitePositionOnSyplot = new PointsGraphSeries<>(new DataPoint[]{
                    new DataPoint(-10, 20),
                    new DataPoint(this.azimuth, this.elevation)

 */
        }
        catch(IOException e) {
            e.printStackTrace();
            this.satellitePositionOnSyplot = new PointsGraphSeries<>(new DataPoint[]{
                    new DataPoint(-10, 20),
                    new DataPoint(50, 50),
            });
        }
        this.satellitePositionOnSyplot = new PointsGraphSeries<>(new DataPoint[]{
                new DataPoint(-10, 20),
             //   new DataPoint(this.azimuth, 0),
        });
        graph.addSeries(this.satellitePositionOnSyplot);

    }

    /**
     * Initialize the graphic: one circle as the border and we change the range of the axis.
     * We may change the name of the axis and put some legends and design
     */
    public void defaultGraphic () {

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

    public void refreshSatellitesInformation () {

        this.satelliteSkyPlots = new HashMap<Integer, SatelliteSkyPlot>();

        Bundle bundle;
        bundle = getArguments();
        if (getArguments() != null) {

            if (bundle.getSerializable("GnssObservations") != null) {

                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("GnssObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {

                    GNSSObservation current = entry.getValue();

                    if (bundle.getSerializable("ComputedPosition") != null) {
                        //Coordinates userCord = (Coordinates) bundle.getSerializable("ComputedPosition");
                        /*
                        if (bundle.getSerializable("ComputedPosition") != null) {

                            try {
                                Coordinates userCord = (Coordinates) bundle.getSerializable("ComputedPosition");
                                TopocentricCoordinates teste = new TopocentricCoordinates();
                                org.gogpsproject.positioning.Coordinates userCord_transformed = new org.gogpsproject.positioning.Coordinates();
                                userCord_transformed.setENU(userCord.getE(), userCord.getN(), userCord.getU());
                                org.gogpsproject.positioning.Coordinates SatCoord_transformed = new org.gogpsproject.positioning.Coordinates();
                                try {
                                    current.getSatellitePosition().getSatCoordinates().setENUvelocity();
                                    userCord.setENUvelocity();
                                    SatCoord_transformed.setENU(current.getSatellitePosition().getSatCoordinates().getE(),current.getSatellitePosition().getSatCoordinates().getN(), current.getSatellitePosition().getSatCoordinates().getU());
                                    SatCoord_transformed.setENU(userCord.getE(), userCord.getN(), userCord.getU());
                                    teste.computeTopocentric(userCord_transformed, SatCoord_transformed);
                                    teste.getAzimuth();

                                }catch (NullPointerException e )
                                {

                                }

                                Log.i ("testea",Double.toString( teste.getAzimuth()));


                                Log.i ("entreeei","testee");

                            }catch (NullPointerException e)
                            {
                                Log.e("nullll","null");
                            }


                        }
                        */


                        try {
                            current.getSatellitePosition().computeSatellitePosition(current.getTransmissionTime());
                            //this.satelliteSkyPlots.add(new SatelliteSkyPlot(current.getId(), 0, current.getSatellitePosition().getSatElevation(), current.getConstellation()));
                            //this.satelliteSkyPlots.put(current.getId(),a);
                            Log.i("igorSAt", this.satelliteSkyPlots.toString());
                        } catch (NullPointerException e) {
                            Log.e("satNull", "null excepition to take satellite data");
                        }


                    }
                }
            }


            if (bundle.getSerializable("TrackedObservations") != null) {

                HashMap<String, GNSSObservation> gnssObservation = (HashMap<String, GNSSObservation>) bundle.getSerializable("TrackedObservations");

                for (HashMap.Entry<String, GNSSObservation> entry : gnssObservation.entrySet()) {

                    Log.i("entry??", "yes");
                    GNSSObservation current = entry.getValue();
/*
                    try {
                        Log.i("trackInfoGPS",Double.toString(current.getSatellitePosition().getSatElevation()));

                        SatelliteSkyPlot a = new SatelliteSkyPlot(current.getId(),0,current.getSatellitePosition().getSatElevation(),1);

                        if (!this.satelliteSkyPlots.containsKey(current.getId()))
                        {
                            this.satelliteSkyPlots.put(current.getId(),a);
                        }

                        Log.i("allinfo", a.toString());

                        //  this.graph.addSeries(this.satellitePositionOnSyplot);

                    } catch (NullPointerException e)
                    {
                        Log.e("merda","merda");
                    }
                    */

                    switch (current.getConstellation()) {
                        case GnssStatus.CONSTELLATION_GPS:
                            if (current.getPseudorangeL1() > 0) {
                                ///  gpsSatellitesTrackL1++;
                            }
                            if (current.getPseudorangeL5() > 0) {
                                // gpsSatellitesTrackL5 ++;
                            }
                            try {
                                Log.i("trackInfoGPS",Double.toString(current.getSatellitePosition().getSatElevation()));

                                SatelliteSkyPlot a = new SatelliteSkyPlot(current.getId(),0,current.getSatellitePosition().getSatElevation(),1);

                                if (!this.satelliteSkyPlots.containsKey(current.getId()))
                                {
                                    this.satelliteSkyPlots.put(current.getId(),a);
                                }

                                Log.i("allinfo", a.toString());

                              //  this.graph.addSeries(this.satellitePositionOnSyplot);

                            } catch (NullPointerException e)
                            {
                                Log.e("merda","merda");
                            }

                            break;
                        case GnssStatus.CONSTELLATION_GALILEO:
                            try {
                                Log.i("trackInfoGALLILIO", Double.toString(current.getSatellitePosition().getSatElevation()));
                            } catch (NullPointerException e)
                            {
                                Log.e("merda","merda");
                            }
                            break;
                        case GnssStatus.CONSTELLATION_BEIDOU:
                            try {
                                Log.i("trackInfoBEIDOU", Double.toString(current.getSatellitePosition().getSatElevation()));
                            } catch (NullPointerException e)
                            {
                                Log.e("merda","merda");
                            }

                            break;
                    }

                    // Log.i("lool2",Double.toString(current.getSatellitePosition().getSatElevation()));
                    //Log.i("testefinal2" ,current.getSatellitePosition().getSatCoordinates().toString());
                    //Log.i("lesinfo2" ," id: " + Integer.toString( current.getId() ) + " c :" + current.getConstellation() );
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

    }

}
class Downloader extends AsyncTask <String, Void, String> {

    protected String doInBackground(String... urls) {

        String resultat = null;
        try {

            URL url = new URL(urls[0]);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            InputStream in = con.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = "";

            while ((line = br.readLine()) != null) {
                if (line.contains("azimuth")) {
                    Double a, b;
                    a = Double.valueOf(line.substring(9 + line.indexOf("azimuth"), line.indexOf("azimuth") + 14));
                    b = Double.valueOf(line.substring(11 + line.indexOf("elevation"), line.indexOf("elevation") + 16));
                    resultat = resultat + "a:" + String.valueOf(a) + "b:" + String.valueOf(b);
                }
            }
        } catch (Exception e) {
            Log.e("Error Connecting to satellite positions", "error");
        }
        return resultat;
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
        this.dataPoint = new DataPoint(this.azimuth, this.elevation);
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
        return "satellite id: " + this.id + " azimuth: " + this.azimuth + " elevation: " + this.elevation + " Constelation: " + this.constelation;
    }
}
