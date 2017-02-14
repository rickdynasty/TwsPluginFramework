package com.example.plugindemo.communicationdma;

import android.app.TwsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.communicationdma.download.DownloadDemoActivity;
import com.example.plugindemo.communicationdma.filetransfer.FileTransferDemoActivity;

public class CommunicationDMAActivity extends TwsActivity implements View.OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_communication_dma_main);
		findViewById(R.id.communication_dma_download).setOnClickListener(this);
		findViewById(R.id.communication_dma_ft_demo).setOnClickListener(this);
		findViewById(R.id.communication_dma_filetransfer).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		Intent intent;
		switch (view.getId()) {
		case R.id.communication_dma_download:
			intent = new Intent();
			intent.setClassName(this, DownloadDemoActivity.class.getName());
			startActivity(intent);
			break;
		case R.id.communication_dma_ft_demo:
			intent = new Intent();
			intent.setClassName(this, FTDemoActivity.class.getName());
			startActivity(intent);
			break;
		case R.id.communication_dma_filetransfer:
			intent = new Intent();
			intent.setClassName(this, FileTransferDemoActivity.class.getName());
			startActivity(intent);
			break;

		default:
			break;
		}
	}

}
