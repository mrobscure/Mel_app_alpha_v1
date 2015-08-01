package au.com.arthur.maptest1;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    //class wide variables
    private GoogleMap mMap;
    private SlidingUpPanelLayout slidingLayout;
    private int POIViewed = 0;
    private int toastGuide_clickMarker = 0;
    private int toastGuide_POIFirstView = 0;
    final int TOASTGUIDE_SHOWCOUNT = 2;


    ////////////////////////////////////////
    //
    //  General methods for activity
    //
    ////////////////////////////////////////

    //oncreate for activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set content view
        setContentView(R.layout.activity_maps);

        //maps related
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //slidepanel realted
        slidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingLayout.setPanelSlideListener(onSlideListener());
        slidingLayout.setTouchEnabled(false);

        //For text content - move to relevant location soon
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        String htmlText = readRawTextFile(getApplicationContext(), R.raw.poi_vicmarket);
        TextView htmlTextView = (TextView)findViewById(R.id.main_text);
        htmlTextView.setText(Html.fromHtml(htmlText, new ImageGetter(), null));
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        //For POI title click scroll
        TextView POI_Title = (TextView)findViewById(R.id.poi_title);
        POI_Title.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED))
                    {//Show POI panel

                        //hide actionbar
                        getActionBar().hide();

                        //slide up panel
                        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                        //show guide message for POI slider
                        if (toastGuide_clickMarker < TOASTGUIDE_SHOWCOUNT)
                        {
                            Context context = getApplicationContext();
                            CharSequence text = "Touch title to return to map";
                            int duration = Toast.LENGTH_SHORT;;
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
                    }
            }
        });
   }

    //On app resume
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
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
            case R.id.action_list:
                showListOfPOI();
                return true;
            case R.id.action_info:
                showInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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


    ////////////////////////////////////////
    //
    //  Google Maps related code
    //
    ////////////////////////////////////////

    //maps onready
    @Override
    public void onMapReady(GoogleMap map) {

        //Load and show POI markers
        mapAddMarkers(map);
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

    //maps - load and show POI markers
    private void mapAddMarkers(GoogleMap map)
    {
        // Create marker locations - load from file
        LatLng townhall = new LatLng(-37.8150496,144.9667106);
        LatLng VicMarket = new LatLng(-37.809181, 144.956950);
        LatLng RylExBldg = new LatLng(-37.804843, 144.972271);
        LatLng FlindersSt = new LatLng(-37.818235,144.9676667);
        LatLng StPats = new LatLng(-37.810374, 144.976368);
        LatLng central = new LatLng(-37.810366, 144.962886);
        LatLng Parliament = new LatLng(-37.810794, 144.973687);
        LatLng SouthBank = new LatLng(-37.8185494,144.9654183);

        //Add Markers to Map - load from file
        map.addMarker(new MarkerOptions().position(townhall).title("Melbourne Town Hall").icon(BitmapDescriptorFactory.fromResource(R.drawable.townhall)));
        map.addMarker(new MarkerOptions().position(RylExBldg).title("Royal Exhibition Building").icon(BitmapDescriptorFactory.fromResource(R.drawable.exhibition)));
        map.addMarker(new MarkerOptions().position(VicMarket).title("Queen Victoria Market").icon(BitmapDescriptorFactory.fromResource(R.drawable.queenvic)));
        map.addMarker(new MarkerOptions().position(StPats).title("St Patricks Cathedral").icon(BitmapDescriptorFactory.fromResource(R.drawable.stpauls)));
        map.addMarker(new MarkerOptions().position(FlindersSt).title("Flinders St Station").icon(BitmapDescriptorFactory.fromResource(R.drawable.flinders)));
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
        //Set POI Title Bar from Marker
        TextView POItextView = (TextView)findViewById(R.id.poi_title);
        POItextView.setText(marker.getTitle());

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
            int duration = Toast.LENGTH_SHORT;;
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


    ////////////////////////////////////////
    //
    //  POI content management code
    //
    ////////////////////////////////////////

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

        public Drawable getDrawable(String source) {
            int id;
            if (source.equals("market1.jpg")) {
                id = R.drawable.market1;
            } else if (source.equals("market2.jpg")) {
                id = R.drawable.market2;
            } else if (source.equals("market3.jpg")) {
                id = R.drawable.market3;
            } else {
                return null;
            }

            Drawable d = getResources().getDrawable(id);
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
