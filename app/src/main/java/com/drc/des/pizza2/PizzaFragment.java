package com.drc.des.pizza2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PizzaFragment extends Fragment {

    ArrayAdapter<String> mLocationAdapter;
    private static final String TAG = MainActivity.class.getName();
    private static final String FILENAME = "myFile.txt";

    public PizzaFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pizzafragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchLocationTask locationTask = new FetchLocationTask();
            locationTask.execute("10012");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        String lastCalled = readFromFile();
        String[] called = lastCalled.split("\n");
        String nam = called[0];
        String numb = called[1];
        String[] busNameArray = {nam+"\n"+numb};

        final List<String> busList = new ArrayList<>(Arrays.asList(busNameArray));
        mLocationAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this fragment's parent activities)
                        R.layout.location_view, // ID of the list item layout
                        R.id.location_textView, // ID of text view to populate
                        busList); // data

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        // Get a reference to the ListView, and attach this adapter to it.
        final ListView listView = (ListView) rootView.findViewById(R.id.location_listView);
        listView.setAdapter(mLocationAdapter);

        Button searchButton = (Button) rootView.findViewById(R.id.button1);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText edittext = (EditText) getActivity().findViewById(R.id.editText);

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

                String text = edittext.getText().toString();
                FetchLocationTask locationTask = new FetchLocationTask();
                locationTask.execute(text);

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Object item = busList.get(position);
                String myItem = item.toString();
                String[] pnum = myItem.split("\n");
                String placeName = pnum[0];
                String phoneNum = pnum[1];
                writeToFile(myItem);

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"+phoneNum));
                startActivity(intent);
                //Toast.makeText(getActivity(), pnum[1], Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getActivity().openFileOutput(FILENAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    private String readFromFile() {

        String ret = "dummy\n123 9874";

        try {
            InputStream inputStream = getActivity().openFileInput(FILENAME);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                    stringBuilder.append("\n");
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }

    public class FetchLocationTask extends AsyncTask<String, Void, String[]>{

        private final String LOG_TAG = FetchLocationTask.class.getSimpleName();

        private String[] getLocationDataFromJson(String locationJsonStr, int listNum)
                throws JSONException {

                // These are the names of the JSON objects that need to be extracted.
            final String OWN_SEARCH = "searchResult";
            final String OWM_RESULT = "searchListings";
            final String OWM_LIST = "searchListing";
            final String OWM_NAME = "businessName";
            final String OWN_PHONE = "phone";

            JSONObject locationJson = new JSONObject(locationJsonStr);
            JSONObject searchJson = locationJson.getJSONObject(OWN_SEARCH);
            JSONObject listingsJson = searchJson.getJSONObject(OWM_RESULT);
            JSONArray busArray = listingsJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[listNum];

            for(int i = 0; i < busArray.length(); i++) {
                String name;
                String phone;

                // Get the JSON object representing the location
                JSONObject locationInfo = busArray.getJSONObject(i);
                name = locationInfo.getString(OWM_NAME);
                phone = locationInfo.getString(OWN_PHONE);

                resultStrs[i] = name + "\n" + phone;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "entry: " + s);
            }
            return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... params){

            if (params.length == 0){
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            //JSON response as string
            String busJsonStr;
            String format = "json";
            //TODO get key from YP.com
            String key = "";
            int listNum = 10;

            try {
                //construct URL for YP query
                final String LOCATION_BASE_URL =
                        "http://pubapi.yp.com/search-api/search/devapi/search?";
                final String QUERY_PARAM = "searchloc";
                final String FOOD_PARAM = "term";
                final String FORMAT_PARAM = "format";
                final String SORT_PARAM = "sort";
                final String RADIUS_PARAM = "radius";
                final String LISTNUM_PARAM = "listingcount";
                final String API_KEY = "key";
                Uri builtUri = Uri.parse(LOCATION_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FOOD_PARAM, "pizza")
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(SORT_PARAM, "distance")
                        .appendQueryParameter(RADIUS_PARAM, "5")
                        .appendQueryParameter(LISTNUM_PARAM, Integer.toString(listNum))
                        .appendQueryParameter(API_KEY, key)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // create request to YP and open
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // read input stream to a string
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null){
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null){
                    buffer.append(line + "\n");
                }

                if(buffer.length() == 0){
                    //stream empty
                    return null;
                }
                busJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Business JSON String" + busJsonStr);
            } catch (IOException e){
                Log.e("PizzaFragment", "Error", e);
                return null;
            } finally {
                if (urlConnection != null){
                    urlConnection.disconnect();
                }
                if (reader != null){
                    try{
                        reader.close();
                    } catch (final IOException e){
                        Log.e("PizzaFragment", "Error closing stream", e);
                    }
                }
            }
            try {
                return getLocationDataFromJson(busJsonStr, listNum);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null){
                mLocationAdapter.clear();
                for (String locInfoStr : result){
                    mLocationAdapter.add(locInfoStr);
                }
            }
        }
    }
}