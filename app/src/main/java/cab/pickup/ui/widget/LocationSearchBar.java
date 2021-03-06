package cab.pickup.ui.widget;


import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cab.pickup.R;
import cab.pickup.common.api.Location;
import cab.pickup.ui.activity.MyActivity;
import cab.pickup.common.util.LocationTracker;


public class LocationSearchBar extends TextView implements View.OnClickListener{

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String API_KEY = "AIzaSyChiVpPeOyYNFGq7_aR6-zpHnv6HsnwXQo";
    private static final String LOG_TAG = "PICKUP_LOCATION";

    private Location address;
    LocationTracker tracker;
    MyActivity context;
    OnAddressSelectedListener addrListener;

    boolean myLocationEnabled=true, homeOfficeEnabled=true;

    public LocationSearchBar(Context context) {
        super(context,null,R.style.LocationSearchBar);

        init(context);
    }

    public LocationSearchBar(Context context, AttributeSet attrs) {
        super(context, attrs, R.style.LocationSearchBar);

        init(context);
    }

    private void init(Context context){
        setOnClickListener(this);

        if(context instanceof MyActivity) {
            this.context = (MyActivity) context;
            tracker = this.context.getLocationTracker();
        }
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener lstn){
        addrListener=lstn;
    }

    public void setMyLocationEnabled(boolean myLocationEnabled) {
        this.myLocationEnabled = myLocationEnabled;
    }

    public void setHomeOfficeEnabled(boolean homeOfficeEnabled) {
        this.homeOfficeEnabled = homeOfficeEnabled;
    }

    @Override
    public void onClick(View v) {
        LocationSearchDialog dialog = new LocationSearchDialog();

        dialog.show();
    }

    public void setAddress(Location address) {
        this.address = address;

        if(address!=null){
            setText(address.shortDescription);
            if(addrListener!=null) addrListener.onAddressSelected(this, address);
        } else {
            setText("");
        }
    }

    public Location getAddress() {
        return address;
    }

    class LocationSearchDialog extends Dialog implements View.OnClickListener{
        private static final String TAG = "LocationSearchDialog";
        EditText searchField;
        ListView list;

        boolean running, doAgain;

        PlacesAdapter adapter;
        private SearchTask searchTask;

        public LocationSearchDialog() {
            super(context, R.style.LocationSearchDialog);
        }

        @Override
        public void onCreate(Bundle savedInstanceState){
            //requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.widget_location_search_dialog);

            searchField = (EditText)findViewById(R.id.location_search_edittext);
            if(searchField.requestFocus()) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            list = (ListView) findViewById(R.id.location_search_list);

            adapter=new PlacesAdapter(context);

            list.setAdapter(adapter);

            list.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    setAddress((Location) view.getTag());
                    dismiss();
                }
            });

            findViewById(R.id.location_search_my_location_label).setOnClickListener(this);

            if(myLocationEnabled) {
                findViewById(R.id.location_search_my_location_label).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.location_search_my_location_label).setVisibility(View.GONE);
            }

            if (homeOfficeEnabled){
                findViewById(R.id.location_search_home_icon).setVisibility(View.VISIBLE);
                findViewById(R.id.location_search_home_main_text).setVisibility(View.VISIBLE);
                findViewById(R.id.location_search_home_sub_text).setVisibility(View.VISIBLE);
                findViewById(R.id.location_search_office_icon).setVisibility(View.VISIBLE);
                findViewById(R.id.location_search_office_main_text).setVisibility(View.VISIBLE);
                findViewById(R.id.location_search_office_sub_text).setVisibility(View.VISIBLE);

                findViewById(R.id.location_search_fav_label).setVisibility(VISIBLE);
            } else {
                findViewById(R.id.location_search_home_icon).setVisibility(View.INVISIBLE);
                findViewById(R.id.location_search_home_main_text).setVisibility(View.INVISIBLE);
                findViewById(R.id.location_search_home_sub_text).setVisibility(View.INVISIBLE);
                findViewById(R.id.location_search_office_icon).setVisibility(View.INVISIBLE);
                findViewById(R.id.location_search_office_main_text).setVisibility(View.INVISIBLE);
                findViewById(R.id.location_search_office_sub_text).setVisibility(View.INVISIBLE);

                findViewById(R.id.location_search_fav_label).setVisibility(INVISIBLE);
            }


            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!running) {
                        searchTask = new SearchTask();
                        searchTask.execute(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().getAttributes().x=0;
            getWindow().getAttributes().y=0;
        }

        @Override
        public void onClick(View v) {

            tracker = context.getLocationTracker();

            if(tracker==null)
                return;

            switch(v.getId()){
                case R.id.location_search_my_location_label:
                    if(tracker.getLastKnownLocation()==null){
                        Toast.makeText(context, "Unable to get current location. Please check your GPS", Toast.LENGTH_LONG).show();
                        break;
                    }
                    String addressText="MyLocation";
                    try {
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        List<Address> addresses  = geocoder.getFromLocation(tracker.getLatitude(),tracker.getLongitude(), 1);
                        int numLines = addresses.get(0).getMaxAddressLineIndex();
                        addressText="";
                        for (int i = 0;i<numLines-1;i++) {
                            if(i!=0)
                                addressText+=", ";
                            addressText += addresses.get(0).getAddressLine(i);
                        }
                    }catch (Exception E){
                        E.printStackTrace();
                    }
                    setAddress(new Location(tracker.getLatitude(),tracker.getLongitude(),addressText));
                    dismiss();
                    break;
            }
        }


        @Override
        public void onStop()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        {
            if (searchTask!=null) searchTask.cancel(true);
            super.onStop();
        }

        final class SearchTask extends AsyncTask<String, Integer,  List<Location>>{
            @Override
            protected void onPreExecute(){
                running = true;
            }

            @Override
            protected List<Location> doInBackground(String... params) {

                return searchFromString(params[0]);
            }

            @Override
            protected void onPostExecute(List<Location> arr){
                running = false;

                adapter.clear();

                if(arr!=null) {
                    for (Location a : arr) {
                        adapter.add(a);
                    }
                } else {
                    Toast.makeText(context, "No results!", Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();

                if(doAgain){
                    //searchTask.execute(getText().toString());
                    doAgain=false;
                }
            }
        }
    }

    public interface OnAddressSelectedListener{
        public void onAddressSelected(LocationSearchBar bar, Location a);
    }

    List<Location> searchFromString(String input){
        List<Location> resultList = new ArrayList();
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();

        // Checking validity of input search string
        if(input == null || input.length()==0)
            return  resultList;

        tracker = context.getLocationTracker();
        String locString ="";
        if(tracker!=null){
            if(tracker.getLastKnownLocation()!=null){
                locString=String.valueOf(tracker.getLatitude())+","+String.valueOf(tracker.getLongitude());
            }
        }

        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:in");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
            if(!locString.equals(""))
            {
                sb.append("&location="+locString);
                sb.append("&radius=50");
            }
            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
            // Extract the Place descriptions from the results
            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(new Location(predsJsonArray.getJSONObject(i).getString("place_id"), predsJsonArray.getJSONObject(i).getString("description")));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;

    }

};
