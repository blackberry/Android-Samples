/* Copyright (c) 2011-2016 BlackBerry Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package blackberry.googleplayservicestest;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * checks weather google maps services are available or not.
 * It displays error dialog in case it is not present.
 */
public class MapsActivity extends FragmentActivity {

    private static final String TAG = "MapActivity";
    public static final int errorCode = 10;
    public static final double LAT = 43.4667;
    public static final double LNG = -80.5167;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMap();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Getting the status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if (status == ConnectionResult.SUCCESS)
            Toast.makeText(this, R.string.play_services_success, Toast.LENGTH_LONG).show();
        else {
            Log.i(TAG, "Google Play Services are not available");
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, errorCode);
            dialog.show();
        }
        setUpMap();
    }

    /**
     * checks if the map has been already instantiated. If not, it obtains
     * the map from supportFragment.
     */
    private void setUpMap() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                animateMap();
            }
        }
    }

    /**
     * animates the map to the given position of latitude and longitude.
     * This should be called only after verifying that {@link #mMap} is not null.
     */
    private void animateMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(LAT, LNG)).title("Waterloo"));
        CameraUpdate center =
                CameraUpdateFactory.newLatLng(new LatLng(LAT, LNG));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(5);

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);
    }
}
