package cab.pickup.driver.util;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import android.location.Location;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import cab.pickup.common.Constants;
import cab.pickup.common.api.Driver;
import cab.pickup.common.server.PostTask;
import cab.pickup.common.server.Result;
import cab.pickup.common.util.LocationTracker;
import cab.pickup.driver.R;

/**
 * Created by prateek on 10/8/15.
 */
public class DriverTracker extends LocationTracker {

    public static DriverTracker instance = null;
    Driver d=null;
    SharedPreferences prefs;
    public void onCreate()
    {
        super.onCreate();
        connect();
        prefs=getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        if(d==null){
                            try {
                                d = new Driver(new JSONObject(prefs.getString("driver_json","")));
                            }catch (Exception E){
                                E.printStackTrace();
                            }
                        }
                        Location loc = getLocation();
                        if(loc==null || d == null)
                            Log.d("TIMEDTASK", "YO "+ (new Timestamp(new Date().getTime())));
                        else{
                            final String pos = String.valueOf(loc.getLatitude())+","+String.valueOf(loc.getLongitude());
                            new PostTask(DriverTracker.this) {
                                @Override
                                public List<NameValuePair> getPostData(String[] params, int i) {
                                    List<NameValuePair> nameValuePairs = new ArrayList<>();
                                    nameValuePairs.add(new BasicNameValuePair("position",pos));
                                    nameValuePairs.add(new BasicNameValuePair("key", Constants.KEY));

                                    return nameValuePairs;
                                }

                                @Override  public void onPostExecute(Result ret){
                                    super.onPostExecute(ret);
                                    Log.d("TIMEDTASK", "YO " + " D " + d.driver_id + "  :  " + pos + " : " + (new Timestamp(new Date().getTime()))+ " : "+ret.statusMessage+ " : " + String.valueOf(ret.statusCode));
                                }
                            }.execute(Constants.getUrl("/driver_modify_location/"+d.driver_id));
                        }
                    }
                }, 0, 3, TimeUnit.MINUTES);

        instance = this;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

}