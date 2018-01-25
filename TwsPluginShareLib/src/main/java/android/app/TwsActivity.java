package android.app;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.rick.tws.sharelib.R;
import com.rick.tws.widget.TwsToolbar;

public class TwsActivity extends AppCompatActivity {
    private static final String TAG = TwsActivity.class.getSimpleName();
    private TwsToolbar mToolbar = null;

    protected void initTwsActionBar(boolean showNavigationIcon) {
        if (null == mToolbar) {
            mToolbar = (TwsToolbar) findViewById(R.id.toolbar);
            setSupportActionBar(mToolbar);

            final ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                mToolbar.setBackDrawable(mToolbar.getBackDrawable());
                supportActionBar.setHomeAsUpIndicator(mToolbar.getBackDrawable());
                supportActionBar.setDisplayShowTitleEnabled(false);
                supportActionBar.setDisplayHomeAsUpEnabled(false);
                if (showNavigationIcon) {
                    mToolbar.setNavigationIcon(mToolbar.getBackDrawable());
                    mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                        }
                    });
                }
            } else {
                //mToolbar 为 null，造成supportActionBar也为null，不做处理
            }
        }
    }

    public void setNavigationOnClickListener(View.OnClickListener listener) {
        if (null != mToolbar) {
            mToolbar.setNavigationOnClickListener(listener);
        }
    }

    @Override
    public void setTitle(int resId) {
        if (null != mToolbar) {
            mToolbar.setTitle(resId);
        } else {
            super.setTitle(resId);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (null != mToolbar) {
            mToolbar.setTitle(title);
        } else {
            super.setTitle(title);
        }
    }
}
