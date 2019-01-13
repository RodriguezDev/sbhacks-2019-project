package com.project.sbhacks.sbhacks_2019_project;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
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


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable pinRenderable;

    private  AnchorNode anchorNode;

    private LocationManager locationManager;
    private Location location1;
    private Location location2;

    private RequestQueue queue;
    private String url = "https://ar-back-end.herokuapp.com/api/location/dnguyen";

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        getLocationRequest();

        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

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

        //TransformableNode pin = new TransformableNode(arFragment.getTransformationSystem());
        //pin.setParent(arFragment.getArSceneView().getScene().getCamera());
        //pin.setLocalPosition(new Vector3(0, -2, -1));
        //pin.setRenderable(pinRenderable);
        //pin.select();

        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                if (arFragment.getArSceneView().getArFrame() == null) {
                    return;
                }

                if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
                    return;
                }

                if (anchorNode == null) {
                    Session session = arFragment.getArSceneView().getSession();

                    //Get difference vector of positions of points
                    //Find its unit vector
                    //Multiply by distance value
                    //Orient to AR space
                    float x = (float) (location2.getLongitude() - location1.getLongitude());
                    float y = (float) (location2.getLatitude() - location1.getLatitude());
                    Vector3 position = new Vector3(x, y, 0);
                    position = position.normalized().scaled(3);

                    //float[] pos = {0, 0, -1};
                    float[] pos = {position.x, position.y, position.z};
                    float[] rot = {0, 0, 0, 1};
                    Anchor anchor = session.createAnchor(new Pose(pos, rot));
                    anchorNode = new AnchorNode(anchor);
                    float scale = (float) 0.25;
                    anchorNode.setLocalScale(new Vector3(scale, scale, scale));
                    anchorNode.setRenderable(pinRenderable);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                } else {
                    Vector3 pos = Vector3.add(new Vector3(0, 0, -1), arFragment.getArSceneView().getScene().getCamera().getWorldPosition());
                    anchorNode.setWorldPosition(arFragment.getArSceneView().getScene().getCamera().getWorldPosition());
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

    private void getLocationRequest() {
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

    private void postLocationRequest() {
        
    }
}
