package com.example.bataanresponse;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class GoogleMapSearch extends FragmentActivity implements OnMapReadyCallback {

    //    GoogleMap mMap;
    GoogleMap map;
    SupportMapFragment mapFragment;
    SearchView searchView;

    LatLng myLatLng;
    LatLng incidentLatLng;


    TextView distanceTxtView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map_search);

        distanceTxtView = findViewById(R.id.distanceTxt);
        /*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);*/

        searchView = findViewById(R.id.sv_location);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        mapFragment.getMapAsync(this);

    }

    void searchLocation(){
        String location = searchView.getQuery().toString();
        List<Address> addressList = null;
        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(GoogleMapSearch.this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("CHECKPOINT 4", "Right here");
            System.out.println(addressList);
            if (!addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                map.addMarker(new MarkerOptions().position(latLng).title(location));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

        }
    }

    void getIncidentLocation(String location){
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(GoogleMapSearch.this);
        try {
            addressList = geocoder.getFromLocationName(location, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("CHECKPOINT 4", "Right here");
        if (!addressList.isEmpty()) {
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            map.addMarker(new MarkerOptions().position(latLng).title(location).icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.people)));
            Log.e("INCIDENT MARKER", "location 2");
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            incidentLatLng = latLng;

            Log.e("Error2222222222222",myLatLng+"-"+latLng);
            if(myLatLng!=null) {
                float[] results = new float[1];
                Location.distanceBetween(latLng.latitude, latLng.longitude,
                        myLatLng.latitude, myLatLng.longitude,
                        results);
//                distanceTxtView.setText(CalculationByDistance(myLatLng,latLng));
                distanceTxtView.setText(String.format("DISTANCE: %.2f KM | %.2f M",results[0]/1000,results[0]));
            }


        }else {
            getIncidentLocation(location.substring(0,location.lastIndexOf("-")));

        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable= ContextCompat.getDrawable(context,vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap =Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    String CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);


        return kmInDec+" KILOMETER/s | "+meterInDec+" METER/s";
    }

    void getMyLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location arg0) {
                LatLng latLng = new LatLng(arg0.getLatitude(), arg0.getLongitude());
                map.addMarker(new MarkerOptions().position(new LatLng(arg0.getLatitude(), arg0.getLongitude())).title("Barangay Admin").icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.newbase)));
                map.setOnMyLocationChangeListener(null);
                myLatLng = latLng;
                Log.e("INCIDENT MARKER", "location 1");
                if(getIntent().getStringExtra("location") != null) {
                    getIncidentLocation(getIntent().getStringExtra("location"));
                }
                //get location
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
                Drawable vectorDrawable= ContextCompat.getDrawable(context,vectorResId);
                vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),
                        vectorDrawable.getIntrinsicHeight());
                Bitmap bitmap =Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                        vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                vectorDrawable.draw(canvas);
                return BitmapDescriptorFactory.fromBitmap(bitmap);
            }

        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        getMyLocation();
        if(getIntent().getStringExtra("location") != null) {
            getIncidentLocation(getIntent().getStringExtra("location"));
        }
//        mMap = googleMap;
//
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
