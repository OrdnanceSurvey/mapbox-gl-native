package com.mapbox.mapboxsdk.testapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.mapbox.mapboxsdk.MapFragment;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngZoom;
import com.mapbox.mapboxsdk.layers.CustomLayer;
import com.mapbox.mapboxsdk.testapp.layers.ExampleCustomLayer;
import com.mapbox.mapboxsdk.testapp.utils.GeoParseUtil;
import com.mapbox.mapboxsdk.utils.ApiAccess;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapboxMap;
import com.mapbox.mapboxsdk.views.OnMapReadyCallback;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //
    // Static members
    //

    // Used for saving instance state
    private static final String STATE_IS_ANNOTATIONS_ON = "isAnnotationsOn";
    private static final String STATE_SELECTED_STYLE = "selectedStyle";
    private static final String STATE_MARKER_LIST = "markerList";

    // Used for permissions requests
    private static final int PERMISSIONS_LOCATION = 0;
    private static final int PERMISSIONS_TRACKING_MODE_ACTIVITY = 1;

    // Used for info window
    private static final DecimalFormat LAT_LON_FORMATTER = new DecimalFormat("#.#####");

    //
    // Instance members
    //

    // Used for the UI
    private DrawerLayout mDrawerLayout;
    private TextView mFpsTextView;
    private int mSelectedStyle = R.id.actionStyleMapboxStreets;
    private NavigationView mNavigationView;
    private CoordinatorLayout mCoordinatorLayout;
    private boolean mIsShowingCustomLayer;

    // MapboxMap
    private MapboxMap mMapboxMap;

    // Used for GPS
    private FloatingActionButton mLocationFAB;

    // Used for Annotations
    private boolean mIsAnnotationsOn = false;
    private ArrayList<MarkerOptions> mMarkerList = new ArrayList<>();

    //
    // Lifecycle events
    //

    // Called when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the layout
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_menu_24dp);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            setupDrawerContent(mNavigationView);
        }

        mFpsTextView = (TextView) findViewById(R.id.view_fps);
        mFpsTextView.setText("");

        mLocationFAB = (FloatingActionButton) findViewById(R.id.locationFAB);
        mLocationFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle GPS position updates
                toggleGps(!mMapboxMap.isMyLocationEnabled());
            }
        });

        // Restore saved state
        if (savedInstanceState != null) {
            mIsAnnotationsOn = savedInstanceState.getBoolean(STATE_IS_ANNOTATIONS_ON);
            mSelectedStyle = savedInstanceState.getInt(STATE_SELECTED_STYLE);
            mMarkerList = savedInstanceState.getParcelableArrayList(STATE_MARKER_LIST);
        }else{
            mMarkerList = new ArrayList<>();
        }

        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                mMapboxMap = mapboxMap;

                mapboxMap.setOnFpsChangedListener(new MyOnFpsChangedListener());

                mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        MarkerOptions marker = new MarkerOptions()
                                .position(point)
                                .title("Dropped Pin")
                                .snippet(LAT_LON_FORMATTER.format(point.getLatitude()) + ", " +
                                        LAT_LON_FORMATTER.format(point.getLongitude()))
                                .icon(null);

                        mMarkerList.add(marker);
                        mapboxMap.addMarker(marker);
                    }
                });

                mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        String location = LAT_LON_FORMATTER.format(point.getLatitude()) + ", " +
                                LAT_LON_FORMATTER.format(point.getLongitude());
                        Snackbar.make(mCoordinatorLayout, "Map Click Listener " + location, Snackbar.LENGTH_SHORT).show();
                    }
                });

                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        Snackbar.make(mCoordinatorLayout, "Marker Click Listener for " + marker.getTitle(), Snackbar.LENGTH_SHORT).show();
                        return false;
                    }
                });

                mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        Snackbar.make(mCoordinatorLayout, "InfoWindow Click Listener for " + marker.getTitle(), Snackbar.LENGTH_SHORT).show();
                        marker.hideInfoWindow();
                        return true;
                    }
                });

                mMapboxMap.addMarkers(mMarkerList);
                setDefaultState();
            }
        });
    }

    private void setDefaultState(){
        mNavigationView.getMenu().findItem(R.id.action_compass).setChecked(mMapboxMap.isCompassEnabled());
        mNavigationView.getMenu().findItem(R.id.action_debug).setChecked(mMapboxMap.isDebugActive());
        mNavigationView.getMenu().findItem(R.id.action_markers).setChecked(mIsAnnotationsOn);
        changeMapStyle(mSelectedStyle);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // We need to recheck permissions in case user revoked them via settings app
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)) {
            toggleGps(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_ANNOTATIONS_ON, mIsAnnotationsOn);
        outState.putInt(STATE_SELECTED_STYLE, mSelectedStyle);
        outState.putParcelableArrayList(STATE_MARKER_LIST, mMarkerList);
    }

    //
    // Other events
    //

    // Called when pressing action bar items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleGps(true);
                }
                break;

            case PERMISSIONS_TRACKING_MODE_ACTIVITY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(getApplicationContext(), MyLocationTrackingModeActivity.class));
                }
                break;
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();

                        // Respond To Selection
                        switch (menuItem.getItemId()) {

                            case R.id.action_debug:
                                // Cycle map debug options
                                mMapboxMap.cycleDebugOptions();
                                toggleFpsCounter(mMapboxMap.isDebugActive());
                                return true;

                            case R.id.action_markers:
                                // Toggle markers
                                toggleAnnotations(!mIsAnnotationsOn);
                                return true;

                            case R.id.action_compass:
                                // Toggle compass
                                mMapboxMap.setCompassEnabled(!mMapboxMap.isCompassEnabled());
                                return true;

                            case R.id.action_info_window_adapter:
                                startActivity(new Intent(getApplicationContext(), InfoWindowAdapterActivity.class));
                                return true;

                            case R.id.action_camera:
                                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                                return true;

                            case R.id.action_tilt:
                                startActivity(new Intent(getApplicationContext(), TiltActivity.class));
                                return true;

                            case R.id.action_map_fragment:
                                startActivity(new Intent(getApplicationContext(), MapFragmentActivity.class));
                                return true;

                            case R.id.action_press_for_marker:
                                startActivity(new Intent(getApplicationContext(), PressForMarkerActivity.class));
                                return true;

                            case R.id.action_coordinate_change:
                                startActivity(new Intent(getApplicationContext(), CoordinateChangeActivity.class));
                                return true;

                            case R.id.action_bulk_markers:
                                startActivity(new Intent(getApplicationContext(), BulkMarkerActivity.class));
                                return true;

                            case R.id.action_info_window:
                                startActivity(new Intent(getApplicationContext(), InfoWindowActivity.class));
                                return true;

                            case R.id.action_info_window_concurrent:
                                startActivity(new Intent(getApplicationContext(), InfoWindowConcurrentActivity.class));
                                return true;

                            case R.id.action_visible_bounds:
                                startActivity(new Intent(getApplicationContext(), VisibleCoordinateBoundsActivity.class));
                                return true;

                            case R.id.action_user_tracking_mode:
                                if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) ||
                                        (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                                != PackageManager.PERMISSION_GRANTED)) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                            PERMISSIONS_TRACKING_MODE_ACTIVITY);
                                } else {
                                    startActivity(new Intent(getApplicationContext(), MyLocationTrackingModeActivity.class));
                                }
                                return true;

                            case R.id.action_polyline:
                                startActivity(new Intent(getApplicationContext(), PolylineActivity.class));
                                return true;

                            case R.id.action_custom_layer:
                                if (mIsShowingCustomLayer) {
                                    removeCustomLayer();
                                } else {
                                    addCustomLayer();
                                }
                                return true;

                            default:
                                return changeMapStyle(menuItem.getItemId());
                        }
                    }
                });
    }

    private void toggleFpsCounter(boolean enableFps) {
        // Show the FPS counter
        if (enableFps) {
            mFpsTextView.setVisibility(View.VISIBLE);
            mFpsTextView.setText(getResources().getString(R.string.label_fps));
        } else {
            mFpsTextView.setVisibility(View.INVISIBLE);
        }
    }

    private boolean changeMapStyle(int id) {
        switch (id) {
            case R.id.actionStyleMapboxStreets:
                mMapboxMap.setStyle(Style.MAPBOX_STREETS);
                mSelectedStyle = id;
                return true;

            case R.id.actionStyleEmerald:
                mMapboxMap.setStyle(Style.EMERALD);
                mSelectedStyle = id;
                return true;

            case R.id.actionStyleLight:
                mMapboxMap.setStyle(Style.LIGHT);
                mSelectedStyle = id;
                return true;

            case R.id.actionStyleDark:
                mMapboxMap.setStyle(Style.DARK);
                mSelectedStyle = id;
                return true;

            case R.id.actionStyleSatellite:
                mMapboxMap.setStyle(Style.SATELLITE);
                mSelectedStyle = id;
                return true;

            case R.id.actionStyleSatelliteStreets:
                mMapboxMap.setStyle(Style.SATELLITE_STREETS);
                mSelectedStyle = id;
                return true;

            default:
                return false;
        }
    }

    /**
     * Enabled / Disable GPS location updates along with updating the UI
     *
     * @param enableGps true if GPS is to be enabled, false if GPS is to be disabled
     */
    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_LOCATION);
            } else {
                mMapboxMap.setOnMyLocationChangeListener(new MapboxMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(@Nullable Location location) {
                        if (location != null) {
                            mMapboxMap.setZoom(16);
                            mMapboxMap.setLatLng(new LatLng(location));
                            mMapboxMap.setOnMyLocationChangeListener(null);
                        }
                    }
                });
                mMapboxMap.setMyLocationEnabled(true);
                mMapboxMap.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
                mMapboxMap.setMyBearingTrackingMode(MyBearingTracking.GPS);
                mLocationFAB.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            }
        } else {
            mMapboxMap.setMyLocationEnabled(false);
            mLocationFAB.setColorFilter(Color.TRANSPARENT);
        }
    }

    /**
     * Enable / Disable Annotations.
     *
     * @param enableAnnotations True to display, False to hide
     */
    private void toggleAnnotations(boolean enableAnnotations) {
        if (enableAnnotations) {
            if (!mIsAnnotationsOn) {
                mIsAnnotationsOn = true;
                addMarkers();
                addPolyline();
                addPolygon();
                mMapboxMap.setZoom(7);
                mMapboxMap.setLatLng(new LatLng(38.11727, -122.22839));
            }
        } else {
            if (mIsAnnotationsOn) {
                mIsAnnotationsOn = false;
                removeAnnotations();
            }
        }
    }

    private void addMarkers() {
        List<MarkerOptions> markerOptionsList = new ArrayList<>();

        final MarkerOptions backLot = generateMarker("Back Lot", "The back lot behind my house", null, 38.649441, -121.369064);
        markerOptionsList.add(backLot);

        final Icon dogIcon = mMapboxMap.getIconFactory().fromAsset("dog-park-24.png");
        final MarkerOptions cheeseRoom = generateMarker("Cheese Room", "The only air conditioned room on the property", dogIcon, 38.531577, -122.010646);
        markerOptionsList.add(cheeseRoom);

        mMapboxMap.addMarkers(markerOptionsList);
    }

    private MarkerOptions generateMarker(String title, String snippet, Icon icon, double lat, double lng) {
        return new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title)
                .icon(icon)
                .snippet(snippet);
    }

    private void addPolyline() {
        try {
            String geojsonStr = GeoParseUtil.loadStringFromAssets(this, "small_line.geojson");
            List<LatLng> latLngs = GeoParseUtil.parseGeoJSONCoordinates(geojsonStr);
            mMapboxMap.addPolyline(new PolylineOptions()
                    .add(latLngs.toArray(new LatLng[latLngs.size()]))
                    .width(2)
                    .color(Color.RED));
        } catch (Exception e) {
            Log.e(TAG, "Error adding Polyline: " + e);
            e.printStackTrace();
        }
    }

    private void addPolygon() {
        try {
            String geojsonStr = GeoParseUtil.loadStringFromAssets(this, "small_poly.geojson");
            List<LatLng> latLngs = GeoParseUtil.parseGeoJSONCoordinates(geojsonStr);
            ArrayList<PolygonOptions> opts = new ArrayList<>();
            opts.add(new PolygonOptions()
                    .add(latLngs.toArray(new LatLng[latLngs.size()]))
                    .strokeColor(Color.MAGENTA)
                    .fillColor(Color.BLUE).alpha(0.5f));
            mMapboxMap.addPolygons(opts).get(0);
        } catch (Exception e) {
            Log.e(TAG, "Error adding Polygon: " + e);
            e.printStackTrace();
        }
    }

    private void removeAnnotations() {
        mMarkerList.clear();
        mMapboxMap.removeAllAnnotations();
    }

    private void addCustomLayer() {
        mIsShowingCustomLayer = true;
        mMapboxMap.addCustomLayer(
            new CustomLayer("custom",
                ExampleCustomLayer.createContext(),
                ExampleCustomLayer.InitializeFunction,
                ExampleCustomLayer.RenderFunction,
                ExampleCustomLayer.DeinitializeFunction),
            null);
    }

    private void removeCustomLayer() {
        mIsShowingCustomLayer = false;
        mMapboxMap.removeCustomLayer("custom");
    }

    // Called when FPS changes
    private class MyOnFpsChangedListener implements MapboxMap.OnFpsChangedListener {

        @Override
        public void onFpsChanged(double fps) {
            mFpsTextView.setText(getResources().getString(R.string.label_fps) + String.format(" %4.2f", fps));
        }
    }

}
