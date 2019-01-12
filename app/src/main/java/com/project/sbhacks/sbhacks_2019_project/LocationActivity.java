package com.project.sbhacks.sbhacks_2019_project;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LocationActivity extends AppCompatActivity implements LocationListener {

    static final int REQUEST_LOCATION = 1;
    private LocationManager locationManager;
    private Location location;

    private Location location2;
    private float orientation;

    private static SensorManager sensorManager;
    private Sensor sensor;

    private View locationView;
    private TextView longText;
    private TextView latText;
    private Button button;
    private TextView longLabel;
    private TextView latLabel;
    private TextView angleLabel;
    private TextView rawAngleLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        locationView = (View) findViewById(R.id.location_view);

        longText = (TextView) findViewById(R.id.long_text);
        latText = (TextView) findViewById(R.id.lat_text);
        button = (Button) findViewById(R.id.button);
        longLabel = (TextView) findViewById(R.id.long_label);
        latLabel = (TextView) findViewById(R.id.lat_label);
        angleLabel = (TextView) findViewById(R.id.angle_label);
        rawAngleLabel = (TextView) findViewById(R.id.raw_angle_label);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getLocation();

        location2 = new Location("");
        location2.setLongitude(-119.845812);
        location2.setLatitude(34.413564);
        longText.setText("Longitude: " + location2.getLongitude());
        latText.setText("Latitude: " + location2.getLatitude());


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                getLocation();
                location2.setLongitude(Double.parseDouble(longText.getText().toString()));
                location2.setLatitude(Double.parseDouble(latText.getText().toString()));
            }
        });

        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                orientation = event.values[0];
                rawAngleLabel.setText("Raw Angle: " + orientation);
                updateBackground();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (sensor != null) {
            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                Log.d("Long", "" + location.getLongitude());
                Log.d("Lat", "" + location.getLatitude());
            } else
                Toast.makeText(this, "Nope", Toast.LENGTH_SHORT).show();
        }

        longLabel.setText("Longitude: " + location.getLongitude());
        latLabel.setText("Latitude: " + location.getLatitude());
    }

    float getAngle() {
        double dLat = (Math.PI / 180) * location2.getLatitude() - location.getLatitude();
        double dLong = (Math.PI / 180) * location2.getLongitude() - location.getLongitude();
        double bearing = (180 / Math.PI) * Math.atan2(dLong, dLat);
        double angle = Math.abs(orientation - bearing);
        if (angle > 180) {
            double rem = angle - 180;
            angle = 180 - rem;
        }

        angleLabel.setText("Angle: " + angle);

        return (float) angle;
    }

    void updateBackground() {
        float angle = getAngle();

        int color = Color.rgb((int) (255 * Math.sin(0.5 * Math.toRadians(angle))), (int) (255 * Math.cos(0.5 * Math.toRadians(angle))), 0);

        locationView.setBackgroundColor(color);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION:
                getLocation();
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        updateBackground();
        //textView.setText(i + " Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
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
}
