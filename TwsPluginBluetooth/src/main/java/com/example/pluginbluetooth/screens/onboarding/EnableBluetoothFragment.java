package com.example.pluginbluetooth.screens.onboarding;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.screens.BaseFragment;

public class EnableBluetoothFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialogfragment_onboarding_enable_bluetooth, null);
        view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Onboarding.getInstance().gotoState(Onboarding.State.CANCEL);
            }
        });
        view.findViewById(R.id.btn_turn_on_bluetooth).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothAdapter.getDefaultAdapter().enable();
            }
        });
        return view;
    }

    @Override
    public String getName() {
        return "EnableBluetoothFragment";
    }

    public static EnableBluetoothFragment newInstance() {
        return new EnableBluetoothFragment();
    }
}

