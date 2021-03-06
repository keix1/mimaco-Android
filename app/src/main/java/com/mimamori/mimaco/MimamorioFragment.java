package com.mimamori.mimaco;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import otoshimono.com.lost.mamorio.sdk.Mamorio;
import otoshimono.com.lost.mamorio.sdk.MamorioSDK;
import otoshimono.com.lost.mamorio.sdk.User;
import otoshimono.com.lost.mamorio.sdk.Error;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;


public class MimamorioFragment extends Fragment implements OnMapReadyCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = MimamorioFragment.class.getName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View rootView;

    private OnFragmentInteractionListener mListener;


    // Fused Location Provider API.
    private FusedLocationProviderClient fusedLocationClient;

    // Location Settings APIs.
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location location;
    private String lastUpdateTime;
    private Boolean requestingLocationUpdates;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private int priority = 0;

    private KEYS keys = new KEYS();

    private String APP_TOKEN = "APP_TOKEN";
    private PointManager pointManager;
    private String MY_USERNAME = "grandpa";

    //UI部品
    private TextView pointText;
    private Button gpsButton;
    private Button heatMapButton;


    private GoogleMap mMap;
    private MapView mapView;
    private LocationManager locationManager;
    private List<Marker> mMarkerList;

    private Vibrator vib;
    private long pattern[] = {1000, 200, 700, 200, 400, 200 };


    public MimamorioFragment() {
        // Required empty public constructor

    }

    @Override
    public void onStart(){
        super.onStart();
        Log.i("LifeCycle", "onStart");

        pointManager = new PointManager();
        locationManager = new LocationManager();
        mMarkerList = new ArrayList<>();




        // ACCESS_FINE_LOCATIONの許可(Android 6.0以上向け）
        if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {

            MamorioSDK.setUp(getActivity().getBaseContext(), APP_TOKEN); // 探索専用
            step1();
        }

        try {
//            Log.d(TAG, pointManager.findWatchedUser("grandpa", 33333, 44444, 35.5704322, 139.7325909).getString("username"));
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }

        startLocationUpdates();
//        setInitialLocation();

//        int point = pointManager.getPoint(MY_USERNAME);
//        pointText.setText("ポイントは " + point);


//         一定ごとにUI更新（ポイント）
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int point = pointManager.getPoint(MY_USERNAME);
                            pointText.setText("ポイント：" + point);
                            Log.d("now_point", "ポイント：" + point);
                        }
                    });

                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }).start();


    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MimamorioFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MimamorioFragment newInstance(String param1, String param2) {
        MimamorioFragment fragment = new MimamorioFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


        //起動時に位置を現在地へ
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                int count = 0;
//                while(true) {
//                    if(count < 5) {
//                        getActivity().runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                double latitudeSeed = 0.0f;
//                                double longitudeSeed = 0.0f;
//
//                                if (location != null && mMap != null) {
//                                    latitudeSeed = location.getLatitude();
//                                    longitudeSeed = location.getLongitude();
//
//                                    LatLng focus = new LatLng(latitudeSeed, longitudeSeed);
//                                    mMap.addMarker(new MarkerOptions().position(focus).title("現在位置").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
//                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16));
//                                }
//                            }
//                        });
//
//                        try {
//                            Thread.sleep(3000);
//                        } catch (Exception e) {
//                            Log.e(TAG, e.getMessage());
//                        }
//                        count += 1;
//                    }
//                }
//            }
//        }).start();


        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(getActivity());
        settingsClient = LocationServices.getSettingsClient(getActivity());
        priority = 0;

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

    }

    private void setInitialLocation() {
        // 一定ごとにUI更新（ポイント）
        new Thread(new Runnable() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        double latitudeSeed = 35.6;
                        double longitudeSeed = 139.7;
                        try {
                            latitudeSeed = location.getLatitude();
                            longitudeSeed = location.getLongitude();

                            LatLng focus = new LatLng(latitudeSeed, longitudeSeed);
                            mMap.addMarker(new MarkerOptions().position(focus).title("現在位置").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16));
                        } catch (Exception e) {
                            Log.e("setInitialLocation", e.getMessage());
                        }
                    }
                });

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

                if(location==null) {
                    setInitialLocation();
                }
            }
        }).start();

    }

