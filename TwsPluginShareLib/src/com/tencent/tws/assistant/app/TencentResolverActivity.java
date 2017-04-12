/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tws.assistant.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.assistant.support.v4.view.PagerAdapter;
import com.tencent.tws.assistant.support.v4.view.ViewPager;
import com.tencent.tws.assistant.support.v4.view.ViewPager.OnPageChangeListener;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.assistant.widget.AdapterView.OnItemLongClickListener;
import com.tencent.tws.assistant.widget.CheckBox;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.assistant.widget.TwsGridView;
import com.tencent.tws.sharelib.R;
//Warning don't change this to tws widget!!
//Warning end




/**
 * This activity is displayed when the system attempts to start an Intent for
 * which there is more than one matching activity, allowing the user to decide
 * which to go to. It is not normally used directly by application developers.
 */
public class TencentResolverActivity extends AlertActivity {
	private static final String TAG = "TencentResolverActivity";
	private static final boolean DEBUG = false;

	private int mLaunchedFromUid;
	private TencentPagerAdapter mAdapter;
	private PackageManager mPm;
	private boolean mAlwaysUseOption;
	private boolean mShowExtended;
	private ListView mListView;
	private Button mAlwaysButton;
	private Button mOnceButton;
	private int mIconDpi;
	private int mIconSize;
	private boolean mIsNeedSaveToSharedPreference = true;
	private int mLastSelected = ListView.INVALID_POSITION;

	
	private int mCategoryIndex = -1;
	private boolean mRegistered;
	
	private static final int SINGLE_COUNT = 8;
	private static final int SINGLE_LINE_HEIGHT = 98;
	private static final int TWO_LINE_HEIGHT = 199;
	
	private int whichPage;
	private boolean mAlwaysCheck;
	
	private List<ResolveInfo> mBaseResolveList;
	private List<ResolveInfo> mOrigResolveList;
	private ResolveInfo mLastChosen;
	private List<DisplayResolveInfo> mList;
	private Intent[] mInitialIntents;
	private Intent mIntent;
	
	private final PackageMonitor mPackageMonitor = new PackageMonitor() {
		@Override
		public void onSomePackagesChanged() {
			mAdapter.handlePackagesChanged();
		}
	};

	private Intent makeMyIntent() {
		Intent intent = new Intent(getIntent());
		
		intent.setComponent(null);
		// The resolver activity is set to be hidden from recent tasks.
		// we don't want this attribute to be propagated to the next activity
		// being launched. Note that if the original Intent also had this
		// flag set, we are now losing it. That should be a very rare case
		// and we can live with this.
		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		Log.i("yan","intent="+intent.toString());
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		final int titleResource;
		final Intent intent = makeMyIntent();
		final Set<String> categories = intent.getCategories();

		onCreate(savedInstanceState, intent, null, null, true);
	}

