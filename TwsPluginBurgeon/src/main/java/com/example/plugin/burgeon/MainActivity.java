package com.example.plugin.burgeon;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "rick_Print:MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_test).setOnClickListener(this);
        findViewById(R.id.btn_test_crash).setOnClickListener(this);

        printProcess();
    }

    @Override
    public void onClick(View v) {
        String nullStr = null;
        switch (v.getId()) {
            case R.id.btn_test:
                printProcess();
                break;
            case R.id.btn_test_crash:
                nullStr.equals("111");
            default:
                break;
        }
    }

    private void printProcess() {

    }
}
