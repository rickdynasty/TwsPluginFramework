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
import java.util.Map;
import java.util.Set;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.assistant.support.v4.view.PagerAdapter;
import com.tencent.tws.assistant.support.v4.view.ViewPager;
import com.tencent.tws.assistant.support.v4.view.ViewPager.OnPageChangeListener;
import com.tencent.tws.assistant.utils.BitmapUtil;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.utils.BitmapUtil.IconAnalyzedResult;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.CheckBox;
import com.tencent.tws.assistant.widget.TwsGridView;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.sharelib.R;

/**
 * This activity is displayed when the system attempts to start an Intent for
 * which there is more than one matching activity, allowing the user to decide
 * which to go to.  It is not normally used directly by application developers.
 */
public class TwsResolverActivity extends AlertActivity {
    private CheckBox mAlwaysCheck;
    private PackageManager mPm;
    
	private List<DisplayResolveInfo> mList;
	private int whichPage;
	private static final int SINGLE_LINE_HEIGHT = 98;
	private static final int TWO_LINE_HEIGHT = 214;
	
	private String mIntentType;
	private String mAction;
	
	private SharedPreferences mResolverSP;
	private Bundle mDataBundle;
	
	private static final int SINGLE_COUNT = 8;
	private static final int TOP_COUNT = SINGLE_COUNT-1;
	private String[] mTopReNames;
	
	private boolean mBothQQFlag;
	
