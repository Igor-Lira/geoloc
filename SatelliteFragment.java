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

import java.util.Objects;

import fr.ifsttar.geoloc.geolocpvt.R;

/**
 * the fragment of skyplot of the satellites
 */
public class SatelliteFragment extends Fragment {

    private PointsGraphSeries<DataPoint> satellitePositionOnSyplot;
    private GraphView graph;

    //defining the xml for the fragment
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_satellite, null);

        this.graph = (GraphView) view.findViewById(R.id.graph);

        defaultGraphic();

        refreshData();

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
                refreshPointsToPlot();

                //We change the shape of the points:
                satellitePositionOnSyplot.setShape(PointsGraphSeries.Shape.POINT);

                // If the user wants to know about one satellite, they click and it displays some information:
                satellitePositionOnSyplot.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        Toast.makeText(SatelliteFragment.this.getActivity(), "The satellite position is : "+dataPoint, Toast.LENGTH_SHORT).show();
                    }
                });

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
        //We need to get the Data from the site and put here MONGI ::
        this.satellitePositionOnSyplot = new PointsGraphSeries<>(new DataPoint[]{
                new DataPoint(-10, 20),
                new DataPoint(50, 50)
        });
        graph.addSeries(satellitePositionOnSyplot);

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

    }
}
