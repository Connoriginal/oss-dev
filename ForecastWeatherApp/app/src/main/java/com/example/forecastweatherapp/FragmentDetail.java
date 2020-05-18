package com.example.forecastweatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class FragmentDetail extends Fragment {

    public FragmentDetail(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle data = getArguments();
        String detailData = null;

        if (data != null) {
            detailData = data.getString("data");
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);


        TextView detailTextView = (TextView)rootView.findViewById(R.id.detail_textview);
        detailTextView.setText(detailData);

        return rootView;
    }


}
