package com.example.plugindemo.communicationdma;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.plugindemo.R;
import com.example.plugindemo.communicationdma.filetransfer.FileTransferDemoActivity;

public class FTDemoActivity extends Activity implements OnClickListener {
    
    private Button mOpenFtDemoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ft_demo);
        
        mOpenFtDemoButton = (Button) findViewById(R.id.open_ft_bt);
        mOpenFtDemoButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.open_ft_bt:
            startActivity(new Intent(getApplicationContext(), FileTransferDemoActivity.class));
            break;

        default:
            break;
        }
    }

}