    private Intent makeMyIntent() {
        Intent intent = new Intent(getIntent());
        // The resolver activity is set to be hidden from recent tasks.
        // we don't want this attribute to be propagated to the next activity
        // being launched.  Note that if the original Intent also had this
        // flag set, we are now losing it.  That should be a very rare case
        // and we can live with this.
        intent.setFlags(intent.getFlags()&~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onCreate(savedInstanceState, makeMyIntent(),
			/*NANJI-START::change::haoranma::2012-11-16*/
                getResources().getText(R.string.whichApplication),
                /*NANJI-END::change::haoranma::2012-11-16*/
                null, null, false);
    }

    
    
    protected void onCreate(Bundle savedInstanceState, Intent intent,
            CharSequence title, Intent[] initialIntents, List<ResolveInfo> rList,
            boolean alwaysUseOption) {
        super.onCreate(savedInstanceState);
        setBottomDialog(true);
        mPm = getPackageManager();
        intent.setComponent(null);
        mIntentType = intent.getType();
        mAction = intent.getAction();
        mDataBundle = intent.getExtras();
        AlertController.AlertParams ap = mAlertParams;

        ap.mTitle = "";

        if (alwaysUseOption) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mAlwaysCheck = (CheckBox)ap.mView.findViewById(R.id.alwaysUse);
            mAlwaysCheck.setText(R.string.alwaysUse);
        }
        
		if (rList == null) {
            rList = mPm.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY
                    | (mAlwaysCheck != null ? PackageManager.GET_RESOLVED_FILTER : 0));
        }
		int N;
        if ((rList != null) && ((N = rList.size()) > 0)) {
            // Only display the first matches that are either of equal
            // priority or have asked to be default options.
            ResolveInfo r0 = rList.get(0);
            for (int i=1; i<N; i++) {
                ResolveInfo ri = rList.get(i);
                if (false) Log.v(
                    "ResolveListActivity",
                    r0.activityInfo.name + "=" +
                    r0.priority + "/" + r0.isDefault + " vs " +
                    ri.activityInfo.name + "=" +
                    ri.priority + "/" + ri.isDefault);
               if (r0.priority != ri.priority ||
                    r0.isDefault != ri.isDefault) {
                    while (i < N) {
                        rList.remove(i);
                        N--;
                    }
                }
            }
            if (N > 1) {
                ResolveInfo.DisplayNameComparator rComparator =
                        new ResolveInfo.DisplayNameComparator(mPm);
                
                if (isConfigExist()) {
                	mResolverSP = getSP();
                	List<ResolveInfo> tempList = new ArrayList<ResolveInfo>(rList);
                	rList.clear();
                	for (int i = 0; i < tempList.size(); i++) {
                		ResolveInfo info = tempList.get(i);
                		if (mResolverSP.contains(info.activityInfo.name)) {
                			if (mResolverSP.getBoolean(info.activityInfo.name, false)) {
                				rList.add(info);
                			}
                		}
                		else {
                			rList.add(info);
                		}
                	}
                	N = rList.size();
                	sortResolver(rList, rComparator);
                }
                
                else {
                	N = rList.size();
                	sortResolver(rList, rComparator);
                	if (N > TOP_COUNT) {
                		List<ResolveInfo> tempList = new ArrayList<ResolveInfo>(rList);
                		rList.clear();
                		for (int i = 0; i < TOP_COUNT; i++) {
                			rList.add(tempList.get(i));
                		}
                		tempList.clear();
                		N = rList.size();
                	}
                }
                
            }
            
            mList = new ArrayList<DisplayResolveInfo>();
            
            // First put the initial items at the top.
            if (initialIntents != null) {
                for (int i=0; i<initialIntents.length; i++) {
                    Intent ii = initialIntents[i];
                    if (ii == null) {
                        continue;
                    }
                    ActivityInfo ai = ii.resolveActivityInfo(
                            getPackageManager(), 0);
                    if (ai == null) {
                        Log.w("TwsResolverActivity", "No activity found for "
                                + ii);
                        continue;
                    }
                    ResolveInfo ri = new ResolveInfo();
                    ri.activityInfo = ai;
                    if (ii instanceof LabeledIntent) {
                        LabeledIntent li = (LabeledIntent)ii;
                        ri.resolvePackageName = li.getSourcePackage();
                        ri.labelRes = li.getLabelResource();
                        ri.nonLocalizedLabel = li.getNonLocalizedLabel();
                        ri.icon = li.getIconResource();
                    }
                	mList.add(new DisplayResolveInfo(ri,
                			ri.loadLabel(getPackageManager()), null, ii));
                }
            }
            
            // Check for applications with same name and use application name or
            // package name if necessary
            if (rList.size() != 0) {
            	r0 = rList.get(0);
            	int start = 0;
            	CharSequence r0Label =  r0.loadLabel(mPm);
            	N = rList.size();
            	for (int i = 1; i < N; i++) {
            		if (r0Label == null) {
            			r0Label = r0.activityInfo.packageName;
            		}
            		ResolveInfo ri = rList.get(i);
            		CharSequence riLabel = ri.loadLabel(mPm);
            		if (riLabel == null) {
            			riLabel = ri.activityInfo.packageName;
            		}
            		if (riLabel.equals(r0Label)) {
            			continue;
            		}
            		processGroup(rList, start, (i-1), r0, r0Label);
            		r0 = ri;
            		r0Label = riLabel;
            		start = i;
            	}
            	// Process last group
            	processGroup(rList, start, (N-1), r0, r0Label);
            }
        }

        int pageCount = 0;
        
        if (mList != null && mDataBundle != null && (mAction.equals(Intent.ACTION_SEND) || mAction.equals(Intent.ACTION_SEND_MULTIPLE))) {
        	mList.add(new DisplayResolveInfo(null, null, null, null));
        }
        int itemCount = mList == null ? 0 : mList.size();

