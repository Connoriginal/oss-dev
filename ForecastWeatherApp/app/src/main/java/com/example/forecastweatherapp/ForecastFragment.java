package com.example.forecastweatherapp;



import android.content.Intent;


import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> forecastAdapter;


    public ForecastFragment(){

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refreshWeatherData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshWeatherData(){
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String cityId = prefs.getString("city", "1835847");
        weatherTask.execute(cityId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){

        String[] data = new String[7];

        Time dayTime = new Time();
        dayTime.setToNow();

        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        dayTime = new Time();
        for(int i = 0 ; i < 7 ; i++){
            String day;
            long dateTime = dayTime.setJulianDay(julianStartDay+i);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            day = simpleDateFormat.format(dateTime);
            data[i] = day;
        }


        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        forecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_view_item,
                R.id.list_item_forecast_textview,
                weekForecast);

        View rootView = (View) inflater.inflate(R.layout.forecast_fragment, container, false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(forecastAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = forecastAdapter.getItem(position);
                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra("data", forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private String getDate(long time){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return simpleDateFormat.format(time);
        }

        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        private String[] getWeatherDataFromJson(String forecastJsonStr,int numDays)
                throws JSONException {
            //JSON objects
            final String OWM_LIST = "list";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_WEATHER = "weather";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0 ; i < weatherArray.length(); i++){
                String day;
                String tempHighAndLow;
                String description;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getDate(dateTime);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble("max");
                double low = temperatureObject.getDouble("min");
                tempHighAndLow = formatHighLows(high,low);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString("main");

                resultStrs[i] = day + ",\nWeather: " + description + " and the temperature (high/low) :" + tempHighAndLow;
            }

            for(String s : resultStrs){
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;
        }
        @Override
        protected String[] doInBackground(String... strings) {

            if(strings.length == 0) return null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;


            String forecastJsonStr = null;


            String format = "json";
            String units = "metric";
            int numDays = 7;
            try {

                final String FORECAST_BASE_URL =
                        "https://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "id";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, strings[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, "5fd2f2cde90c1533efb95b19c048a528")
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                Log.v(LOG_TAG,"여기는왔냐?ㅅ");
                urlConnection.setRequestMethod("GET");
                Log.v(LOG_TAG,"여기는왔냐?ㅅㅂㅅㅃㅅ");
                Log.v(LOG_TAG,urlConnection.toString());


                urlConnection.connect();
                Log.v(LOG_TAG,"여기는왔냐?여기가 문제냐?");

                Log.v(LOG_TAG,"여기는왔냐?");
                InputStream inputStream = urlConnection.getInputStream();
                Log.v(LOG_TAG,"여기는왔냐?너냐?");
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) return null;
                Log.v(LOG_TAG,"여기는왔냐?2");
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                Log.v(LOG_TAG,"여기는왔냐?3");
                while ((line = reader.readLine()) != null) buffer.append(line + "\n");

                if (buffer.length() == 0) return null;

                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);

                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                forecastAdapter.clear();
                for(String dayForecastStr : result) {
                    forecastAdapter.add(dayForecastStr);
                }
            }
        }
    }

}

