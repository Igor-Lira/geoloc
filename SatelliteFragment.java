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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import fr.ifsttar.geoloc.geolocpvt.R;

/**
 * the fragment of skyplot of the satellites
 */
public class SatelliteFragment extends Fragment {
    private PointsGraphSeries<DataPoint> series;
    private GraphView graph;
    private View viewSkyplot;

    //defining the xml for the fragment
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_satellite, null);

         this.graph = (GraphView) view.findViewById(R.id.graph);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);

        graph.getViewport().setMinX(-270);
        graph.getViewport().setMaxX(90);
        graph.getViewport().setMinY(-180);
        graph.getViewport().setMaxY(0);

       // Drawable image = ContextCompat.getDrawable(this, R.drawable.common_google_signin_btn_icon_dark);
        graph.setBackground(image);



        this.series = new PointsGraphSeries<>(new DataPoint[]{
                new DataPoint(1, 2),
                new DataPoint(1, 0)
        });
             graph.addSeries(series);
             series.setShape(PointsGraphSeries.Shape.POINT);




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
            }
        });
    }
    public PointsGraphSeries<DataPoint> refreshPointsToPlot()
    {
        this.series = new PointsGraphSeries<>(new DataPoint[]{
                new DataPoint(1, 2),
                new DataPoint(1, 0)
        });
        Log.d("deu bom","deu bom");
        return this.series;

    }
}
