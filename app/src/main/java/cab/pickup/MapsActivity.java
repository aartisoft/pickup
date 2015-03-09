package cab.pickup;

import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cab.pickup.util.IOUtil;
import cab.pickup.util.Journey;

public class MapsActivity extends MyActivity {
    GoogleMap map; // Might be null if Google Play services APK is not available.

    Polyline currPath;

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    public void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        map.setMyLocationEnabled(true);
        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((start.latitude + end.latitude) / 2, (start.longitude + end.longitude) / 2), 12));
    }

    class MapDirectionsTask extends AsyncTask<String, Integer, String> {
        public final static String MODE_DRIVING = "driving";
        public final static String MODE_WALKING = "walking";

        static final String TAG = "Directions";

        public MapDirectionsTask() {
        }

        @Override
        protected String doInBackground(String... params) {
            String json=null;
            String url = params[0];

            HttpResponse response;
            HttpGet request;
            AndroidHttpClient client = AndroidHttpClient.newInstance("somename");

            try {
                request = new HttpGet(url);
                response = client.execute(request);

                json = IOUtil.buildStringFromIS(response.getEntity().getContent());


            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            client.close();

            return json;
        }

        @Override
        public void onPostExecute(String json) {
            try {
                Journey j = new Journey(new JSONObject(json), me);

                addPath(j.getPath(), j.getLatLngBounds());
            } catch (JSONException e) {
                Log.e(TAG, "JSON error: "+e.getMessage());

                Toast.makeText(getApplicationContext(), "Error while loading path!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void addPath(ArrayList<LatLng> directions, LatLngBounds bnds){
        if(currPath!=null) currPath.remove();

        PolylineOptions rectLine = new PolylineOptions().width(4).color(Color.BLUE);

        for(int i = 0 ; i < directions.size() ; i++) {
            rectLine.add(directions.get(i));
        }

        currPath=map.addPolyline(rectLine);

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bnds, 10));
    }
}
