package com.example.forecastweatherapp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class FetchWeatherService extends Service {

    public static final String ACTION_RETRIEVE_WEATHER_DATA="com.example.forecastweatherapp.RETRIEVE_DATA";
    public static final String EXTRA_WEATHER_DATA = "weather-data";

    public FetchWeatherService(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        String action = intent.getAction();
        if(action.equals(ACTION_RETRIEVE_WEATHER_DATA)){
            retrieveWeatherData(startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void retrieveWeatherData(int startId){
        FetchWeatherTask weatherTask = new FetchWeatherTask(startId);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String cityId = prefs.getString("city","1835847");
        weatherTask.execute(cityId);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new FetchWeatherServiceProxy(this);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private int mStartId = -1;

        public FetchWeatherTask(int startId){
            mStartId = startId;
        }

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
                notifyWeatherDataRetrieved(result);
            }
            if(mStartId < 0) return;

            stopSelf(mStartId);
            Log.v(LOG_TAG,"여기까지 온거보니 좀 된듯?");
        }

    }

    private void notifyWeatherDataRetrieved(String[] result){
        synchronized (mListeners) {
            for(IFetchDataListener listener : mListeners){
                try{
                    listener.onWeatherDataRetrieved(result);
                } catch (RemoteException ex){
                    ex.printStackTrace();
                }
            }
        }
        Intent intent = new Intent(ACTION_RETRIEVE_WEATHER_DATA);
        intent.putExtra(EXTRA_WEATHER_DATA,result);
        sendBroadcast(intent);
        Log.v(FetchWeatherTask.class.getSimpleName(),"브로드보냄?");

    }

    private ArrayList<IFetchDataListener> mListeners = new ArrayList<IFetchDataListener>();
    private void registerFetchDataListener(IFetchDataListener listener) {
        synchronized (mListeners) {
            if (mListeners.contains(listener)) {
                return;
            }
            mListeners.add(listener);
        }
    }

    private void unregisterFetchDataListener(IFetchDataListener listener) {
        synchronized (mListeners) {
            if (!mListeners.contains(listener)) {
                return;
            }

            mListeners.remove(listener);
        }
    }

    private class FetchWeatherServiceProxy extends IFetchWeatherService.Stub {
        private WeakReference<FetchWeatherService> mService = null;

        public FetchWeatherServiceProxy(FetchWeatherService service) {
            mService = new WeakReference<FetchWeatherService>(service);
        }

        @Override
        public void retrieveWeatherData() throws RemoteException {
            mService.get().retrieveWeatherData(-1);
        }

        @Override
        public void registerFetchDataListener(IFetchDataListener listener) throws RemoteException {
            mService.get().registerFetchDataListener(listener);
        }

        @Override
        public void unregisterFetchDataListener(IFetchDataListener listener) throws RemoteException {
            mService.get().unregisterFetchDataListener(listener);
        }
    }
}
