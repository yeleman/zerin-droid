package com.yeleman.zerindroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class TaskActivity extends ActionBarActivity {

	private String TAG = "LKLC-Task";

	// Full Task JSON
    private JSONObject task_data;
    
    // Task Informations
    public Integer task_id;
    public String task_name; 
    public String task_status;
    public String task_action;

    // Items-related task-level informations
    public Integer nb_items;
    public Integer total_amount;

    // List of individual Items
    public List<HashMap<String, Object>> items = new ArrayList<HashMap<String,Object>>();

    // Adapter for LV
    SimpleAdapter items_adapter;

    // UI Elements
    private Button start_task_btn;
    private TextView task_title_label;
    private TextView task_type_text;
    private TextView task_id_text;
    private TextView task_total_amount_text;
    private TextView task_nb_recipients_text;
    private ListView task_recipients_list;

    // poped'out list of USSD codes
    private List<HashMap<String, Object>> action_items = new ArrayList<HashMap<String,Object>>();

    // helpers
    protected static String serverUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        Log.d(TAG, "onCreate");

        // retrieve serialized JSON string from intent call
        Intent intent = getIntent();
        String serialialized_json = intent.getStringExtra(
        	HomeActivity.SERIALIZED_TASK);
        Log.d(TAG, serialialized_json);

        // Parse JSON and launch UI if successful
        // otherwise return to Home.
        if (this.parseJSON(serialialized_json)) {
        	this.setupUI();
        } else {
        	Toast.makeText(
        		TaskActivity.this,
        		"Impossible de lire le fichier JSON de la tâche",
        		Toast.LENGTH_LONG).show();
        	finish();
        }
        
    }

    private boolean parseJSON(String serialialized_json) {
    	Log.d(TAG, "parseJSON");

        // some usefull data
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(TaskActivity.this);
        serverUrl = sharedPrefs.getString("serverUrl", Constants.DEFAULT_SERVER_URL);

    	// Parse serialized JSON.
    	// Should never failed as HomeActivity already calls this
    	// before firing this activity
    	try {
        	task_data = new JSONObject(serialialized_json);
        } catch (JSONException e) {
        	Log.e(TAG, "Error parsing JSON. leaving.");
        	e.printStackTrace();
        	return false;
        }

        // grab task-level informations
        try {
	        task_id = task_data.getInt("id");
	        task_name = task_data.getString("name");
	        task_status = task_data.getString("status");
	        task_action = task_data.getString("action");
	   	} catch(JSONException e) {
	   		Log.e(TAG, "Error parsing JSON. leaving.");
        	e.printStackTrace();
	   		return false;
	   	}

        // build HashMap for items and set loop variables (lengths)
        JSONArray jsItems;
        try {
    		jsItems = task_data.getJSONArray("items");
    	} catch(JSONException e) {
	   		Log.e(TAG, "Error parsing JSON. leaving.");
        	e.printStackTrace();
	   		return false;
	   	}
        nb_items = jsItems.length();
        total_amount = 0;

        for (int i=0; i < nb_items; i++) {
        	try {
        		JSONObject jsItem = (JSONObject) jsItems.get(i);
        		HashMap<String, Object> item = getItem(jsItem);
        		items.add(item);
                total_amount += (Integer) item.get("amount");
        	} catch (JSONException e) {
        		Log.e(TAG, "Unable to decode single-item JSON");
                e.printStackTrace();
            }
        }
        return true;
    }

    private HashMap<String, Object> getItem(JSONObject jItem) throws JSONException {
    	Log.d(TAG, "getItem");

        HashMap<String, Object> item = new HashMap<String, Object>();
        int id = -1;
        int amount = -1;
        String amount_str = null;
        String status = null;
        int c_id = -1;
        String c_str = null;
        String c_name = null;
        String c_number = null;
        String c_number_str = null;
        String c_number_ussd = null;
        String c_operator = null;

        try {
        	// Item level fields
            id = jItem.getInt("id");
            amount = jItem.getInt("amount");
            status = jItem.getString("status");
            amount_str = String.format("%s FCFA", amount);

            // Contact level fields
            JSONObject contact = (JSONObject) jItem.get("contact");
            c_id = contact.getInt("id");
            c_name = contact.getString("name");
            c_number = contact.getString("number");
            c_number_str = contact.getString("number_str");
            c_number_ussd = c_number.replace("+223", "");
            c_operator = contact.getString("operator");

            // Custom strings based on data
            if (c_name != null && c_name.length() > 0) {
            	c_str = String.format("%s - %s", c_number_str, c_name);
            } else {
            	c_str = c_number_str;
            } 
        } catch (JSONException e) {
        	Log.e(TAG, "Failed single-item JSON decode: " + e.toString());
            e.printStackTrace();
        }

        item.put("c_str", c_str);
        item.put("id", id);
        item.put("amount", amount);
        item.put("amount_str", amount_str);
        item.put("status", status);
        item.put("c_id", c_id);
        item.put("c_name", c_name);
        item.put("c_number", c_number);
        item.put("c_number_str", c_number_str);
        item.put("c_number_ussd", c_number_ussd);
        item.put("c_operator", c_operator);
        item.put("c_str", c_str);
        return item;
    }

    private void setupUI() {
        Log.i(TAG, "setupUI");
        
        // fancy title
        this.setTitle(String.format("Tâche #%s", task_id));

        // retrieve all our UI elements
        task_title_label = (TextView) findViewById(R.id.task_title_label);
        task_type_text = (TextView) findViewById(R.id.task_type_text);
		task_id_text = (TextView) findViewById(R.id.task_id_text);
		task_nb_recipients_text = (TextView) findViewById(R.id.task_nb_recipients_text);
		task_total_amount_text = (TextView) findViewById(R.id.task_total_amount_text);
		start_task_btn = (Button) findViewById(R.id.start_task_btn);
		task_recipients_list = (ListView) findViewById(R.id.task_recipients_list);

		// Fill-in all UI elements
        task_title_label.setText(task_name);       
        task_type_text.setText(task_action);
        task_id_text.setText(task_id.toString());
        task_nb_recipients_text.setText(nb_items.toString());
        task_total_amount_text.setText(total_amount.toString() + " FCFA");

        // Build List of recipients/items
        String[] items_keys = {		// Keys used in Hashmap
        	"c_str",
        	"amount_str"
        };
        int[] lv_layout_keys = {	// IDs used in layout
        	R.id.number_text,
        	R.id.amount_text,
        };
        items_adapter = new SimpleAdapter(
        	getBaseContext(), items, R.layout.recipients,
        	items_keys, lv_layout_keys);
        task_recipients_list.setAdapter(items_adapter);

        // Build Startup button
        final AlertDialog.Builder adb = new AlertDialog.Builder(this);
        start_task_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "StartTask Button Clicked");
                adb.setTitle("Confirmez");
                adb.setMessage("Êtes vous sûr de vouloir déclencher cet envoi ? Vous ne pourrez plus l'arrêter.");
                adb.setNegativeButton("Annuler", null);
                adb.setPositiveButton("Confirmer", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        Log.i(TAG, "Task started");
                        Toast.makeText(TaskActivity.this, "Starting Task…", Toast.LENGTH_SHORT).show();
                        start_task_btn.setEnabled(false);
                        executeTaskAction();
                    }
                });
                adb.show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String ussdForItem(HashMap<String, Object> item) {

    	String airtime_orange = "*111*%s*%s*1*%s#OK";
    	String airtime_code = "12345";

    	String ussd = String.format(
    		airtime_orange,
    		item.get("c_number_ussd"),
    		item.get("amount"),
    		airtime_code);
    	return ussd;
    }

    private void processSingleItem(HashMap<String, Object> item) {
    	// get ussd
    	String ussd = (String) item.get("ussd");
    	Log.d(TAG, ussd);

		// display toast text    	
    	Toast.makeText(
    		TaskActivity.this,
    		String.format(
    			"Sending %s F to %s",
    			item.get("amount_str"),
    			item.get("c_str")),
    		Toast.LENGTH_SHORT).show();
    	
    	// change status on listView

    	// send USSD to intent
    	boolean failure = !startUSSD(ussd);

    	// submit status change to server
    	this.sendItemStatusUpdate(
    		(Integer) item.get("id"),
    		Constants.TASK_ITEM_PROCESSING);
    }

    protected boolean startUSSD(String ussd) {
        String encodedUSSD = Uri.encode(ussd);
        try {
            startActivityForResult(
                new Intent("android.intent.action.CALL",
                           Uri.parse("tel:"+ encodedUSSD)), 1);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error submitting USSD: " + e.toString());
            return false;
        }
    }

    private void executeTaskAction() {

    	Log.d(TAG, "executeTaskAction");

    	// update status on LKLC
    	sendTaskStatusUpdate(Constants.TASK_PROCESSING);

    	Log.d(TAG, "status updated");

    	for (int i=0; i < nb_items; i++) {
    		HashMap<String, Object> item = items.get(i);
    		item.put("ussd", this.ussdForItem(item));
    		action_items.add(item);
    	}

    	final Iterator<HashMap<String, Object>> it = action_items.iterator();

    	HandlerThread hThread = new HandlerThread("HandlerThread");
		hThread.start();
		final Handler handler = new Handler(hThread.getLooper());
		final long interval = 6 * 1000; // 6 seconds
		Runnable sendNextUSSD = new Runnable() {	
			@Override
			public void run() {
				Log.d(TAG, "Each minute task executing");
				if (it.hasNext()) {
					HashMap<String, Object> item = it.next();
    				it.remove();
    				processSingleItem(item);
    				handler.postDelayed(this, interval);	
				} else {
					// iterator exhausted. Task completed.
					Log.i(TAG, String.format("Task #%s completed.", task_id));

					// update status on LKLC
					sendTaskStatusUpdate(Constants.TASK_COMPLETE);

					final AlertDialog.Builder adb = new AlertDialog.Builder(TaskActivity.this);
	                adb.setTitle("Envoi terminé");
	                adb.setIcon(R.drawable.ic_launcher);
	                adb.setMessage("Les éléments de la tâche ont été traités.\nMerci de vérifier le statut sur LKLC.");
	                adb.setNeutralButton("OK", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int arg1) {
                            finish();
                        }
                    });

                    Context context = getApplicationContext();
                    Intent intent = new Intent(context, TaskActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // You need this if starting
					                                                // the activity from a service
					intent.setAction(Intent.ACTION_MAIN);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					startActivity(intent);
                    adb.show();
				}			
			}
		};
		handler.postDelayed(sendNextUSSD, interval);
    }

    protected void sendItemStatusUpdate(Integer item_id, String new_status) {
    	String url = String.format(
    		Constants.TASK_ITEM_UPDATE_URL, serverUrl, item_id);
    	ArrayList<NameValuePair> payload = new ArrayList<NameValuePair>();
        payload.add(new BasicNameValuePair("status", new_status));
    	this.submitPostData(url, payload);
    }

    protected void sendTaskStatusUpdate(String new_status) {
    	String url = String.format(
    		Constants.TASK_UPDATE_URL, serverUrl, task_id);
    	ArrayList<NameValuePair> payload = new ArrayList<NameValuePair>();
        payload.add(new BasicNameValuePair("status", new_status));
    	this.submitPostData(url, payload);
    }

    protected void submitPostData(
	    	final String url,
	    	final ArrayList<NameValuePair> payload) {

    	new Thread(new Runnable() {
		    public void run() {
		      postData(url, payload);
		    }
		}).start();
    }

    protected boolean postData(String url, ArrayList<NameValuePair> payload) {
    	HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(payload));
            httpClient.execute(httpPost);
        } catch (ClientProtocolException e) {
            Log.e(TAG, "ClientProtocolException: " + e.toString());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.toString());
            e.printStackTrace();
            return false;
        } catch(Exception e) {
        	Log.e(TAG, "Exception: " + e.toString());
        	e.printStackTrace();
        	return false;
        }
        return true;
    }
}
