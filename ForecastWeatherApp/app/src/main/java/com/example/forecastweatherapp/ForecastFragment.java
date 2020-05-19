package com.example.forecastweatherapp;



import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;


import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.Time;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> forecastAdapter;
    private IFetchWeatherService mService;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(FetchWeatherService.ACTION_RETRIEVE_WEATHER_DATA)) {
                String[] data = intent.getStringArrayExtra(FetchWeatherService.EXTRA_WEATHER_DATA);
                forecastAdapter.clear();
                for(String dayForecastStr : data) {
                    forecastAdapter.add(dayForecastStr);
                }
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IFetchWeatherService.Stub.asInterface(service);

            try {
                mService.registerFetchDataListener(mFetchDataListener);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private IFetchDataListener.Stub mFetchDataListener = new IFetchDataListener.Stub() {
        @Override
        public void onWeatherDataRetrieved(String[] data) throws RemoteException {
            forecastAdapter.clear();
            for(String dayForecastStr : data) {
                forecastAdapter.add(dayForecastStr);
            }
        }
    } ;

    public ForecastFragment(){

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        getActivity().bindService(new Intent(getActivity(), FetchWeatherService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        if (mService != null) {
            try {
                mService.unregisterFetchDataListener(mFetchDataListener);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
        getActivity().unbindService(mServiceConnection);
        super.onDestroy();
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
//        Intent intent = new Intent(getActivity(), FetchWeatherService.class);
//        intent.setAction(FetchWeatherService.ACTION_RETRIEVE_WEATHER_DATA);
//        getActivity().startService(intent);
        if (mService != null) {
            try {
                mService.retrieveWeatherData();
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
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

    @Override
    public void onDestroyView() {
        //getActivity().unregisterReceiver(mBroadcastReceiver);
        super.onDestroyView();
    }
}

