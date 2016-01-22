package com.mapbox.mapboxsdk.maps;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.R;
import com.mapbox.mapboxsdk.utils.ApiAccess;
import com.mapbox.mapboxsdk.maps.MapView;

/**
 * Fragment wrapper around a {@link MapView}.
 * <p>
 * A Map component in an app. This fragment is the simplest way to place a map in an application.
 * It's a wrapper around a view of a map to automatically handle the necessary life cycle needs.
 * Being a fragment, this component can be added to an activity's layout or can dynamically be added
 * using a FragmentManager.
 * </p>
 * <p>
 * To get a reference to the MapView, use {@link #getMapAsync(OnMapReadyCallback)}}
 * </p>
 *
 * @see #getMapAsync(OnMapReadyCallback)
 */
public class MapFragment extends Fragment {

    //
    // Static members
    //

    // Tag used for logging
    private static final String TAG = "MapFragment";

    // Argument used for configuration
    private static final String ARGS_MAPBOXMAP_OPTIONS = "MapboxMapOptions";

    //
    // Instance members
    //

    // The map
    private MapView mMap;
    private OnMapReadyCallback mMapReadyCallback;

    public MapFragment newInstance(){
        return new MapFragment();
    }

    public MapFragment newInstance(MapboxMapOptions mapboxMapOptions) {
        final MapFragment mapFragment = new MapFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_MAPBOXMAP_OPTIONS, mapboxMapOptions);
        mapFragment.setArguments(bundle);
        return mapFragment;
    }

    //
    // Lifecycle events
    //

    // Called when the fragment is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.v(TAG, "onCreateView");

        // Create the map
        mMap = (MapView) inflater.inflate(R.layout.fragment_mapview, container, false);

        // Set accessToken
        mMap.setAccessToken(ApiAccess.getToken(inflater.getContext()));

        // Need to pass on any saved state to the map
        mMap.onCreate(savedInstanceState);

        // Return the map as the root view
        return mMap;
    }

    // Called when the fragment is destroyed
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.v(TAG, "onDestroyView");

        // Need to pass on to view
        mMap.onDestroy();
        mMap = null;
    }

    // Called when the fragment is visible
    @Override
    public void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");

        // Need to pass on to view
        mMap.onStart();
    }

    // Called when the fragment is invisible
    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");

        // Need to pass on to view
        mMap.onStop();
    }

    // Called when the fragment is in the background
    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");

        // Need to pass on to view
        mMap.onPause();
    }

    // Called when the fragment is no longer in the background
    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");

        // Need to pass on to view
        mMap.onResume();

        if(mMapReadyCallback!=null){
            mMapReadyCallback.onMapReady(mMap.getMapboxMap());
        }
    }

    // Called before fragment is destroyed
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState");

        // Need to retrieve any saved state from the map
        mMap.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        Log.v(TAG, "OnLowMemory");

        // Need to pass on to view
        mMap.onLowMemory();
        super.onLowMemory();
    }

    //
    // Property methods
    //

    @NonNull
    public void getMapAsync(@NonNull OnMapReadyCallback onMapReadyCallback){
        mMapReadyCallback = onMapReadyCallback;
    }

}
