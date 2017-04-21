package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.assistant.widget.ToggleButton;

public class ActionBarNormalButton extends TwsActivity {
	
	private Button leftBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getTwsActionBar().setTitle(R.string.action_bar_title);
		getTwsActionBar().setSubtitle(R.string.action_bar_subtitle);
		leftBtn = ((Button)getTwsActionBar().getCloseView(false));
		leftBtn.setText("上一步");
		ToggleButton b = (ToggleButton)getTwsActionBar().getMultiChoiceView(false);
		b.setText("下一步");
		b.setFixedText(false);
		b.setOnClickListener(new OnClickListener() {
			
			int count = 0;
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (count % 2 == 0) {
					getTwsActionBar().getActionBarHome().setVisibility(View.VISIBLE);
					getTwsActionBar().setDisplayShowHomeEnabled(true);
					getTwsActionBar().getActionBarView().removeView(leftBtn);
				}
				else {
					getTwsActionBar().getActionBarHome().setVisibility(View.GONE);
					getTwsActionBar().setDisplayShowHomeEnabled(false);
					((Button)getTwsActionBar().getCloseView(false)).setVisibility(View.VISIBLE);
				}
				count++;
			}
		});
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this, "action_bar_settings_action_provider_no_handling", Toast.LENGTH_SHORT).show();
        return false;
    }

    public static class SettingsActionProvider extends ActionProvider {
        private static final Intent sSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
        private final Context mContext;

        public SettingsActionProvider(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public View onCreateActionView() {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            View view = layoutInflater.inflate(R.layout.action_bar_settings_action_provider, null);
            ImageButton button = (ImageButton) view.findViewById(R.id.button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(sSettingsIntent);
                }
            });
            return view;
        }

        @Override
        public boolean onPerformDefaultAction() {
            mContext.startActivity(sSettingsIntent);
            return true;
        }
    }
}
