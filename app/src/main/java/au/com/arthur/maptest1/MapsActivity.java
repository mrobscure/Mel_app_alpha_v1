package au.com.arthur.maptest1;

import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;


public class MapsActivity extends FragmentActivity
        implements  OnMapReadyCallback,
                    GoogleMap.OnMarkerClickListener,
                    GoogleApiClient.ConnectionCallbacks,
                    GoogleApiClient.OnConnectionFailedListener,
                    LocationListener {


    //class wide variables
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = MapsActivity.class.getSimpleName();
    private Location myLocation;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;

    private SlidingUpPanelLayout slidingLayout;
    private int POIViewed = 0;
    private int toastGuide_clickMarker = 0;
    private int toastGuide_POIFirstView = 0;
    final int TOASTGUIDE_SHOWCOUNT = 2;
    Marker currentMarker;

    private static final String POI_LISTFILE = "poi_rootlist";


    ////////////////////////////////////////
    //
    //  General methods for activity
    //
    ////////////////////////////////////////

    //Activity OnCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set content view
        setContentView(R.layout.activity_maps);

        //maps related
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        buildGoogleApiClient();
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        //load and display POIs
        prepare_POI();

        //slidepanel related
        slidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingLayout.setPanelSlideListener(onSlideListener());
        slidingLayout.setTouchEnabled(false);
        TextView POI_Title = (TextView)findViewById(R.id.POITitleTxt);
        POI_Title.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPOITitleClick();
            }
        });
        ImageView POI_TitleImg = (ImageView)findViewById(R.id.POITitleImg);
        POI_TitleImg.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPOITitleClick();
            }
        });
    }

    //On app resume
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    //build actionbar items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //add handlers for actionbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_home:
                navigateHome();
                return true;
            case R.id.action_myloc:
                navigateCurrentLocation();
                return true;
            case R.id.action_list:
                showListOfPOI();
                return true;
            case R.id.action_info:
                showInfo();
                return true;
            case R.id.action_test_func_1:
                TestFunc1();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //actionbar Test Func 1
    public void TestFunc1()
    {
        LatLng DestLoc = new LatLng(-37.810374, 144.976368);

        final Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?"+
                        "saddr="
                        + myLocation.getLatitude() + "," + myLocation.getLongitude() +
                        "&daddr="
                        + DestLoc.latitude + "," + DestLoc.longitude));
        intent.setClassName("com.google.android.apps.maps",
                "com.google.android.maps.MapsActivity");
        startActivity(intent);

    }

    //actionbar activity
    public void showListOfPOI()
    {
        Toast.makeText(getApplicationContext(), "TBC: List of POIs", Toast.LENGTH_LONG).show();
    }

    //actionbar activity
    public void showInfo()
    {
        Toast.makeText(getApplicationContext(), "TBC: Credits", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (hasFocus)
            {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }


    ////////////////////////////////////////
    //
    //  Google Maps related code
    //
    ////////////////////////////////////////

    //maps onready
    @Override
    public void onMapReady(GoogleMap map) {

        //Load and show POI markers
        prepare_POI();

        //Set camera to home location
        LatLng central = new LatLng(-37.810366, 144.962886);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(central, 14));

        navigateHome();

        //set map listener - On Marker Click
        map.setOnMarkerClickListener(this);

        //set map listener - On Map Click
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        //set map listener - On Camera Change
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                if (POIViewed == 2) {
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    POIViewed = 0;
                }
            }
        });

        //provide a how to Toast on map first show
        Toast toast = Toast.makeText(getApplicationContext(), "Touch Point of Interest marker to view", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    //maps - navigate to central map location over Melbourne CBD
    public void navigateHome()
    {
        LatLng central = new LatLng(-37.810366, 144.962886);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(central, 14));
    }

    //maps - prepare map from resume
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    //actions for On Marker click
    @Override
    public boolean onMarkerClick(final Marker marker)
    {
        //if marker is the user location marker, do not perform usual actions
        if (marker.getTitle().equals("You are here!")) {
            marker.showInfoWindow();
            //Do nothing...
            return(true);
        }

        //Set POI Title Bar from Marker
        TextView POItextView = (TextView)findViewById(R.id.POITitleTxt);
        POItextView.setText(marker.getTitle());

        //Set currently selected marker
        currentMarker = marker;

        //Centre Market in Map
        LatLng latLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        //Make POI Title Bar Visible
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        //Prep variable to determine if POI has been read, we can then hide the POI title bar
        POIViewed = 0;

        //Show a guide Toast to indicate to user to touch the title to view POI
        if (toastGuide_POIFirstView < TOASTGUIDE_SHOWCOUNT)
        {
            Context context = getApplicationContext();
            CharSequence text = "Click below to view Point of Interest";
            int duration = Toast.LENGTH_SHORT;
            if (toastGuide_POIFirstView == 0)
            {duration = Toast.LENGTH_LONG;}
            else if (toastGuide_POIFirstView == 1)
            {duration = Toast.LENGTH_SHORT;}
            Toast toast = Toast.makeText(context, text, duration);
            toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 200);
            toast.show();

            toastGuide_POIFirstView++;
        }

        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (myLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);        }
        else {
            handleNewLocation(myLocation);
        }
        Log.i(TAG, "Location services connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        myLocation = location;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    public void navigateCurrentLocation()
    {
        Marker marker;
        double currentLatitude = myLocation.getLatitude();
        double currentLongitude = myLocation.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("You are here!");
        marker = mMap.addMarker(options);
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        marker.showInfoWindow();
    }



    ////////////////////////////////////////
    //
    //  POI content management code
    //
    ////////////////////////////////////////

    private void prepare_POI()
    {
        try {
            InputStream inputstream;
            XmlPullParserFactory xmlFactoryObject;
            int resID = getResources().getIdentifier(POI_LISTFILE, "raw", this.getPackageName());
            inputstream = this.getResources().openRawResource(resID);
            xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();
            myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            myparser.setInput(inputstream, null);

            try
            {
                String text = null;
                String val_DisplayName = "";
                String val_Icon = "";
                String val_Html = "";
                double val_Coord_Lon = 0;
                double val_Coord_Lat = 0;
                boolean XMLelementMarker = false;
                String name;
                int event = myparser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT)  //while not EOF
                {
                    name = myparser.getName();
                    switch (event)
                    {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.TEXT:
                            text = myparser.getText();
                            break;
                        case XmlPullParser.END_TAG:
                            if (name.equals("DisplayName")) {
                                val_DisplayName = text;
                            }
                            else if (name.equals("Icon")) {
                                val_Icon = text;
                            }
                            else if (name.equals("Html")) {
                                val_Html = text;
                            }
                            else if(name.equals("Coord")) {
                                val_Coord_Lon = Double.parseDouble(myparser.getAttributeValue(null, "lon"));
                                val_Coord_Lat = Double.parseDouble(myparser.getAttributeValue(null, "lat"));
                            }
                            break;
                    }

                    if (name != null) {
                        if (name.equals("POI")) {
                            if (!XMLelementMarker) {
                                XMLelementMarker = true;
                            } else {
                                XMLelementMarker = false;
                                //Add marker to map
                                if (!"".equals(val_DisplayName)) {
                                    LatLng loc = new LatLng(val_Coord_Lat, val_Coord_Lon);
                                    resID = getResources().getIdentifier(val_Icon, "raw", getPackageName());
                                    mMap.addMarker(new MarkerOptions().position(loc).title(val_DisplayName).icon(BitmapDescriptorFactory.fromResource(resID)).snippet(val_Html));
                                }
                            }
                        }
                    }

                    event = myparser.next();
                }

            }
            catch (Exception e) {
                    e.printStackTrace();
            }
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void onPOITitleClick()
    {
        if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED))
        {//Show POI panel

            //Populate the panel with the relevant HTML
            //Get HTML test from file
            String htmlFName = currentMarker.getSnippet();
            int resID = getResources().getIdentifier(htmlFName, "raw", getPackageName());
            String htmlText = readRawTextFile(getApplicationContext(), resID);
            //Load HTML into TextView
            TextView htmlTextView = (TextView)findViewById(R.id.main_text);
            htmlTextView.setText(Html.fromHtml(htmlText, new ImageGetter(), null));

            //Scroll view to top
            ScrollView scrollView = (ScrollView)findViewById(R.id.main_scroll);
            scrollView.smoothScrollTo(0, 0);

            //hide actionbar
            getActionBar().hide();

            //Show arrow as hint
            ImageView imageView = (ImageView)findViewById(R.id.POITitleImg);
            resID = getResources().getIdentifier("ic_chevron_left_white_24dp", "drawable", getPackageName());
            imageView.setImageResource(resID);

            // Execute some code after load has completed, rough but it works
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    //slide up panel
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
            }, 100);

            //show guide message for POI slider
            if (toastGuide_clickMarker < TOASTGUIDE_SHOWCOUNT)
            {
                Context context = getApplicationContext();
                CharSequence text = "Touch title to return to map";
                int duration = Toast.LENGTH_SHORT;
                if (toastGuide_clickMarker == 0)
                {duration = Toast.LENGTH_LONG;}
                else if (toastGuide_clickMarker == 1)
                {duration = Toast.LENGTH_SHORT;}
                Toast toast = Toast.makeText(context, text, duration);
                toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 200);
                toast.show();

                toastGuide_clickMarker++;
            }
        }
        else if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.EXPANDED))
        {//close POI panel

            //slide down panel
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

            //show actionbar
            getActionBar().show();

            //Show chevron as hint
            ImageView imageView = (ImageView)findViewById(R.id.POITitleImg);
            int resID = getResources().getIdentifier("ic_expand_less_white_24dp", "drawable", getPackageName());
            imageView.setImageResource(resID);

        }
    }

    //Load HTML file for POI
    public static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    //Find HTML image by name and return a drawable object - inserts images into POI display
    private class ImageGetter implements Html.ImageGetter {

        public Drawable getDrawable(String source)
        {
            String imageName = source.substring(0, source.indexOf('.'));
            int resID = getResources().getIdentifier(imageName, "raw", getPackageName());

            Drawable d = getResources().getDrawable(resID);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            return d;
        }
    }


    ////////////////////////////////////////
    //
    //  POI slider code
    //
    ////////////////////////////////////////

    private SlidingUpPanelLayout.PanelSlideListener onSlideListener() {
        return new SlidingUpPanelLayout.PanelSlideListener() {

            @Override
            public void onPanelCollapsed(View view) {
                if (POIViewed == 1)
                {
                    POIViewed = 2;
                }
            }

            @Override
            public void onPanelExpanded(View view)
            {
                if (POIViewed == 0)
                {
                    POIViewed = 1;
                }
            }

            @Override
            public void onPanelSlide(View view, float v)
            { }  //available if needed

            @Override
            public void onPanelAnchored(View view)
            { }  //available if needed

            @Override
            public void onPanelHidden(View view)
            { }  //available if needed
        };
    }
}