        if (itemCount > 0) {
            if (itemCount % SINGLE_COUNT == 0) {
                pageCount = itemCount / SINGLE_COUNT;
            } else {
                pageCount = itemCount / SINGLE_COUNT + 1;
            }

            final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View vg = inflater.inflate(R.layout.chooserlayout, null);
            ViewPager viewPager = (ViewPager) vg.findViewById(R.id.chooserviewpager);
            LinearLayout viewPoints = (LinearLayout) vg.findViewById(R.id.chooserpoints);

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
            viewPager.setAdapter(new TwsPagerAdapter(TwsResolverActivity.this, pageCount, mList, intent));

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
            viewPager.setOnPageChangeListener(new TwsPageChangeListener(pageCount, imageViews));
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
            ap.mViewSpacingTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18,
                    getResources().getDisplayMetrics());
        }else{

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

    protected void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
        if (alwaysCheck) {
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

            int cat = ri.match&IntentFilter.MATCH_CATEGORY_MASK;
            Uri data = intent.getData();
            if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
                String mimeType = intent.resolveType(this);
                if (mimeType != null) {
                    try {
                        filter.addDataType(mimeType);
                    } catch (IntentFilter.MalformedMimeTypeException e) {
                        Log.w("TwsResolverActivity", e);
                        filter = null;
                    }
                }
            }
            if (data != null && data.getScheme() != null) {
                // We need the data specification if there was no type,
                // OR if the scheme is not one of our magical "file:"
                // or "content:" schemes (see IntentFilter for the reason).
                if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                        || (!"file".equals(data.getScheme())
                                && !"content".equals(data.getScheme()))) {
                    filter.addDataScheme(data.getScheme());
    
                    // Look through the resolved filter to determine which part
                    // of it matched the original Intent.
                    Iterator<IntentFilter.AuthorityEntry> aIt = ri.filter.authoritiesIterator();
                    if (aIt != null) {
                        while (aIt.hasNext()) {
                            IntentFilter.AuthorityEntry a = aIt.next();
                            if (a.match(data) >= 0) {
                                int port = a.getPort();
                                filter.addDataAuthority(a.getHost(),
                                        port >= 0 ? Integer.toString(port) : null);
                                break;
                            }
                        }
                    }
                    Iterator<PatternMatcher> pIt = ri.filter.pathsIterator();
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
                final int N = mList.size();
                ComponentName[] set = new ComponentName[N];
                int bestMatch = 0;
                for (int i=0; i<N; i++) {
                    ResolveInfo r = mList.get(i).ri;
                    set[i] = new ComponentName(r.activityInfo.packageName,
                            r.activityInfo.name);
                    if (r.match > bestMatch) bestMatch = r.match;
                }
                getPackageManager().addPreferredActivity(filter, bestMatch, set,
                        intent.getComponent());
            }
        }

        if (intent != null) {
        	try {
        		startActivity(intent);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    }

    class TwsPagerAdapter extends PagerAdapter {
		
		private int pagerCount;
		private List<TwsGridView> pagers;
		private Intent mIntent;
		private List<DisplayResolveInfo> mListInfo;
		
		public TwsPagerAdapter(Context context, int pagerCount, List<DisplayResolveInfo> listInfo, Intent intent) {
		    pagers = new ArrayList<TwsGridView>();
			for (int i = 0; i < pagerCount; i++) {
			    TwsGridView gridView = (TwsGridView) View.inflate(context, R.layout.chooserlayout_gridview, null);
			    gridView.setNumColumns((listInfo.size() > (SINGLE_COUNT/2-1)) ? SINGLE_COUNT/2 : listInfo.size());
				gridView.setIsNeedBounce(false);
				gridView.setAdapter(new TwsGridAdapter(i, listInfo, intent));
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
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			TwsGridView view = pagers.get(position);
			view.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                	
                	if (mDataBundle != null && (mAction.equals(Intent.ACTION_SEND) || mAction.equals(Intent.ACTION_SEND_MULTIPLE)) && (pagerCount == (whichPage+1) && position == (mListInfo.size() % SINGLE_COUNT-1)) 
                			|| (mListInfo.size() % SINGLE_COUNT == 0 && (position == SINGLE_COUNT - 1) && whichPage == mListInfo.size()/SINGLE_COUNT-1)) {
                		Intent intent = new Intent();
                		intent.setAction("android.intent.action.resolversettings");
                		intent.putExtra("intentType", mIntentType);
                		intent.putExtra("topReNames", mTopReNames);
                		intent.putExtra("intentRawAction", mAction);
                		intent.putExtras(mDataBundle);
                		try {
                			startActivity(intent);
                		} catch (Exception e) {
                			Toast.makeText(TwsResolverActivity.this, "Not found", Toast.LENGTH_SHORT).show();
                			e.printStackTrace();
                		}
                		finish();
                	}
                	else {
                		ResolveInfo ri = resolveInfoForPosition(position);
                		Intent intent = intentForPosition(position);
                		boolean alwaysCheck = (mAlwaysCheck != null && mAlwaysCheck.isChecked());
                		onIntentSelected(ri, intent, alwaysCheck);
                		finish();
                	}
                }
            });
			container.addView(view);
			return view;
		}
		
		public ResolveInfo resolveInfoForPosition(int position) {
            if (mList == null) {
                return null;
            }
            return mList.get(position + SINGLE_COUNT*whichPage).ri;
        }
        
        public Intent intentForPosition(int position) {
          if (mList == null) {
              return null;
          }

          DisplayResolveInfo dri = mList.get(position + SINGLE_COUNT*whichPage);
          
          Intent intent = new Intent(dri.origIntent != null
                  ? dri.origIntent : mIntent);
          intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
                  |Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
          ActivityInfo ai = dri.ri.activityInfo;
          intent.setComponent(new ComponentName(
                  ai.applicationInfo.packageName, ai.name));
          return intent;
        }
	}

	
	
	
	
	class TwsPageChangeListener implements OnPageChangeListener {
		
		private int pagerCount;
		private ImageView[] imageViews;
		
		public TwsPageChangeListener(int pagerCount, ImageView[] imageViews) {
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

	
	
	
	
	class TwsGridAdapter extends BaseAdapter {

		private List<DisplayResolveInfo> totalInfo;
		private List<DisplayResolveInfo> gridInfo;
		private Intent mIntent;
		private int mPage;

		public TwsGridAdapter(int page, List<DisplayResolveInfo> listInfo, Intent intent) {
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
		public View getView(final int position, View convertView, ViewGroup parent) {

			ViewHolder holder = null;
			
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.chooserlayout_gridview_item, parent, false);
				holder.iconView = (ImageView) convertView.findViewById(R.id.chooserlayout_image);
				holder.textView = (TextView) convertView.findViewById(R.id.chooserlayout_text);
				holder.settingView = (LinearLayout) convertView.findViewById(R.id.chooserlayout_setting);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			if (mDataBundle != null && (mAction.equals(Intent.ACTION_SEND) || mAction.equals(Intent.ACTION_SEND_MULTIPLE)) && ((mPage == totalInfo.size() / SINGLE_COUNT) && position == getCount()-1) 
				|| (totalInfo.size() % SINGLE_COUNT == 0 && position == getCount()-1 && mPage == (totalInfo.size()/SINGLE_COUNT-1))) {
				holder.iconView.setVisibility(View.GONE);
				holder.textView.setVisibility(View.GONE);
				holder.settingView.setVisibility(View.VISIBLE);
			}
			else {
				holder.iconView.setVisibility(View.VISIBLE);
				holder.textView.setVisibility(View.VISIBLE);
				holder.settingView.setVisibility(View.GONE);
				DisplayResolveInfo info = gridInfo.get(position);
				if (info.displayIcon == null) {
					if (mDataBundle!= null 
							&& "true".equals(mDataBundle.getString("screenshot")) 
								&& android.os.Build.VERSION.SDK_INT < 19) {
						info.displayIcon = loadIconForResolveInfo(info.ri);
						info.displayBitmap = ((BitmapDrawable)info.displayIcon).getBitmap();
						info.displayIcon = null;
						
					}
					else {
						new LoadIconTask().execute(info);
					}
				}
				if (info.displayBitmap != null) {
					for (int i = 0; i < TwsResolverHiIcon.HiIconName.length; i++) {
						if (info.ri.activityInfo.name.equals(TwsResolverHiIcon.HiIconName[i])) {
							holder.iconView.setImageResource(TwsResolverHiIcon.HiIconRes[i]);
							break;
						}
						holder.iconView.setImageBitmap(info.displayBitmap);
					}
				}
				holder.textView.setText(info.displayLabel);
			}
			
			return convertView;
		}
		
		private class ViewHolder {
			ImageView iconView;
			TextView textView;
			LinearLayout settingView;
		}
		
		private class LoadIconTask extends AsyncTask<DisplayResolveInfo, Void, DisplayResolveInfo> {
	        @Override
	        protected DisplayResolveInfo doInBackground(DisplayResolveInfo... params) {
	            final DisplayResolveInfo info = params[0];
	            if (info.displayIcon == null) {
	                info.displayIcon = loadIconForResolveInfo(info.ri);
	                Bitmap srcBitmap = ((BitmapDrawable)info.displayIcon).getBitmap();
	                IconAnalyzedResult analyzedResult = BitmapUtil.analyzeBitmap1(srcBitmap);
	                info.displayBitmap = BitmapUtil.processOrdinaryIcon(TwsResolverActivity.this, srcBitmap, null, true, analyzedResult, false, null);
	            }
	            return info;
	        }

	        @Override
	        protected void onPostExecute(DisplayResolveInfo info) {
	            notifyDataSetChanged();
	        }
	    }
	}
	
	private final class DisplayResolveInfo {
        ResolveInfo ri;
        CharSequence displayLabel;
        Drawable displayIcon;
        Bitmap displayBitmap;
        CharSequence extendedInfo;
        Intent origIntent;

        DisplayResolveInfo(ResolveInfo pri, CharSequence pLabel,
                CharSequence pInfo, Intent pOrigIntent) {
            ri = pri;
            if (pri != null) {
            	if (pri.activityInfo.packageName.equals("com.tencent.mobileqq") && mBothQQFlag) {
            		if (pri.activityInfo.name.equals("com.tencent.mobileqq.activity.JumpActivity")) {
            			displayLabel = pLabel + "(QQ Mob)";
            		}
            		else if (pri.activityInfo.name.equals("com.tencent.mobileqq.activity.qfileJumpActivity")) {
            			displayLabel = pLabel + "(Mob)";
            		}
            		else {
            			displayLabel = pLabel;
            		}
            	}
            	else if (pri.activityInfo.packageName.equals("com.tencent.qqlite") && mBothQQFlag) {
            		if (pri.activityInfo.name.equals("com.tencent.mobileqq.activity.JumpActivity")) {
            			displayLabel = pLabel + "(QQ Lite)";
            		}
            		else if (pri.activityInfo.name.equals("com.tencent.mobileqq.activity.qfileJumpActivity")) {
            			displayLabel = pLabel + "(Lite)";
            		}
            		else {
            			displayLabel = pLabel;
            		}
            	}
            	else {
            		displayLabel = pLabel;
            	}
            }
            else {
            	displayLabel = pLabel;
            }
            extendedInfo = pInfo;
            origIntent = pOrigIntent;
        }
    }
	
	private void processGroup(List<ResolveInfo> rList, int start, int end, ResolveInfo ro,
            CharSequence roLabel) {
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
	
	Drawable loadIconForResolveInfo(ResolveInfo ri) {
        Drawable dr;
        try {
            if (ri.resolvePackageName != null && ri.icon != 0) {
                dr = getIcon(mPm.getResourcesForApplication(ri.resolvePackageName), ri.icon);
                if (dr != null) {
                    return dr;
                }
            }
            final int iconRes = ri.getIconResource();
            if (iconRes != 0) {
                dr = getIcon(mPm.getResourcesForApplication(ri.activityInfo.packageName), iconRes);
                if (dr != null) {
                    return dr;
                }
            }
        } catch (NameNotFoundException e) {
        }
        return ri.loadIcon(mPm);
    }
	
	Drawable getIcon(Resources res, int resId) {
        Drawable result;
        try {
            result = res.getDrawableForDensity(resId, ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getLauncherLargeIconDensity());
        } catch (Resources.NotFoundException e) {
            result = null;
        }

        return result;
    }

	public boolean isConfigExist() {
		Context context = null;
		try {
			context = createPackageContext("com.android.settings", CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return context.getSharedPrefsFile("resolver_config").exists();
	}
	
	public SharedPreferences getSP() {
		Context context = null;
		try {
			context = createPackageContext("com.android.settings", CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		SharedPreferences sp = context.getSharedPreferences("resolver_config", MODE_WORLD_READABLE+MODE_WORLD_WRITEABLE+MODE_MULTI_PROCESS);
		return sp;
	}

	private void sortResolver(List<ResolveInfo> list, ResolveInfo.DisplayNameComparator comparator) {
		List<ResolveInfo> tempList = new ArrayList<ResolveInfo>(list);
		java.util.HashMap<Integer, ResolveInfo> topMap = new java.util.HashMap<Integer, ResolveInfo>();
        List<ResolveInfo> topList = new ArrayList<ResolveInfo>();
        List<ResolveInfo> restList = new ArrayList<ResolveInfo>();
        list.clear();
        List<String> tempPackageList = new ArrayList<String>();
        for (ResolveInfo info : tempList) {
        	tempPackageList.add(info.activityInfo.packageName);
        }
        
        if (tempPackageList.contains("com.tencent.qqlite") && tempPackageList.contains("com.tencent.mobileqq")) {
        	mBothQQFlag = true;
        }
        
        for (ResolveInfo info : tempList) {
        	if (mBothQQFlag) {
        		if (info.activityInfo.name.equals("com.tencent.mm.ui.tools.ShareToTimeLineUI")) {
        			topMap.put(TOP_COUNT-7, info);
        		}
        		else if (info.activityInfo.name.equals("com.tencent.mm.ui.tools.ShareImgUI")) {
        			topMap.put(TOP_COUNT-6, info);
        		}
        		else if (info.activityInfo.name.equals("com.tencent.mobileqq.activity.JumpActivity") && info.activityInfo.packageName.equals("com.tencent.mobileqq")) {
        			topMap.put(TOP_COUNT-5, info);
        		}
        		else if (info.activityInfo.name.equals("com.tencent.mobileqq.activity.JumpActivity") && info.activityInfo.packageName.equals("com.tencent.qqlite")) {
        			topMap.put(TOP_COUNT-4, info);
        		}
        		else if (info.activityInfo.name.equals("com.sina.weibo.composerinde.ComposerDispatchActivity")) {
        			topMap.put(TOP_COUNT-3, info);
        		}
        		else if (info.activityInfo.name.equals("com.meitu.mtxx.img.IMGMainActivity")) {
        			topMap.put(TOP_COUNT-2, info);
        		}
        		else if (info.activityInfo.name.equals("com.android.bluetooth.opp.BluetoothOppLauncherActivity")) {
        			topMap.put(TOP_COUNT-1, info);
        		}
        		else {
        			restList.add(info);
        		}
        	}
        	else {
        		if (info.activityInfo.name.equals("com.tencent.mm.ui.tools.ShareToTimeLineUI")) {
        			topMap.put(TOP_COUNT-7, info);
        		}
        		else if (info.activityInfo.name.equals("com.tencent.mm.ui.tools.ShareImgUI")) {
        			topMap.put(TOP_COUNT-6, info);
        		}
        		else if (info.activityInfo.name.equals("com.tencent.mobileqq.activity.JumpActivity")) {
        			topMap.put(TOP_COUNT-5, info);
        		}
        		else if (info.activityInfo.name.equals("com.sina.weibo.composerinde.ComposerDispatchActivity")) {
        			topMap.put(TOP_COUNT-4, info);
        		}
        		else if (info.activityInfo.name.equals("com.qzonex.module.operation.ui.QZonePublishMoodActivity")) {
        			topMap.put(TOP_COUNT-3, info);
        		}
        		else if (info.activityInfo.name.equals("com.meitu.mtxx.img.IMGMainActivity")) {
        			topMap.put(TOP_COUNT-2, info);
        		}
        		else if (info.activityInfo.name.equals("com.android.bluetooth.opp.BluetoothOppLauncherActivity")) {
        			topMap.put(TOP_COUNT-1, info);
        		}
        		else {
        			restList.add(info);
        		}
        	}
        }
        Collections.sort(restList, comparator);
        
        for (int i = 0; i < TOP_COUNT; i++) {
        	if (topMap.get(i) != null) {
        		topList.add(topMap.get(i));
        	}
        }
        list.addAll(topList);
        list.addAll(restList);
        
        tempPackageList.clear();
        restList.clear();
        topList.clear();
        topMap.clear();
        tempList.clear();

        int listSize = list.size();
        
        mTopReNames = new String[listSize > TOP_COUNT ? TOP_COUNT : listSize];
        for (int i = 0; i < mTopReNames.length; i++) {
        	mTopReNames[i] = list.get(i).activityInfo.name;
        }
	}
}

