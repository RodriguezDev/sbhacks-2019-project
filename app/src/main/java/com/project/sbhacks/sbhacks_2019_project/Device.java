package com.project.sbhacks.sbhacks_2019_project;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

public class Device {
    static final int REQUEST_LOCATION = 1;

    public static void getLocation(Context context, Activity activity, Location location, LocationManager locationManager) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    public static float getAngle(Location location1, Location location2, float orientation) {
        double dLat = (Math.PI / 180) * location2.getLatitude() - location1.getLatitude();
        double dLong = (Math.PI / 180) * location2.getLongitude() - location1.getLongitude();
        double bearing = (180 / Math.PI) * Math.atan2(dLong, dLat);
        double angle = Math.abs(orientation - bearing);
        if (angle > 180) {
            double rem = angle - 180;
            angle = 180 - rem;
        }

        return (float) angle;
    }

    public static float getDistance(Location location1, Location location2) {
        double R = 6373.0;
        double lat1 = Math.toRadians(location1.getLatitude());
        double long1 = Math.toRadians(location1.getLongitude());
        double lat2 = Math.toRadians(location2.getLatitude());
        double long2 = Math.toRadians(location2.getLongitude());

        double dLong = long2 - long1;
        double dLat = lat2 - lat1;

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLong / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = R * c;
        return (float) dist;
    }
}
