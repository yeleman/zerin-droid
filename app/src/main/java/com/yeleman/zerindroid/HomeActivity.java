package com.yeleman.zerindroid;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class HomeActivity extends ActionBarActivity {

    private String TAG = "LKLC-Home";

    public final static String SERIALIZED_TASK = "com.yeleman.zerindroid.SERIALIZED_TASK";

    private Button fetch_btn;
    private EditText edit_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public void setupUI() {
        Log.i(TAG, "setupUI");

        fetch_btn = (Button) findViewById(R.id.fetch_task_btn);
        edit_text = (EditText) findViewById(R.id.task_id_text);
        edit_text.setFocusable(true);
        edit_text.setFocusableInTouchMode(true);
        edit_text.requestFocus();

        fetch_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	Integer task_id = Integer.parseInt(edit_text.getText().toString());
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
                String serverUrl = sharedPrefs.getString("serverUrl", Constants.DEFAULT_SERVER_URL);
                String url = String.format(Constants.TASK_DETAIL_URL, serverUrl, task_id);

            	DownloadTask taskJsonDownload = new DownloadTask();
            	taskJsonDownload.execute(url);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	Log.i(TAG, "settings clicked");
        	Intent i = new Intent(this, Preferences.class);
        	startActivityForResult(i, 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        try{
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();
            br.close();
            iStream.close();
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }

        return data;
    }

	/** AsyncTask to download json data */
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        String data = null;
        private ProgressDialog Dialog = new ProgressDialog(HomeActivity.this);

        public boolean isOnline() {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            // Loading
            if (!isOnline())
                return;
            Dialog.setMessage("Chargement en cours ...");
            Dialog.show();
        }

        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            if (isOnline()) {
                if (result != "") {
                    JSONLoaderTask jsonLoaderTask = new JSONLoaderTask();
                    jsonLoaderTask.execute(result);
                } else {
                	Toast.makeText(
						HomeActivity.this,
	                    "Impossible de récupérer la tâche.\nVérifiez l'identifiant.",
	                    Toast.LENGTH_LONG).show();
                }
                // after completed finished the progressbar
                Dialog.dismiss();
            }else{
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomeActivity.this);
                alertDialog.setTitle("Problème de connexion");
                alertDialog.setIcon(R.drawable.ic_launcher);
                alertDialog.setMessage("Une connexion Internet est requise.\nVeuillez l'activer et réessayer.");
                alertDialog.setNeutralButton("OK", null);
                alertDialog.show();
            }
        }
    }

	private class JSONLoaderTask extends AsyncTask<String, Void, JSONObject> {

        JSONObject jObject;
        @Override
        protected JSONObject doInBackground(String... strJson) {
        	// Parse JSON to make sure URL was properly downloaded
            try{
                return new JSONObject(strJson[0]);
            } catch (Exception e) {
                Log.d(TAG, "Unable to parse fetched JSON: " + e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final JSONObject jObject) {
			if (jObject == null) {
				// download or parsing failed.
				Toast.makeText(
					HomeActivity.this,
                    "Erreur dans la description de la tâche (JSON Parsing).\nVérifiez l'identifiant.",
                    Toast.LENGTH_LONG).show();
			} else {
				Intent intent = new Intent(HomeActivity.this, TaskActivity.class);
	            intent.putExtra(SERIALIZED_TASK, jObject.toString());
	            startActivity(intent);
			}
        }
    }
}