private void heatMapStub() {
        if(location != null) {
            if(mMarkerList != null) {
                for(Marker marker : mMarkerList) {
                    marker.remove();
                }
            }
            //会場
//            double latitudeSeed = 35.535347;
//            double longitudeSeed = 139.700937;

            //和菓子　岩
            double latitudeSeed = 35.53575;
            double longitudeSeed = 139.690338;

            LatLng focus = new LatLng(latitudeSeed, longitudeSeed);
            mMarkerList.add(mMap.addMarker(new MarkerOptions().position(focus).title("現在位置").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16));

            List<List<Double>> result = locationManager.getHeatMapTest(latitudeSeed, longitudeSeed);
            for(List<Double> point : result) {
                Log.d("ChildrenPoint", point.get(0) + ", " + point.get(1));
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(point.get(0), point.get(1)));
                options.title("子供いる");
                options.snippet("ポイント率高");
                Marker marker = mMap.addMarker(options);
                marker.showInfoWindow();
                mMarkerList.add(marker);
            }
        }
    }

    private void heatMapStubaaa() {
        if(location != null) {
            if(mMarkerList != null) {
                for(Marker marker : mMarkerList) {
                    marker.remove();
                }
            }
            //会場
//            double latitudeSeed = 35.535347;
//            double longitudeSeed = 139.700937;

            //和菓子　岩
            double latitudeSeed = 35.53575;
            double longitudeSeed = 139.690338;

            LatLng focus = new LatLng(latitudeSeed, longitudeSeed);
            mMarkerList.add(mMap.addMarker(new MarkerOptions().position(focus).title("現在位置").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16));

            List<List<Double>> result = locationManager.getHeatMapTest(latitudeSeed, longitudeSeed);
            for(List<Double> point : result) {
                Log.d("ChildrenPoint", point.get(0) + ", " + point.get(1));
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(point.get(0), point.get(1)));
                options.title("子供いる");
                if(point.get(2).equals("1")) {
                    Log.d("locloc", "111111");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    options.snippet("ポイント率：低");
                }
                else if(point.get(2).equals("2")) {
                    Log.d("locloc", "222");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    options.snippet("ポイント率：中");
                }
                else if(point.get(2).equals("3")) {
                    Log.d("locloc", "33333");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    options.snippet("ポイント率：高");
                } else {
                    Log.d("locloc", "44444");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                    options.snippet("ポイント率：高");
                }
                Marker marker = mMap.addMarker(options);
                marker.showInfoWindow();
                mMarkerList.add(marker);
            }
        }
    }

    private void heatMap() {
        if(location != null) {
            if(mMarkerList != null) {
                for(Marker marker : mMarkerList) {
                    marker.remove();
                }
            }
//            double latitudeSeed = 35.6;
//            double longitudeSeed = 139.7;
            double latitudeSeed = location.getLatitude();
            double longitudeSeed = location.getLongitude();

            LatLng focus = new LatLng(latitudeSeed, longitudeSeed);
            mMarkerList.add(mMap.addMarker(new MarkerOptions().position(focus).title("現在位置").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 15));

            List<List<Double>> result = locationManager.getHeatMap(MY_USERNAME, latitudeSeed, longitudeSeed);
            for(List<Double> point : result) {
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(point.get(0), point.get(1)));
                options.title("子供いる");

                if(point.get(2).equals("1")) {
                    Log.d("locloc", "111111");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    options.snippet("ポイント率：低");
                }
                else if(point.get(2).equals("2")) {
                    Log.d("locloc", "222");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    options.snippet("ポイント率：中");
                }
                else if(point.get(2).equals("3")) {
                    Log.d("locloc", "33333");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    options.snippet("ポイント率：高");
                } else {
                    Log.d("locloc", "44444");
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                    options.snippet("ポイント率：高");
                }
                Marker marker = mMap.addMarker(options);
                marker.showInfoWindow();
                mMarkerList.add(marker);
            }
        }
    }


    void step1()
    {
        Log.d(TAG,"step1()");

        User.signUpAsTemporalUser(new User.UserCallback() {
            @Override
            public void onSuccess(User user) {
                //アカウント登録成功時の処理
                Log.d(TAG,"捜索専用ユーザーの登録に成功しました。ペアリングは行わず、周囲を高精度で探索し続け「みんなでさがす」に貢献します。");
                step2();
            }

            @Override
            public void onError(Error error) {
                //アカウント登録失敗時の処理
                Log.d(TAG,"捜索専用ユーザーの登録に失敗しました。エラーメッセージ：" + error.getMessage());
            }
        });
    }

    void step2()
    {
        Log.d(TAG,"step2()");

        MamorioSDK.rangingStart(
                new MamorioSDK.RangingInitializeCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"レンジングの開始に成功しました。");
                    }
                    @Override
                    public void onError(Error error) {
                        Log.d(TAG,"Sレンジングの開始に失敗しました。エラーメッセージ："+error.getMessage());
                    }
                },
                //MAMORIO発見時のコールバック（null可）
                new MamorioSDK.RangingCallbackEnter() {
                    @Override
                    public void onEnter(List<Mamorio> list) {
                        //list:発見したMAMORIOの一覧
                        //nullの場合コールバックしない
                        Log.d(TAG,"RangingCallbackEnter()");

                        for(int i = 0 ; i < list.size() ; i++) {
                            Mamorio dev = list.get(i);

                            String name = dev.getName();
                            if(name != null){
                                Log.d(TAG,"name=" + name);
                            }

                            int major = dev.getMajor();
                            int minor = dev.getMinor();
                            Log.d(TAG,major + "," + minor);


                            if(dev.isNotYours() == true) {
                                Log.d(TAG,"他人のMAMORIO");
                                if(location!=null) {
                                    double latitude = location.getLatitude();
                                    double longitude = location.getLongitude();
                                    Log.d("mamorio_major_minor", "major" + major + "," + "minor" + minor);
                                    pointManager.findWatchedUser(MY_USERNAME, major, minor, latitude, longitude);
//                                    Toast.makeText(getActivity(), "ポイントUP", Toast.LENGTH_LONG).show();
//                                    // Vibratorクラスのインスタンス取得
//                                    vib = (Vibrator)getActivity().getSystemService(VIBRATOR_SERVICE);
//                                    vib.vibrate(100);


                                }

                            } else {
                                Log.d(TAG,"自分のMAMORIO");
                            }
                        }
                    }
                },
                //MAMORIO紛失時のコールバック(null可)
                new MamorioSDK.RangingCallbackExit() {
                    @Override
                    public void onExit(List<Mamorio> list) {
                        //list:紛失した自分のMAMORIO一覧
                        Log.d(TAG,"RangingCallbackExit()");

                        for(int i = 0 ; i < list.size() ; i++) {
                            Mamorio dev = list.get(i);

                            String name = dev.getName();
                            if(name != null){
                                Log.d(TAG,"name=" + name);
                            }

                            int major = dev.getMajor();
                            int minor = dev.getMinor();
                            Log.d(TAG,major + "," + minor);

                            if(dev.isNotYours() == true) {
                                Log.d(TAG,"他人のMAMORIO");
                            } else {
                                Log.d(TAG,"自分のMAMORIO");
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"onStop()");
        MamorioSDK.rangingStop();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_mimamorio, container, false);
        pointText = rootView.findViewById(R.id.pointText);
//        gpsButton = rootView.findViewById(R.id.gpsButton);
//        gpsButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Add a marker in Sydney and move the camera
//
//                if(mMarkerList != null) {
//                    for(Marker marker : mMarkerList) {
//                        marker.remove();
//                    }
//                }
//
//                if(location != null) {
//                    //以下現在位置
//                    LatLng focus = new LatLng(-34, 151);
//                    focus = new LatLng(location.getLatitude(), location.getLongitude());
//                    Marker marker = mMap.addMarker(new MarkerOptions().position(focus).title("現在位置"));
//                    mMarkerList.add(marker);
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16));
////                    statusBarNitify();
//                }
//            }
//        });

        heatMapButton = rootView.findViewById(R.id.heatMapButton);
        heatMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                heatMap();
