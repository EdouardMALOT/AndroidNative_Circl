package io.circl.circl;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;

import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity  implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    //CONSTANTES
    //----------
    private final static String TAG                     = "Circl";

    //Server adresses
    public static final String Website                  = "http://www.place2b.ovh/";

    private static final String AdrGetMorningEventMark   = Website +"AppGetEventMark.php?DayTime=Morning";
    private static final String AdrGetNoonEventMark      = Website +"AppGetEventMark.php?DayTime=Noon";
    private static final String AdrGetNightEventMark     = Website +"AppGetEventMark.php?DayTime=Night";

    private static final String AdrUsedForEvent          = Website +"AppGetEvent.php";
    private static final String AdrUserAddEvent          = Website +"AppUserAddEvent.php";

    public static final int Nbhttpretry                  = 5;

    //Map parameters
    private static final double DefaultZoom = 13.0;
    private static final int MoveToMyLocationDelay = 1500;          //Delais de 1.5secondes avant de commencer le zoom
    private static final int ZoomAnimationDuration = 2000;          //Durée du zoom
    private static boolean InhibeUpdateZoom = false;

    //Name used to save params when Event screen is loaded
    public static final String AppSharedName                   = "Circl";

    private static final double AixEnProvenceLat= 43.526312;
    private static final double AixEnProvenceLng= 5.445431;


    //VARIABLES
    //---------

    //Map
    private GoogleMap mMap;                                         //MAPS
    private Timer TimerMoveToMyLocation = null;                     //Timer for initial move to my location delay !
    private Timer TimerUpdateZoomAfterIntro = null;

    private static final int PERMISSIONS_GPS_STORAGE = 1;

    private static final int REQUEST_GPS_STORAGE = 1;
    private static String[] PERMISSIONS_GPS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };


    //Markeurs
    public class MarkerEventListType{
        public double Coordonnees[];
        public String Type;
    }
    List<MarkerEventListType> MarkEventList = new ArrayList<MarkerEventListType>();   // Event Mark list (Datas from server)
    List<Marker>    MarkerList = new ArrayList<>();                 // Event Mark for Google MAP API
    List<Integer>   MarkerIdList = new ArrayList<>();               // Event Mark ID.
    //TODO : Make a type of this 2 list above. It will be easier to understand

    //Dialog
    private ProgressDialog DiagWaitMsg;                             //Message d'attente lors d'un chargement

    //Boutons
    //-------
    Button BP_Morning;
    Button BP_Noon;
    Button BP_Night;
    FloatingActionButton BP_AddEvent;


    //TODO : create an Enum type
    public static final int ModeMorning = 1;
    public static final int ModeMoon = 2;
    public static final int ModeNight = 3;

    int DisplayedMode;

    //Asynck Task
    private AsyncTask TaskGetEventMarkFromServer = null;
    private AsyncTask TaskGetEventFromServer = null;
    private AsyncTask UserEventTask = null;



    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                   onCreate                   //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        //Update Google Play if needed
        //----------------------------
        isGooglePlayServicesAvailable(MapsActivity.this);

        //Active GPS (As soon as possible)
        //--------------------------------
        //Coordonnées GPS
        //GPSTracker mGPS;
        //mGPS = new GPSTracker(this);
        //mGPS.getLocation();

        // Lock the screen in portrait mode
        //---------------------------------
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Log on Facebook
        //----------------
        //FacebookSdk.sdkInitialize(getApplicationContext());
        //AppEventsLogger.activateApp(getApplication());


        // Gmap Fragment
        //--------------
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment.getMapAsync(this);


        // Wait Dialogue box init
        //-----------------------
        DiagWaitMsg = new ProgressDialog(this);
        DiagWaitMsg.setIndeterminate(true);
        DiagWaitMsg.setCancelable(false);
        DiagWaitMsg.setMessage(getString(R.string.LoadingText));


        //BackGroundService
        //-----------------
        //if( isMyServiceRunning(BackGroundService.class) == false) {
        //    Intent BackGroundServiceIntent = new Intent(this, BackGroundService.class);
        //    startService(BackGroundServiceIntent);
        //}

        //Morning button
        //---------------
        BP_Morning = (Button) findViewById(R.id.button_morning);
        BP_Morning.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.MorningToastMsg), Toast.LENGTH_SHORT).show();
                UpdateMode(ModeMorning);
            }
        });


        //Moon button
        //-----------
        BP_Noon = (Button) findViewById(R.id.button_moon);
        BP_Noon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.NoonToastMsg), Toast.LENGTH_SHORT).show();
                UpdateMode(ModeMoon);
            }
        });

        //Night button
        //-------------
        BP_Night = (Button) findViewById(R.id.button_night);
        BP_Night.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.NightToastMsg), Toast.LENGTH_SHORT).show();
                UpdateMode(ModeNight);
            }
        });

        //Add Event Button
        //----------------
        BP_AddEvent = (FloatingActionButton) findViewById(R.id.AddEventBtn);
        BP_AddEvent.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UserAddEvent();
            }
        });


        //Check If zoom was saved
        //------------------------
        SharedPreferences prefs = getSharedPreferences(AppSharedName, MODE_PRIVATE);

        if(prefs.contains("ModeAffichage"))
        {
            int SavedDisplayedMode = prefs.getInt("ModeAffichage", ModeMorning);
            prefs.edit().remove("ModeAffichage").commit();

            Log.d(TAG, "DisplayedMode restore: " + SavedDisplayedMode);
            UpdateMode(SavedDisplayedMode);
        }else {

            //Update Mode
            //------------
            Log.d(TAG, "Auto update DisplayedMode accordind to the day time : ");

            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            switch (hour) {
                case 0 :
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    UpdateMode(ModeNight);
                    break;

                case 6 :
                case 7 :
                case 8 :
                case 9 :
                case 10 :
                case 11 :
                case 12 :
                    UpdateMode(ModeMorning);
                    break;
                case 13 :
                case 14 :
                case 15 :
                case 16 :
                case 17 :
                case 18 :
                case 19 :
                    UpdateMode(ModeMoon);
                    break;

                case 20 :
                case 21 :
                case 22 :
                case 23 :
                    UpdateMode(ModeNight);
                    break;
                default:
                    UpdateMode(ModeMoon);
                    break;
            }
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        GoogleApiClient client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //      isGooglePlayServicesAvailable           //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    public boolean isGooglePlayServicesAvailable(Activity activity) {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);

        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //              attachBaseContext               //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }



    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  onDestroy                   //
    //                                              //
    //                                              //
    // Call before close App, destroy all AsyncTask //
    //                                              //
    //////////////////////////////////////////////////
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Stop the background App if running in progress
        if (TaskGetEventMarkFromServer != null){
            TaskGetEventMarkFromServer.cancel(true);
        }

        if(TaskGetEventFromServer != null){
            TaskGetEventFromServer.cancel(true);
        }

        if(UserEventTask != null){
            UserEventTask.cancel(true);
        }

        if (TimerMoveToMyLocation != null) {
            TimerMoveToMyLocation.cancel();
            TimerMoveToMyLocation.purge();
        }

        if(TimerUpdateZoomAfterIntro != null) {
            TimerUpdateZoomAfterIntro.cancel();
            TimerUpdateZoomAfterIntro.purge();
        }
    }


    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  UpdateMode                  //
    //                                              //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    private void UpdateMode(int Mode) {

        switch (Mode) {

            case ModeMorning:

                BP_Morning.setBackgroundResource(R.drawable.reveilplein);
                BP_Noon.setBackgroundResource(R.drawable.soleilvide);
                BP_Night.setBackgroundResource(R.drawable.lunevide);
                DisplayedMode =  ModeMorning;

                break;

            case ModeMoon:

                BP_Morning.setBackgroundResource(R.drawable.reveilvide);
                BP_Noon.setBackgroundResource(R.drawable.soleilplein);
                BP_Night.setBackgroundResource(R.drawable.lunevide);
                DisplayedMode =  ModeMoon;

                break;

            case ModeNight:
            default:

                BP_Morning.setBackgroundResource(R.drawable.reveilvide);
                BP_Noon.setBackgroundResource(R.drawable.soleilvide);
                BP_Night.setBackgroundResource(R.drawable.lunepleine);
                DisplayedMode = ModeNight;

                break;
        }

        //Get event Marks
        //---------------
        //Cancel TaskGetEventMarkFromServer if running
        if (TaskGetEventMarkFromServer != null) {
            TaskGetEventMarkFromServer.cancel(true);
        }

        //Update Event Mark
        TaskGetEventMarkFromServer = new JSONTaskGetEventMark().execute();
    }


    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  UpdateMode                  //
    //                                              //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    private void UpdateZoom() {

        if(InhibeUpdateZoom == true)
        {
            InhibeUpdateZoom = false;
        }else {
            //Update Zoom according to Bounds
            //-------------------------------

            if(MarkEventList.size() == 0){

                //Zoom center of City center
                    LatLng latlng = new LatLng(AixEnProvenceLat, AixEnProvenceLng);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, (float) DefaultZoom), ZoomAnimationDuration, null); //Animates camera and zooms to preferred state on the user's current location.;

            }else {
                //Compute Bounds
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                //Make Bounds according to EventMark
                    for (int i = 0; i < MarkEventList.size(); i++) {
                        builder.include(new LatLng(MarkEventList.get(i).Coordonnees[2], MarkEventList.get(i).Coordonnees[1]));
                    }

                //Add Aix-en-Provence city-center
                    builder.include(new LatLng(AixEnProvenceLat, AixEnProvenceLng));
                    LatLngBounds bounds = builder.build();

                //Update camera
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100)); // 100 =offset from edges of the map in pixels
            }
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                   isOnline                   //
    //                                              //
    // Check if there is an internet connexion      //
    //////////////////////////////////////////////////
    public boolean isOnline() {

        Context context = getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }

        return false;
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //            isMyServiceRunning                //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //**********************************************************************************************
    //
    //                                  Asynchrone Task
    //
    //**********************************************************************************************

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                onMapReady                    //
    //                                              //
    //                                              //
    // Call When Map is ready                       //
    //                                              //
    //////////////////////////////////////////////////
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Get Map
        //-------
        mMap = googleMap;

        if (mMap != null) {

            VerifyGpsPermissions(MapsActivity.this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                //return;
            }else {
                mMap.setMyLocationEnabled(true);
            }

            mMap.getUiSettings().setZoomControlsEnabled(true);                              //Ajout du controle de Zoom
        }

        //Inhibe zoom update
        //------------------
        InhibeUpdateZoom = true;


        //Check If zoom was saved
        //------------------------
        SharedPreferences prefs = getSharedPreferences(AppSharedName, MODE_PRIVATE);

        if(prefs.contains("CameraPositionSaved"))
        {
            String PreviousCameraPosition  = prefs.getString("CameraPositionSaved", null);
            prefs.edit().remove("CameraPositionSaved").commit();
            Log.d(TAG, "CameraPosition restored: "+ PreviousCameraPosition);


            //Moves the camera to users current longitude and latitude
            //--------------------------------------------------------
            String[] ParamCamera = PreviousCameraPosition.split(",");

            double LatPosition  = Double.parseDouble(ParamCamera[0].replace("CameraPosition{target=lat/lng: (", ""));
            double LngPosition  = Double.parseDouble(ParamCamera[1].replace(")", ""));
            float  ZoomValue    = Float.parseFloat(ParamCamera[2].replace("zoom=", ""));
            float  tilt         = Float.parseFloat(ParamCamera[3].replace("tilt=", ""));
            float  bearing     =  Float.parseFloat(ParamCamera[4].replace("}", "").replace("bearing=", ""));

            LatLng latlng = new LatLng(LatPosition, LngPosition);

            CameraPosition cam = new CameraPosition(latlng, ZoomValue, tilt, bearing);
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cam));


            //Get event Marks
            //---------------
            //Cancel TaskGetEventMarkFromServer if running
            if (TaskGetEventMarkFromServer != null) {
                TaskGetEventMarkFromServer.cancel(true);
            }

            //Update Event Mark
            TaskGetEventMarkFromServer = new JSONTaskGetEventMark().execute();

        }else{

            //Welcome message
            //---------------
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.WelcomMsg), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            //Moves  camera
            //-------------
            TimerMoveToMyLocation = new Timer();
            TimerMoveToMyLocation.schedule(new MoveToMyLocation(), MoveToMyLocationDelay);
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //           VerifyGpsPermissions               //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    public static void VerifyGpsPermissions(Activity activity) {

        // Check if we have GPS permission

        int GpsPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if ( ! (GpsPermission == PackageManager.PERMISSION_GRANTED)  )
        {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_GPS,
                    PERMISSIONS_GPS_STORAGE
                    );
        }
    }
    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //             MoveToMyLocation                 //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    class MoveToMyLocation extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    //Moves the camera to users current longitude and latitude
                    //--------------------------------------------------------
                    LatLng latlng = new LatLng(AixEnProvenceLat, AixEnProvenceLng);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, (float) DefaultZoom), ZoomAnimationDuration, null); //Animates camera and zooms to preferred state on the user's current location.;

                    //UpdateZoomAfterIntro
                    //--------------------
                    TimerUpdateZoomAfterIntro = new Timer();
                    TimerUpdateZoomAfterIntro.schedule(new UpdateZoomAfterIntro(), ZoomAnimationDuration);

                }
            });
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //             UpdateZoomAfterIntro             //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    class UpdateZoomAfterIntro extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UpdateZoom();
                }
            });
        }
    }


    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                GetEventMark                  //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    class JSONTaskGetEventMark extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (!DiagWaitMsg.isShowing())
                DiagWaitMsg.show();                                                                 //Display Wait if it wasn't displayed
        }

        @Override
        protected String doInBackground(String... strings) {

            int TryCount = 0;

            //Retry Nbhttpretry Time the connexion before leaving
            //-----------------------------------------
            while (TryCount < Nbhttpretry) {

                HttpURLConnection connection = null;
                BufferedReader reader = null;

                try {
                    //Connect and get datas
                    //---------------------
                    URL url;

                    String Id  = "&I="+android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

                    //Connect and get datas
                    //---------------------
                    switch (DisplayedMode){

                        case ModeMorning :  url = new URL(AdrGetMorningEventMark+Id);
                            break;

                        case ModeMoon :     url = new URL(AdrGetNoonEventMark+Id);
                            break;

                        case ModeNight :
                        default:
                            url = new URL(AdrGetNightEventMark+Id);
                            break;
                    }

                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream stream = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";

                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    //JSON Conversion
                    //---------------
                    JSONArray PointsArray = new JSONArray(buffer.toString());

                    //Clear existing Marks
                    //--------------------
                    MarkEventList.clear();

                    //Create GPS datas
                    // ---------------
                    for (int i = 0; i < PointsArray.length(); i++) {

                        //If params Id and Longitude and Latitude exist
                        //--------------------------------------
                        if (  (!PointsArray.getJSONObject(i).isNull("Id")) && (!PointsArray.getJSONObject(i).isNull("L")) && (!PointsArray.getJSONObject(i).isNull("l"))) {

                            MarkerEventListType Point = new MarkerEventListType();
                            double Coordonnees[] = new double[3];

                            Coordonnees[0] =  PointsArray.getJSONObject(i).getInt("Id");
                            Coordonnees[1] =  PointsArray.getJSONObject(i).getDouble("L");
                            Coordonnees[2] =  PointsArray.getJSONObject(i).getDouble("l");

                            Point.Coordonnees   = Coordonnees;
                            Point.Type          =  PointsArray.getJSONObject(i).getString("C");

                            MarkEventList.add(Point);
                        }
                    }

                    return "OK";

                    //Exeptions
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null)
                        connection.disconnect();

                    try {
                        if (reader != null)
                            reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    TryCount++;
                }
            }

            //If connexion have failled we stop
            //---------------------------------
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //Set Flag to indicate Heat list is ready
            if (result != null) {
                UpdateEventMark();
                UpdateZoom();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.FailConnexionMsg), Toast.LENGTH_LONG).show();
            }

            DiagWaitMsg.dismiss();
            TaskGetEventMarkFromServer = null;           //Release ressource
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  GetEvent                    //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    class JSONTaskGetEvent extends AsyncTask<String, String, String> {

        JSONObject      JSON_Event;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (!DiagWaitMsg.isShowing())
                DiagWaitMsg.show();                                                                 //Display Wait if it wasn't displayed
        }

        @Override
        protected String doInBackground(String... strings) {

            int TryCount = 0;

            //Retry Nbhttpretry Time the connexion before leaving
            //-----------------------------------------
            while (TryCount < Nbhttpretry) {

                HttpURLConnection connection = null;
                BufferedReader reader = null;

                try {
                    //Connect and get datas
                    //---------------------
                    String Id  = "&I="+android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    URL url = new URL(strings[0]+Id);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream stream = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";

                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    //JSON Conversion
                    //---------------
                    JSON_Event = new JSONObject(buffer.toString());

                    return "OK";

                    //Exeptions
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null)
                        connection.disconnect();

                    try {
                        if (reader != null)
                            reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    TryCount++;
                }
            }

            //If connexion have failled we stop
            //---------------------------------
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //Set Flag to indicate Heat list is ready
            if (result != null) {

                try {
                    //Create Intent
                    Intent EventIntent = new Intent(getApplicationContext(), EventActivity.class );

                    //Put datas
                    Bundle Eventbundle = new Bundle();


                    Eventbundle.putString("Titre", JSON_Event.getString("Titre"));
                    Eventbundle.putString("Description", JSON_Event.getString("Description"));
                    Eventbundle.putString("Image", JSON_Event.getString("Image"));
                    Eventbundle.putString("Prix", JSON_Event.getString("Prix"));
                    Eventbundle.putString("Auteur", JSON_Event.getString("Auteur"));
                    Eventbundle.putString("Lien", JSON_Event.getString("Lien"));

                    //Get event dates
                    //---------------
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String Horraires = "Erreur";

                    try {
                        Date DateDebut = format.parse(JSON_Event.getString("DateDebut"));
                        Calendar CalendarDateDebut = Calendar.getInstance(); // creates a new calendar instance
                        CalendarDateDebut.setTime(DateDebut);   // assigns calendar to given date

                        Date DateFin = format.parse(JSON_Event.getString("DateFin"));
                        Calendar CalendarDateFin = Calendar.getInstance(); // creates a new calendar instance
                        CalendarDateFin.setTime(DateFin);   // assigns calendar to given date

                        Horraires = String.valueOf(CalendarDateDebut.get(Calendar.HOUR_OF_DAY)) + "h - " + String.valueOf(CalendarDateFin.get(Calendar.HOUR_OF_DAY)) + "h";

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    Eventbundle.putString("Horraires", Horraires);

                    Eventbundle.putString("CameraPositionSaved", mMap.getCameraPosition().toString());
                    Eventbundle.putInt("ModeAffichage", DisplayedMode);

                    EventIntent.putExtras(Eventbundle);

                    //Start it
                    startActivity(EventIntent);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.FailConnexionMsg), Toast.LENGTH_LONG).show();
            }

            DiagWaitMsg.dismiss();
            TaskGetEventFromServer = null;           //Release ressource
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                UpdateEventMark               //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    private final void UpdateEventMark(){
        Marker NewMarker;

        if(mMap != null)
        {
            //Remove previous Marker
            for (int i = 0; i < MarkerList.size(); i++) {
                MarkerList.get(i).remove();
            }


            //Delete Markers list
            MarkerList.clear();
            MarkerIdList.clear();


            //Active On Click Listener
            mMap.setOnMarkerClickListener(MapsActivity.this);


            //Add each Mark
            for (int i = 0; i < MarkEventList.size(); i++) {

                LatLng Gps = new LatLng(MarkEventList.get(i).Coordonnees[2], MarkEventList.get(i).Coordonnees[1]);

                NewMarker = mMap.addMarker(new MarkerOptions()
                        .position(Gps)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icongmap3))
                );

                MarkerList.add(NewMarker);
                MarkerIdList.add((int) (MarkEventList.get(i).Coordonnees[0]));
            }
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                 onMarkerClick                //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    @Override
    public boolean onMarkerClick(Marker marker) {

        //When Marker is clicked
        for(int i=0; i < MarkerList.size();i++) {

            //Found the Marker
            if(MarkerList.get(i).equals(marker))
            {
                //Ask The Event with EventId param
                TaskGetEventFromServer = new JSONTaskGetEvent().execute(AdrUsedForEvent + "?EventId="+MarkerIdList.get(i));
                return true;
            }
        }
        return false;
    }


    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                 UserAddEvent                 //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    public void UserAddEvent() {

        //Create dialog msg
        //-----------------
        AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);

        //Title
        alert.setTitle("Proposez un évènement");

        //Description
        final EditText EventDescription = new EditText(MapsActivity.this);
        EventDescription.setHint("Décris nous en quelques lignes l'évènement à partager : nom, lieu, dates... \n\n L' équipe Circl s'occupe du reste :) !");
        EventDescription.setMinimumHeight(100);
        EventDescription.setGravity(Gravity.LEFT);


        //Pseudo
        final EditText Pseudo = new EditText(MapsActivity.this);
        Pseudo.setHint("Ton prénom ou pseudo");
        Pseudo.setGravity(Gravity.LEFT);

        //Contact
        final EditText Contact = new EditText(MapsActivity.this);
        Contact.setHint("Contact : mail ou tel");
        Contact.setGravity(Gravity.LEFT);

        //Add to dialog msg
        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(EventDescription);
        layout.addView(Pseudo);
        layout.addView(Contact);
        alert.setView(layout);


        // Add the buttons
        //----------------

        //Ok button
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(EventDescription.getText().length() >= 10) {
                    String Msg = "Description :\n\n" + EventDescription.getText() + "\n\n\n\nPseudo :\n\n" + Pseudo.getText() + "\n\n\n\nContact :\n\n" + Contact.getText();
                    UserEventTask = new TaskUserEvent().execute(Msg);
                }else{
                    Toast.makeText(getApplicationContext(), getString(R.string.ErrorDescriptionShare), Toast.LENGTH_LONG).show();
                }
            }
        });


        //Cancel button
        alert.setNegativeButton("Retour", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        //Display
        //-------
        alert.show();

        //Button button = ((AlertDialog) alert).getButton(AlertDialog.BUTTON_POSITIVE);
        //button.setEnabled(false);
    }


    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //              SendUserEvent                   //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    class TaskUserEvent extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {

            int TryCount = 0;

            //Retry Nbhttpretry Time the connexion before leaving
            //-----------------------------------------
            while (TryCount < Nbhttpretry) {

                HttpURLConnection connection = null;
                BufferedReader reader = null;

                try {
                    //Connect and get datas
                    //---------------------
                    String StringUrl = AdrUserAddEvent + "?EventDescription=" + URLEncoder.encode(strings[0], "UTF-8")  + "&I=" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    URL url = new URL(StringUrl);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream stream = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";

                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    //JSON Conversion
                    //---------------
                    JSONObject      JSON_Response;
                    JSON_Response = new JSONObject(buffer.toString());

                    return JSON_Response.getString("Titre");


                    //Exeptions
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null)
                        connection.disconnect();
                    try {
                        if (reader != null)
                            reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    TryCount++;
                }
            }

            return "Error";

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //Toast Msg according to Http exchange
            //------------------------------------
            if (result.contentEquals("Done")) {
                Toast.makeText(getApplicationContext(), "Votre évènement a bien été envoyé", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Une erreur s'est produite, votre message n'a pas été correctement envoyé.\n\nSi cela se reproduit envoyez un texto au : 06 15 49 74 46. Merci", Toast.LENGTH_LONG).show();
            }
        }
    }

}
