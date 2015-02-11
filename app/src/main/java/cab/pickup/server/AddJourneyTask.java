package cab.pickup.server;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cab.pickup.MyActivity;

public class AddJourneyTask extends AsyncTask<String, Integer, String> {
    private static final String TAG = "SendMessageTask";
    MyActivity context;

    @Override
    protected String doInBackground(String... params) {
        String url = params[0],
                user_id = params[1],
                access_key = params[2];

        AndroidHttpClient httpclient = AndroidHttpClient.newInstance(TAG);
        HttpPost httppost = new HttpPost(url);
        int statusCode = 0;

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

            nameValuePairs.add(new BasicNameValuePair("user_id", user_id));
            nameValuePairs.add(new BasicNameValuePair("key", access_key));

            nameValuePairs.add(new BasicNameValuePair("start_lat", params[3]));
            nameValuePairs.add(new BasicNameValuePair("start_long", params[4]));
            nameValuePairs.add(new BasicNameValuePair("end_lat", params[5]));
            nameValuePairs.add(new BasicNameValuePair("end_long", params[6]));

            nameValuePairs.add(new BasicNameValuePair("journey_time", params[7]));
            nameValuePairs.add(new BasicNameValuePair("margin_before", params[8]));
            nameValuePairs.add(new BasicNameValuePair("margin_after", params[9]));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = httpclient.execute(httppost);

            statusCode = response.getStatusLine().getStatusCode();

            Log.d(TAG, statusCode + " : " + response.getStatusLine().getReasonPhrase());

            if(statusCode==200){
                // do shit
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }
}