//                heatMapStub();
            }
        });

        mapView = (MapView) rootView.findViewById(R.id.map_mimamorio);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }


    private void statusBarNitify() {

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("My Notification")
                        .setContentText("Hello World!")
                        .setTicker("notification is displayed !!");

        int mNotificationId = 001;

        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    // FusedLocationApiによるlocation updatesをリクエスト
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(getActivity(),
                        new OnSuccessListener<LocationSettingsResponse>() {
                            @Override
                            public void onSuccess(
                                    LocationSettingsResponse locationSettingsResponse) {
                                Log.i("debug", "All location settings are satisfied.");

                                // パーミッションの確認
                                if (ActivityCompat.checkSelfPermission(
                                        getActivity(),
                                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED
                                        && ActivityCompat.checkSelfPermission(
                                        getActivity(),
                                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {

                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                fusedLocationClient.requestLocationUpdates(
                                        locationRequest, locationCallback, Looper.myLooper());

                            }
                        })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i("debug", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(
                                            getActivity(),
                                            REQUEST_CHECK_SETTINGS);

                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("debug", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e("debug", errorMessage);
                                Toast.makeText(getActivity(),
                                        errorMessage, Toast.LENGTH_LONG).show();

                                requestingLocationUpdates = false;
                        }

                    }
                });

        requestingLocationUpdates = true;
    }

    // locationのコールバックを受け取る
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                location = locationResult.getLastLocation();

                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {
        // getLastLocation()からの情報がある場合のみ
        if (location != null) {

            String fusedName[] = {
                    "Latitude", "Longitude", "Accuracy",
                    "Altitude", "Speed", "Bearing"
            };

            double fusedData[] = {
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    location.getAltitude(),
                    location.getSpeed(),
                    location.getBearing()
            };

            StringBuilder strBuf =
                    new StringBuilder("---------- UpdateLocation ---------- \n");

            for(int i=0; i< fusedName.length; i++) {
                strBuf.append(fusedName[i]);
                strBuf.append(" = ");
                strBuf.append(String.valueOf(fusedData[i]));
                strBuf.append("\n");
            }

            strBuf.append("Time");
            strBuf.append(" = ");
            strBuf.append(lastUpdateTime);
            strBuf.append("\n");

            Log.d("MyLocation", "" + strBuf);

        }

    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        if (priority == 0) {
            // 高い精度の位置情報を取得したい場合
            // インターバルを例えば5000msecに設定すれば
            // マップアプリのようなリアルタイム測位となる
            // 主に精度重視のためGPSが優先的に使われる
            locationRequest.setPriority(
                    LocationRequest.PRIORITY_HIGH_ACCURACY);

        } else if (priority == 1) {
            // バッテリー消費を抑えたい場合、精度は100mと悪くなる
            // 主にwifi,電話網での位置情報が主となる
            // この設定の例としては　setInterval(1時間)、setFastestInterval(1分)
            locationRequest.setPriority(
                    LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        } else if (priority == 2) {
            // バッテリー消費を抑えたい場合、精度は10kmと悪くなる
            locationRequest.setPriority(
                    LocationRequest.PRIORITY_LOW_POWER);

        } else {
            // 受け身的な位置情報取得でアプリが自ら測位せず、
            // 他のアプリで得られた位置情報は入手できる
            locationRequest.setPriority(
                    LocationRequest.PRIORITY_NO_POWER);
        }

        // アップデートのインターバル期間設定
        // このインターバルは測位データがない場合はアップデートしません
        // また状況によってはこの時間よりも長くなることもあり
        // 必ずしも正確な時間ではありません
        // 他に同様のアプリが短いインターバルでアップデートしていると
        // それに影響されインターバルが短くなることがあります。
        // 単位：msec
//        locationRequest.setInterval(60000);
        // このインターバル時間は正確です。これより早いアップデートはしません。
        // 単位：msec
        locationRequest.setFastestInterval(1000);

    }

    // 端末で測位できる状態か確認する。wifi, GPSなどがOffになっているとエラー情報のダイアログが出る
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        LatLng focus = new LatLng(35, 139);
        // Add a marker in Sydney and move the camera
//        if(location != null) {
//            focus = new LatLng(location.getLatitude(), location.getLongitude());
//        }
//        mMap.addMarker(new MarkerOptions().position(focus).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(focus));

//        MarkerOptions options = new MarkerOptions();
//        options.position(new LatLng(35, 151));
//        options.title("テストMaker");
//        options.snippet("補足情報を記載");
//        Marker marker = mMap.addMarker(options);
//        marker.showInfoWindow();


        //和菓子　岩
            double latitudeSeed = 35.53575;
            double longitudeSeed = 139.690338;

            LatLng focus = new LatLng(latitudeSeed, longitudeSeed);
//            mMarkerList.add(mMap.addMarker(new MarkerOptions().position(focus).title("現在位置").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 14));


    }



}
