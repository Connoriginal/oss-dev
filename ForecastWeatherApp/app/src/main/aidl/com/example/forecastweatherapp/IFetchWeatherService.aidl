// IFetchWeatherService.aidl
package com.example.forecastweatherapp;

// Declare any non-default types here with import statements
import com.example.forecastweatherapp.IFetchDataListener;

interface IFetchWeatherService {
    void retrieveWeatherData();
    void registerFetchDataListener(IFetchDataListener listener);
    void unregisterFetchDataListener(IFetchDataListener listener);
}