	protected void onCreate(Bundle savedInstanceState, Intent intent, Intent[] initialIntents,
			List<ResolveInfo> rList, boolean alwaysUseOption) {

		super.onCreate(savedInstanceState);
		setBottomDialog(true);
		try {
			mLaunchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
		} catch (RemoteException e) {
			mLaunchedFromUid = -1;
		}
		AlertController.AlertParams ap = mAlertParams;
		ap.mTitle = "";

		mPm = getPackageManager();
		mAlwaysUseOption = alwaysUseOption;

		mPackageMonitor.register(this, getMainLooper(), false);

		mRegistered = true;

		final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		mIconDpi = am.getLauncherLargeIconDensity();
		mIconSize = am.getLauncherLargeIconSize();
		
		
		mIntent = new Intent(intent);
		mList = new ArrayList<DisplayResolveInfo>();
		rebuildList();
		
		int itemCount = mList == null ? 0 : mList.size();
		int pageCount = 0;
		
		if (itemCount > 0) {
			if (itemCount % SINGLE_COUNT == 0) {
                pageCount = itemCount / SINGLE_COUNT;
            } else {
                pageCount = itemCount / SINGLE_COUNT + 1;
            }
			
			final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View vg = inflater.inflate(R.layout.resolverlayout, null);
			CheckBox checkBox = (CheckBox) vg.findViewById(R.id.resolvercheck);
			ViewPager viewPager = (ViewPager) vg.findViewById(R.id.resolverviewpager);
			LinearLayout viewPoints = (LinearLayout) vg.findViewById(R.id.resolverpoints);
			
			checkBox.setOnCheckedChangeListener(new TencentOnCheckedChangeListener());
			
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			
			if (itemCount <= SINGLE_COUNT/2) {
				params.height = (int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, SINGLE_LINE_HEIGHT, getResources().getDisplayMetrics());
			} else {
				params.height = (int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, TWO_LINE_HEIGHT, getResources().getDisplayMetrics());
			}
			viewPager.setLayoutParams(params);
			mAdapter = new TencentPagerAdapter(TencentResolverActivity.this, pageCount, mList, intent);
			viewPager.setAdapter(mAdapter);
			
			ImageView[] imageViews = new ImageView[pageCount];
			for (int i = 0; i < pageCount; i++) {
				ImageView imageView = new ImageView(this);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				if (i != 0)
					lp.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
							getResources().getDisplayMetrics());
				imageView.setLayoutParams(lp);
				if (i == 0) {
					imageView.setBackgroundResource(R.drawable.chooserlayout_focused);
				} else {
					imageView.setBackgroundResource(R.drawable.chooserlayout_unfocused);
				}
				
				imageViews[i] = imageView;
				viewPoints.addView(imageView);
				viewPoints.setVisibility(View.VISIBLE);
			}
			
			if (pageCount <= 1 && viewPoints != null) {
				viewPoints.removeAllViews();
				viewPoints.setVisibility(View.GONE);
			}
			
			viewPager.setOnPageChangeListener(new TencentPageChangeListener(pageCount, imageViews));
			
			ap.mView = vg;
			ap.mViewSpacingSpecified = true;
			if (pageCount <= 1 && viewPoints != null) {
				ap.mViewSpacingBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
						getResources().getDisplayMetrics());
			}
			else {
				ap.mViewSpacingBottom = 0;
			}
			ap.mViewSpacingLeft = 0;
			ap.mViewSpacingRight = 0;
			ap.mViewSpacingTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
					getResources().getDisplayMetrics());
		}
		else {
			TextView mTextView = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                 LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			lp.gravity = Gravity.CENTER;
            mTextView.setLayoutParams(lp);
			mTextView.setText(R.string.noApplications);
            ap.mView = mTextView;
        }
		
		setupAlert();
	}

	Drawable getIcon(Resources res, int resId) {
		Drawable result;
		try {
			result = res.getDrawableForDensity(resId, mIconDpi);
		} catch (Resources.NotFoundException e) {
			result = null;
		}

		return result;
	}

	Drawable loadIconForResolveInfo(ResolveInfo ri) {
		Drawable dr;
		try {
			if (ri.resolvePackageName != null && ri.icon != 0) {
				dr = getIcon(mPm.getResourcesForApplication(ri.resolvePackageName),
						ri.activityInfo.applicationInfo.icon);
				if (dr != null) {
					return dr;
				}
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Couldn't find resources for package " + e);
		}
		return ri.activityInfo.applicationInfo.loadIcon(mPm);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (!mRegistered) {
			mPackageMonitor.register(this, getMainLooper(), false);
			mRegistered = true;
		}
		mAdapter.handlePackagesChanged();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mRegistered) {
			mPackageMonitor.unregister();
			mRegistered = false;
		}
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			// This resolver is in the unusual situation where it has been
			// launched at the top of a new task. We don't let it be added
			// to the recent tasks shown to the user, and we need to make sure
			// that each time we are launched we get the correct launching
			// uid (not re-using the same resolver from an old launching uid),
			// so we will now finish ourself since being no longer visible,
			// the user probably can't get back to us.
			if (!isChangingConfigurations()) {
				finish();
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (mAlwaysUseOption) {
			final int checkedPos = mListView.getCheckedItemPosition();
			final boolean enabled = checkedPos != ListView.INVALID_POSITION;
			mLastSelected = checkedPos;
			if (enabled) {
				mListView.setSelection(checkedPos);
			}
		}
	}

	void showAppDetails(ResolveInfo ri) {
		Intent in = new Intent().setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
				.setData(Uri.fromParts("package", ri.activityInfo.packageName, null))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(in);
	}

	private final class DisplayResolveInfo {
		ResolveInfo ri;
		CharSequence displayLabel;
		Drawable displayIcon;
		CharSequence extendedInfo;
		Intent origIntent;
		Bitmap displayBitmap;

		DisplayResolveInfo(ResolveInfo pri, CharSequence pLabel, CharSequence pInfo, Intent pOrigIntent) {
			ri = pri;
			displayLabel = pLabel;
			extendedInfo = pInfo;
			origIntent = pOrigIntent;
		}
	}
	
	public static boolean isSystemApplication(ApplicationInfo applicationInfo) {
        if ((applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 
                || (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return true;
        }
        if(applicationInfo.packageName.contains("com.tencent.tws")){
        	Log.i(TAG, "isSystemApp "+applicationInfo.packageName);
        	return true;
        }
        return false;
    }
	public static CharSequence loadLabel(PackageManager pm, PackageItemInfo ai){
		CharSequence label = ai.loadLabel(pm);
		if(label==null ||label.length()==0||TrimString(label).length()==0){
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	        mainIntent.setPackage(ai.packageName);

	        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
	        if(apps==null||apps.size()==0){
	        	label = ai.packageName;
	        }else{
	        	label = apps.get(0).loadLabel(pm);
	        	if(label==null ||label.length()==0||TrimString(label).length()==0){
	        		label = ai.packageName;
	    		}
	        }
		}
		
		return TrimString(label);
		
	}
	public static CharSequence TrimString(CharSequence label) {
		if(label==null ||label.length()==0){
			return label;
		}
		int start = 0;
		for(int i=0;i<label.length();i++){
			int charToInt = label.charAt(i);
			if(charToInt<'!' ||(charToInt>128 &&charToInt<256)){
				start++;
			}else{
				break;
			}
		}
		return label.subSequence(start, label.length());
	}
	
	private ComponentName getPreferredAComponentNameByIntent(Intent intent){
		Uri queryIntentUri= Uri.withAppendedPath(Uri.parse("content://" + "com.tencent.tws.assistant.permission"),
				"intents_table");
		String action =intent.getAction();
		if(action==null){
			return null;
		}
		String category =null;
		String mimeType = intent.getType();
		String scheme = intent.getScheme();
		Set<String> catSet = intent.getCategories();
		if(catSet!=null){
			for(String cat:catSet){
				if(Intent.CATEGORY_HOME.equals(cat)){
					category = Intent.CATEGORY_HOME;
				}
			}
		}
		Cursor c =null;
		ComponentName cmName =null;
		StringBuilder Selection =new StringBuilder();
		Selection.append("_action='"+action+"'");
		if(category!=null){
			Selection.append(" AND _category='"+category+"'");
		}
		if(mimeType!=null){
			Selection.append(" AND _mimetype='"+mimeType+"'");
		}
		if(scheme!=null){
			Selection.append(" AND _scheme='"+scheme+"'");
		}
		
		try{
			c = getContentResolver().query(queryIntentUri, null, Selection.toString(), null, null);
		
			if (c != null && c.getCount() > 0) {
				while (c.moveToNext()) {
					mCategoryIndex = c.getInt(c.getColumnIndex("_index"));
					
					Log.i("default","mCategoryIndex="+mCategoryIndex);
					break;
				}
			}
		}finally{
			if(c!=null){
				c.close();
			}
		}
		if(mCategoryIndex >=0){
			Uri queryPreferredUri= Uri.withAppendedPath(Uri.parse("content://" + "com.tencent.tws.assistant.permission"),
					"defaultapp");
			Cursor defaultCursor = null;
			String  selection = "_category"+"="+mCategoryIndex+" AND "+
					"_is_default"+"=1";
			
			try{
				defaultCursor = getContentResolver().query(queryPreferredUri, null, selection, null, null);
				if (defaultCursor != null && defaultCursor.getCount() > 0) {
					while (defaultCursor.moveToNext()) {
						String pkgName = defaultCursor.getString(defaultCursor.getColumnIndex("_pkgname"));
						String activity = defaultCursor.getString(defaultCursor.getColumnIndex("_activity"));
						cmName = new ComponentName(pkgName,activity);
						Log.i(TAG,"cmName="+cmName);
						break;
					}
				}
			}finally{
				if(defaultCursor!=null){
					defaultCursor.close();
				}
			}
			
			
		}
		return cmName;
	}
	
	class TencentOnCheckedChangeListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			mAlwaysCheck = isChecked;
		}
		
	}
	
	class TencentPagerAdapter extends PagerAdapter {
		
		private int pagerCount;
		private List<TwsGridView> pagers;
		private Intent mIntent;
		private List<DisplayResolveInfo> mListInfo;
		private Context mContext;
		
		public TencentPagerAdapter(Context context, int pagerCount, List<DisplayResolveInfo> listInfo, Intent intent) {
		    pagers = new ArrayList<TwsGridView>();
			for (int i = 0; i < pagerCount; i++) {
			    TwsGridView gridView = (TwsGridView) View.inflate(context, R.layout.resolverlayout_gridview, null);
			    gridView.setNumColumns((listInfo.size() > (SINGLE_COUNT/2-1)) ? SINGLE_COUNT/2 : listInfo.size());
				gridView.setIsNeedBounce(false);
				gridView.setAdapter(new TencentGridAdapter(i, listInfo, intent));
				gridView.setVerticalScrollBarEnabled(false);
				TwsRippleDrawable rDrawable = TwsRippleUtils.getDefaultDrawable(context);
				rDrawable.setMaxRadius(getResources().getDimensionPixelSize(R.dimen.tws_resolver_item_radius));
		        gridView.setDrawSelectorOnTop(true);
		        gridView.setSelector(rDrawable);
				pagers.add(gridView);
			}
			this.pagerCount = pagerCount;
			this.mIntent = intent;
			this.mListInfo = listInfo;
			mContext = context;
		}

		@Override
		public int getCount() {
			return pagerCount;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
		
		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			TwsGridView view = pagers.get(position);
			view.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                	if (isFinishing()) {
        				return;
        			}
                	ResolveInfo ri = resolveInfoForPosition(position);
                	Intent intent = intentForPosition(position);
                	boolean alwaysCheck = mAlwaysCheck;
                	onIntentSelected(ri, intent, alwaysCheck);
                	finish();
                }
            });
			view.setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					ResolveInfo ri = resolveInfoForPosition(position);
					showAppDetails(ri);
					return true;
				}
			});
			container.addView(view);
			return view;
		}
		
		public ResolveInfo resolveInfoForPosition(int position) {
			return mList.get(position + SINGLE_COUNT*whichPage).ri;
		}

		public Intent intentForPosition(int position) {
			DisplayResolveInfo dri = mList.get(position + SINGLE_COUNT*whichPage);

			Intent intent = new Intent(dri.origIntent != null ? dri.origIntent : mIntent);
			intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
			ActivityInfo ai = dri.ri.activityInfo;
			intent.setComponent(new ComponentName(ai.applicationInfo.packageName, ai.name));
			return intent;
		}
		
		protected void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
			if (mAlwaysUseOption && mOrigResolveList != null) {
				// Build a reasonable intent filter, based on what matched.
				IntentFilter filter = new IntentFilter();

				if (intent.getAction() != null) {
					filter.addAction(intent.getAction());
				}
				Set<String> categories = intent.getCategories();
				if (categories != null) {
					for (String cat : categories) {
						filter.addCategory(cat);
					}
				}
				filter.addCategory(Intent.CATEGORY_DEFAULT);

				int cat = ri.match & IntentFilter.MATCH_CATEGORY_MASK;
				Uri data = intent.getData();
				if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
					String mimeType = intent.resolveType(TencentResolverActivity.this);
					if (mimeType != null) {
						try {
							filter.addDataType(mimeType);
						} catch (IntentFilter.MalformedMimeTypeException e) {
							Log.w("ResolverActivity", e);
							filter = null;
						}
					}
				}
				if (data != null && data.getScheme() != null) {
					// We need the data specification if there was no type,
					// OR if the scheme is not one of our magical "file:"
					// or "content:" schemes (see IntentFilter for the reason).
					if (cat != IntentFilter.MATCH_CATEGORY_TYPE
							|| (!"file".equals(data.getScheme()) && !"content".equals(data.getScheme()))) {
						filter.addDataScheme(data.getScheme());

						// Look through the resolved filter to determine which part
						// of it matched the original Intent.
						Iterator<PatternMatcher> pIt = null;
						if (android.os.Build.VERSION.SDK_INT > 18) {

							pIt = ri.filter.schemeSpecificPartsIterator();
							if (pIt != null) {
								String ssp = data.getSchemeSpecificPart();
								while (ssp != null && pIt.hasNext()) {
									PatternMatcher p = pIt.next();
									if (p.match(ssp)) {
										filter.addDataSchemeSpecificPart(p.getPath(), p.getType());
										break;
									}
								}
							}
						}
						Iterator<IntentFilter.AuthorityEntry> aIt = ri.filter.authoritiesIterator();
						if (aIt != null) {
							while (aIt.hasNext()) {
								IntentFilter.AuthorityEntry a = aIt.next();
								if (a.match(data) >= 0) {
									int port = a.getPort();
									filter.addDataAuthority(a.getHost(), port >= 0 ? Integer.toString(port) : null);
									break;
								}
							}
						}
						pIt = ri.filter.pathsIterator();
						if (pIt != null) {
							String path = data.getPath();
							while (path != null && pIt.hasNext()) {
								PatternMatcher p = pIt.next();
								if (p.match(path)) {
									filter.addDataPath(p.getPath(), p.getType());
									break;
								}
							}
						}
					}
				}

				if (filter != null) {
					final int N = mOrigResolveList.size();
					ComponentName[] set = new ComponentName[N];
					int bestMatch = 0;
					for (int i = 0; i < N; i++) {
						ResolveInfo r = mOrigResolveList.get(i);
						set[i] = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
						if (r.match > bestMatch)
							bestMatch = r.match;
					}
					if (alwaysCheck) {
						getPackageManager().addPreferredActivity(filter, bestMatch, set, intent.getComponent());
					} else {
						if (android.os.Build.VERSION.SDK_INT > 18) {

							try {
								AppGlobals.getPackageManager().setLastChosenActivity(intent,
										intent.resolveTypeIfNeeded(getContentResolver()),
										PackageManager.MATCH_DEFAULT_ONLY, filter, bestMatch, intent.getComponent());
							} catch (RemoteException re) {
								Log.d(TAG, "Error calling setLastChosenActivity\n" + re);
							}
						}
					}
				}
			}

			if (intent != null) {
				startActivity(intent);
			}
		}
		
		public void handlePackagesChanged() {
			final int oldItemCount = getCount();
			rebuildList();
			notifyDataSetChanged();
			final int newItemCount = getCount();
			if (newItemCount == 0) {
				finish();
			}
		}
	}
	
	class TencentPageChangeListener implements OnPageChangeListener {

		private int pagerCount;
		private ImageView[] imageViews;
		
		public TencentPageChangeListener(int pagerCount, ImageView[] imageViews) {
			this.pagerCount = pagerCount;
			this.imageViews = imageViews;
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {

		}

		@Override
        public void onPageSelected(int position) {
            whichPage = position;
            if (imageViews.length <= 1) {
                return;
            }
            for (int i = 0; i < imageViews.length; i++) {
                if (i == position % pagerCount) {
                    imageViews[i].setBackgroundResource(R.drawable.chooserlayout_focused);
                } else {
                    imageViews[i].setBackgroundResource(R.drawable.chooserlayout_unfocused);
                }
            }
        }
	}
	
	class TencentGridAdapter extends BaseAdapter {
		
		private List<DisplayResolveInfo> totalInfo;
		private List<DisplayResolveInfo> gridInfo;
		private Intent mIntent;
		private int mPage;

		public TencentGridAdapter(int page, List<DisplayResolveInfo> listInfo, Intent intent) {
			mIntent = intent;
			mPage = page;
			totalInfo = listInfo;
			gridInfo = new ArrayList<DisplayResolveInfo>();
			for (int i = page * SINGLE_COUNT; i < page * SINGLE_COUNT + SINGLE_COUNT; i++) {
				if (i == listInfo.size())
					break;
				gridInfo.add(listInfo.get(i));
			}
		}

		@Override
		public int getCount() {
			return gridInfo == null ? 0 : gridInfo.size();
		}

		@Override
		public Object getItem(int position) {
			return gridInfo == null ? null : gridInfo.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.resolverlayout_gridview_item, parent, false);
				holder.iconView = (ImageView) convertView.findViewById(R.id.resolverlayout_image);
				holder.textView = (TextView) convertView.findViewById(R.id.resolverlayout_text);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			DisplayResolveInfo info = gridInfo.get(position);
			if (info.displayIcon == null) {
				new LoadIconTask().execute(info);
			}
			if (info.displayBitmap != null) {
				holder.iconView.setImageBitmap(info.displayBitmap);
			}
			holder.textView.setText(info.displayLabel);
			
			return convertView;
		}
		
		private class ViewHolder {
			ImageView iconView;
			TextView textView;
		}
		
		private class LoadIconTask extends AsyncTask<DisplayResolveInfo, Void, DisplayResolveInfo> {
	        @Override
	        protected DisplayResolveInfo doInBackground(DisplayResolveInfo... params) {
	            final DisplayResolveInfo info = params[0];
	            if (info.displayIcon == null) {
	                info.displayIcon = loadIconForResolveInfo(info.ri);
	                Bitmap srcBitmap = ((BitmapDrawable)info.displayIcon).getBitmap();
	                info.displayBitmap = srcBitmap;
	            }
	            return info;
	        }

	        @Override
	        protected void onPostExecute(DisplayResolveInfo info) {
	            notifyDataSetChanged();
	        }
	    }
	}
	
	private void rebuildList() {
		List<ResolveInfo> currentResolveList;

		if (android.os.Build.VERSION.SDK_INT > 18) {

			try {
				mLastChosen = AppGlobals.getPackageManager().getLastChosenActivity(mIntent,
						mIntent.resolveTypeIfNeeded(getContentResolver()), PackageManager.MATCH_DEFAULT_ONLY);
			} catch (RemoteException re) {
				Log.d(TAG, "Error calling getLastChosenActivity\n" + re);
			}
		}
		mList.clear();
		if (mBaseResolveList != null) {
			currentResolveList = mBaseResolveList;
			mOrigResolveList = null;
		} else {
			currentResolveList = mOrigResolveList = mPm.queryIntentActivities(mIntent,
					PackageManager.MATCH_DEFAULT_ONLY
							| (mAlwaysUseOption ? PackageManager.GET_RESOLVED_FILTER : 0));
			// Filter out any activities that the launched uid does not
			// have permission for. We don't do this when we have an
			// explicit
			// list of resolved activities, because that only happens when
			// we are being subclassed, so we can safely launch whatever
			// they gave us.
			if (currentResolveList != null) {
				for (int i = currentResolveList.size() - 1; i >= 0; i--) {
					ActivityInfo ai = currentResolveList.get(i).activityInfo;
					if (currentResolveList.get(i).priority < 0) {
						currentResolveList.remove(i);
					}
					int granted = ActivityManager.checkComponentPermission(ai.permission, mLaunchedFromUid,
							ai.applicationInfo.uid, ai.exported);
					if (granted != PackageManager.PERMISSION_GRANTED) {
						// Access not allowed!
						if (mOrigResolveList == currentResolveList) {
							mOrigResolveList = new ArrayList<ResolveInfo>(mOrigResolveList);
						}
						currentResolveList.remove(i);
					}
				}
			}
		}
		int N;
		if ((currentResolveList != null) && ((N = currentResolveList.size()) > 0)) {
			// Only display the first matches that are either of equal
			// priority or have asked to be default options.
			if (mLastChosen == null) {
				mLastChosen = currentResolveList.get(0);
				for (ResolveInfo info : currentResolveList) {
					if (isSystemApplication(info.activityInfo.applicationInfo)) {
						mLastChosen = info;
						break;
					}
				}
			}
			ResolveInfo r0 = currentResolveList.get(0);
			for (int i = 1; i < N; i++) {
				ResolveInfo ri = currentResolveList.get(i);
				if (DEBUG)
					Log.v("ResolveListActivity", r0.activityInfo.name + "=" + r0.priority + "/"
							+ r0.isDefault + " vs " + ri.activityInfo.name + "=" + ri.priority + "/"
							+ ri.isDefault);
				if (r0.priority != ri.priority || r0.isDefault != ri.isDefault) {
					while (i < N) {
						if (mOrigResolveList == currentResolveList) {
							mOrigResolveList = new ArrayList<ResolveInfo>(mOrigResolveList);
						}
						currentResolveList.remove(i);
						N--;
					}
				}
			}
			if (N > 1) {
				ResolveInfo.DisplayNameComparator rComparator = new ResolveInfo.DisplayNameComparator(mPm);
				Collections.sort(currentResolveList, rComparator);
			}

			// First put the initial items at the top.
			if (mInitialIntents != null) {
				for (int i = 0; i < mInitialIntents.length; i++) {
					Intent ii = mInitialIntents[i];
					if (ii == null) {
						continue;
					}
					ActivityInfo ai = ii.resolveActivityInfo(getPackageManager(), 0);
					if (ai == null) {
						Log.w("ResolverActivity", "No activity found for " + ii);
						continue;
					}
					ResolveInfo ri = new ResolveInfo();
					ri.activityInfo = ai;
					if (ii instanceof LabeledIntent) {
						LabeledIntent li = (LabeledIntent) ii;
						ri.resolvePackageName = li.getSourcePackage();
						ri.labelRes = li.getLabelResource();
						ri.nonLocalizedLabel = li.getNonLocalizedLabel();
						ri.icon = li.getIconResource();
					}
					CharSequence appName = loadLabel(mPm,ri.activityInfo.applicationInfo);
					CharSequence activityName = TrimString(ri.loadLabel(mPm));
					if(!appName.equals(activityName)){
						appName = appName+"("+activityName+")";
					}
					mList.add(new DisplayResolveInfo(ri, appName, null,
							ii));
				}
			}

			// Check for applications with same name and use application name or
			// package name if necessary
			r0 = currentResolveList.get(0);
			int start = 0;
			CharSequence r0Label = loadLabel(mPm, r0.activityInfo.applicationInfo);
			mShowExtended = false;
			for (int i = 1; i < N; i++) {
				if (r0Label == null) {
					r0Label = r0.activityInfo.packageName;
				}
				ResolveInfo ri = currentResolveList.get(i);
				CharSequence riLabel = loadLabel(mPm,ri.activityInfo.applicationInfo);
				if (riLabel == null) {
					riLabel = ri.activityInfo.packageName;
				}
//				if (riLabel.equals(r0Label)) {
//					continue;
//				}
				processGroup(currentResolveList, start, (i - 1), r0, r0Label);
				r0 = ri;
				r0Label = riLabel;
				start = i;
			}
			// Process last group
			processGroup(currentResolveList, start, (N - 1), r0, r0Label);
		}
	}
	
	private void processGroup(List<ResolveInfo> rList, int start, int end, ResolveInfo ro,
            CharSequence roLabel) {
		CharSequence roActivityName = TrimString(ro.loadLabel(mPm));
		if(roLabel!=null&&!roLabel.equals(roActivityName)){
			roLabel = roLabel+"("+roActivityName+")";
		}
        // Process labels from start to i
        int num = end - start+1;
        if (num == 1) {
            // No duplicate labels. Use label for entry at start
            mList.add(new DisplayResolveInfo(ro, roLabel, null, null));
        } else {
            boolean usePkg = false;
            CharSequence startApp = ro.activityInfo.applicationInfo.loadLabel(mPm);
            if (startApp == null) {
                usePkg = true;
            }
            if (!usePkg) {
                // Use HashSet to track duplicates
                HashSet<CharSequence> duplicates =
                    new HashSet<CharSequence>();
                duplicates.add(startApp);
                for (int j = start+1; j <= end ; j++) {
                    ResolveInfo jRi = rList.get(j);
                    CharSequence jApp = jRi.activityInfo.applicationInfo.loadLabel(mPm);
                    if ( (jApp == null) || (duplicates.contains(jApp))) {
                        usePkg = true;
                        break;
                    } else {
                        duplicates.add(jApp);
                    }
                }
                // Clear HashSet for later use
                duplicates.clear();
            }
            for (int k = start; k <= end; k++) {
                ResolveInfo add = rList.get(k);
                if (usePkg) {
                    // Use application name for all entries from start to end-1
                    mList.add(new DisplayResolveInfo(add, roLabel,
                            add.activityInfo.packageName, null));
                } else {
                    // Use package name for all entries from start to end-1
                    mList.add(new DisplayResolveInfo(add, roLabel,
                            add.activityInfo.applicationInfo.loadLabel(mPm), null));
                }
            }
        }
    }
}
