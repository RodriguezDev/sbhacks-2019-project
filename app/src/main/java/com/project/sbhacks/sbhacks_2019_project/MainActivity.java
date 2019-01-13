package com.project.sbhacks.sbhacks_2019_project;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.LocationListener;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable pinRenderable;

    private String username;

    private Node node;

    private LocationManager locationManager;
    private Location location1;
    private Location location2;

    private float orientation;

    private RequestQueue queue;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        username = getIntent().getStringExtra("username");
        Log.d("Username", username);

        //Intent intent = new Intent(this, LocationActivity.class);
        //startActivity(intent);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        location1 = new Location("");
        location2 = new Location("");

        queue = Volley.newRequestQueue(this);

        location1 = Device.getLocation(this, this, locationManager);

        // This method is used to get the location of the current user
        postLocationRequest(username);

        // This method is used to get the cccc's location
        getLocationRequest();


        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        ModelRenderable.builder()
                .setSource(this, Uri.parse("pin.sfb"))
                .build()
                .thenAccept(renderable -> pinRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load pin renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                if (arFragment.getArSceneView().getArFrame() == null) {
                    return;
                }

                if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
                    return;
                }

                if (node == null) {
                    node = createPin();
                } else if (isNodeClose(node, 0.75f)) {
                    arFragment.getArSceneView().getScene().removeChild(node);
                    node = null;
                } else {
                    //Rotate pin
                    Quaternion q1 = node.getLocalRotation();
                    Quaternion q2 = Quaternion.axisAngle(new Vector3(0, 1, 0), 3.5f);
                    node.setLocalRotation(Quaternion.multiply(q1, q2));
                }

            }
        });

/*
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (pinRenderable == null) {
                        return;
                    }

                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    TransformableNode pin = new TransformableNode(arFragment.getTransformationSystem());
                    pin.setParent(anchorNode);
                    pin.setRenderable(pinRenderable);
                    pin.select();
                });
*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        String url = "https://ar-back-end.herokuapp.com/api/location/delete/" + username;
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error.Response", error.getMessage());
                    }
                });
        queue.add(postRequest);
    }

    private Node createPin() {
        Node node;
        Session session = arFragment.getArSceneView().getSession();

        float x = (float) (location2.getLongitude() - location1.getLongitude());
        float y = (float) (location2.getLatitude() - location1.getLatitude());
        Vector3 position = new Vector3(x, 0, y);
        //position = position.normalized().scaled(4);

        //Vector3 position = new Vector3(0, 0, -4);
        position = new Vector3((float) (Math.random() * 4), 0, (float) (Math.random() * 4));

        node = new Node();
        float scale = (float) 0.25;
        node.setLocalScale(new Vector3(scale, scale, scale));
        node.setRenderable(pinRenderable);
        node.setParent(arFragment.getArSceneView().getScene());
        node.setLocalPosition(position);
        return node;
    }

    private boolean isNodeClose(Node node, float dist) {
        Vector3 pos1 = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        Vector3 pos2 = node.getWorldPosition();
        double diff = Math.pow(pos2.x - pos1.x, 2) + Math.pow(pos2.y - pos1.y, 2) + Math.pow(pos2.z - pos1.z, 2);
        return Math.pow(dist, 2) > diff;
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        location1 = location;
    }

    private void getLocationRequest() {
        String url = "https://ar-back-end.herokuapp.com/api/location/cccc";
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response", response.toString());
                        try {
                            double longitude = Double.parseDouble(response.get("long").toString());
                            double latitude = Double.parseDouble(response.get("lat").toString());
                            location2.setLongitude(longitude);
                            location2.setLatitude(latitude);
                        } catch (JSONException e) {
                            Log.d("Error", e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.d("Error Response", e.getMessage());
            }

        });
        queue.add(getRequest);
    }

    private void postLocationRequest(String username) {
        String url = "https://ar-back-end.herokuapp.com/api/location/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error.Response", error.getMessage());
                    }
                })
        {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("username", username);
                params.put("long", "" + location1.getLongitude());
                params.put("lat", "" + location1.getLatitude());

                return params;
            }
        };
        queue.add(postRequest);
    }
}
