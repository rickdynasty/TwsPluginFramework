package com.example.pluginbluetooth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    private static final String INTENT_EXTRA_ENTER_ANIM_RES = "enterAnimRes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    public static Intent createStartIntent(Context context, int enterAnimResource) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(INTENT_EXTRA_ENTER_ANIM_RES, enterAnimResource);
        return intent;
    }
}
