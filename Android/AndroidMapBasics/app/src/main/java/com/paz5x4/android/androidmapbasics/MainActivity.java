package com.paz5x4.android.androidmapbasics;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends Activity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener
{
    public static final String TAG = "Android map basics";

    //map object
    private GoogleMap mMap;
    //mapUI object
    private UiSettings mapUI;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Async map load
        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);//gets reference to map fragment
        mapFragment.getMapAsync(this);//calls onMapReady once loaded
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        LatLng start = new LatLng(48.395661, 9.989067);//Map start position

        mMap = googleMap;//assign returned map to reference
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15));//move map focus to start position

        //setup UI
        mapUI = mMap.getUiSettings();//get UI settings
        mapUI.setZoomControlsEnabled(true);//show zoom controls

        //add map click listeners
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);

    }

    @Override
    public void onMapClick(LatLng touchLatLng)
    {
        Log.e(TAG, "You clicked at: " + touchLatLng);//Log message
        mMap.addMarker(new MarkerOptions().position(touchLatLng));//Creates marker at spot
    }

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        marker.setTitle("Marker " + marker.getId());//sets marker's title to its ID
        marker.setSnippet("This marker is at : " + marker.getPosition());//sets marker's text
        return false;
    }
}
