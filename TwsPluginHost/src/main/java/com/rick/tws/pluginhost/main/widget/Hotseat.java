package com.rick.tws.pluginhost.main.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.tws.plugin.content.DisplayItem;
import com.tws.plugin.content.HostDisplayItem;
import com.tws.plugin.manager.PluginManagerHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import qrom.component.log.QRomLog;

public class Hotseat extends LinearLayout implements OnClickListener {
    private final String TAG = "rick_Print_dm:Hotseat";

    public static final String HOST_HOME_FRAGMENT = "host_home_fragment";

    public static final int FRAGMENT_COMPONENT = DisplayItem.TYPE_FRAGMENT;
//    public static final int ACTIVITY_COMPONENT = DisplayItem.TYPE_ACTIVITY;
//    public static final int SERVICE_COMPONENT = DisplayItem.TYPE_SERVICE;

    private ArrayList<OnHotseatClickListener> onListeners = new ArrayList<OnHotseatClickListener>();
    private ArrayList<CellItem> mHomeBottomButtons = new ArrayList<CellItem>();
    private CellItem mFoucsButton = null;
    private static int mAddChildIndex = 0;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(LinearLayout.HORIZONTAL);
    }

    public interface OnHotseatClickListener {
        public void onItemClick(int tagIndex);

        public void onItemClick(int tagIndex, int extras);

        public void updateActionBar(final CellItem.ActionBarInfo actionBarInfo);
    }

    public void addHotseatClickObserver(OnHotseatClickListener onClickListener) {
        if (onListeners == null) {
            QRomLog.e(TAG, "removeHomeBottomTabObserver, mHomeBottomTabCallbacksObserver is null, ignore");
            return;
        }

        if (onListeners.contains(onClickListener)) {
            QRomLog.d(TAG, "mHomeBottomTabCallbacksObserver, mHomeBottomTabCallbacksObserver had it, ignore");
            return;
        }

        onListeners.add(onClickListener);
    }

    public void removeHotseatClickObserver(OnHotseatClickListener onClickListener) {
        if (onListeners == null) {
            QRomLog.e(TAG, "removeHomeBottomTabObserver, mHomeBottomTabCallbacksObserver is null, ignore");
            return;
        }

        if (onListeners.contains(onClickListener)) {
            onListeners.remove(onClickListener);
        }
    }

    // type 1:fragment 2:activity 3:service
    // 专给我的手表等DM固有的Pos位提供的
    public void addOneBottomButton(String classId, String packageName, int componentType, CharSequence text,
                                   Drawable normalBackground, Drawable focusBackground, int textNormalColor, int textFocusColor, int location) {
        CellItem button = new CellItem(getContext());
        button.setActionClass(classId, packageName, componentType);
        button.setText(text);
        button.setNormalBackground(normalBackground);
        button.setFocusBackground(focusBackground);
        button.setTextColorNormal(textNormalColor);
        button.setTextColorFocus(textFocusColor);
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
        button.setLayoutParams(params);
        button.setOnClickListener(this);
        button.setLocation(location);
        button.setTagIndex(mAddChildIndex);
        ++mAddChildIndex;
        addView(button);
        // 这里需要入队列跟My_fragment有些差异，因为fragment的切换需要依赖这个队列
        mHomeBottomButtons.add(button);
    }

    public int addOneBottomButtonForPlugin(final HostDisplayItem hostDisplayItem, int textNormalColor, int textFocusColor,
                                           boolean newInstallPlugin) {
        // 当前显示在Hotseat的内容暂只接收fragment
        if (hostDisplayItem.action_type != DisplayItem.TYPE_FRAGMENT) {
            return -1;
        }

        if (!isEnabledDisplayItem(hostDisplayItem)) {
            QRomLog.e(TAG, "info is illegal(already exists), This will be ignored!");
            return -1;
        }

        CellItem button = new CellItem(getContext());
        button.setActionClass(hostDisplayItem.action_id, hostDisplayItem.pid, hostDisplayItem.action_type);

        final Locale locale = getResources().getConfiguration().locale;
        if ("zh".equals(locale.getLanguage())) {
            if ("HK".equals(locale.getCountry())) {
                button.setText(hostDisplayItem.title_zh_HK);
            } else if ("TW".equals(locale.getCountry())) {
                button.setText(hostDisplayItem.title_zh_TW);
            } else {
                button.setText(hostDisplayItem.title_zh_CN);
            }
        } else {
            button.setText(hostDisplayItem.title_en);
        }

        button.setNormalBackground(PluginManagerHelper.getPluginIcon(hostDisplayItem.normalResName));
        button.setFocusBackground(PluginManagerHelper.getPluginIcon(hostDisplayItem.focusResName));
        button.setTextColorNormal(textNormalColor);
        button.setTextColorFocus(textFocusColor);
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
        button.setLayoutParams(params);
        button.setOnClickListener(this);
        button.setLocation(hostDisplayItem.x);
        button.setVisibility(hostDisplayItem.establishedDependOn ? View.VISIBLE : View.GONE);
        button.setTagIndex(mAddChildIndex);
        ++mAddChildIndex;

        if (null != hostDisplayItem.actionBarDisplayItem) {
            if ("zh".equals(locale.getLanguage())) {
                if ("HK".equals(locale.getCountry())) {
                    button.mActionBarInfo.title = hostDisplayItem.actionBarDisplayItem.title_zh_HK;
                } else if ("TW".equals(locale.getCountry())) {
                    button.mActionBarInfo.title = hostDisplayItem.actionBarDisplayItem.title_zh_TW;
                } else {
                    button.mActionBarInfo.title = hostDisplayItem.actionBarDisplayItem.title_zh_CN;
                }
            } else {
                button.mActionBarInfo.title = hostDisplayItem.actionBarDisplayItem.title_en;
            }

            button.mActionBarInfo.r_type = hostDisplayItem.actionBarDisplayItem.r_action_type;
            button.mActionBarInfo.r_action_id = hostDisplayItem.actionBarDisplayItem.r_action_id;
            button.mActionBarInfo.r_res_type = hostDisplayItem.actionBarDisplayItem.r_res_type;
            button.mActionBarInfo.r_res_normal = hostDisplayItem.actionBarDisplayItem.r_res_normal;
            button.mActionBarInfo.r_res_focus = hostDisplayItem.actionBarDisplayItem.r_res_focus;
        }

        boolean insertRlt = false;
        int index = 1;
        // 注意这里第一个是我的手表 这个位置是不动的
        if (0 < button.getLocation()) {
            for (index = 1; index < mHomeBottomButtons.size(); index++) {
                if (button.getLocation() < mHomeBottomButtons.get(index).getLocation()) {
                    insertRlt = true;
                    mHomeBottomButtons.add(index, button);
                    break;
                }
            }
        }
        if (!insertRlt) {
            mHomeBottomButtons.add(button);
        }

        QRomLog.d(TAG, "addOneBottomButton:" + button.getText() + " add index " + index);
        addView(button, index);

        return index;
    }

    public boolean isEnabledDisplayItem(final HostDisplayItem hostDisplayItem) {
        if (hostDisplayItem == null || TextUtils.isEmpty(hostDisplayItem.pid) || TextUtils.isEmpty(hostDisplayItem.action_id))
            return false;

        for (CellItem item : mHomeBottomButtons) {
            if (hostDisplayItem.pid.equals(item.getPluginPackageName()) && hostDisplayItem.action_id.equals(item.getClassId())) {
                return false;
            }
        }

        return true;
    }

    public int childCount() {
        return mHomeBottomButtons.size();
    }

    // 当前暂时只处理一个，底部Hotseat一个packageName只让放置一个
    public int removePlugin(String packageName) {
        if (TextUtils.isEmpty(packageName))
            return -1;

        Iterator<CellItem> iter = mHomeBottomButtons.iterator();
        boolean foucsChanged = false;
        CellItem removeItem = null, preItem = null;
        while (iter.hasNext()) {
            CellItem button = iter.next();
            if (packageName.equals(button.getPluginPackageName())) {
                if (mFoucsButton == button) {
                    foucsChanged = true;
                    mFoucsButton = preItem;
                }

                removeItem = button;
                // 从Hotseat界面移除
                removeView(button);
                // 从缓存数组里面移除
                mHomeBottomButtons.remove(button);

                if (foucsChanged) {
                    break; // 需要退出遍历执行切换焦点流程
                } else {
                    return button.getTagIndex();
                }
            }
            preItem = button;
        }

        if (foucsChanged && onListeners != null) {
            for (OnHotseatClickListener listener : onListeners) {
                listener.updateActionBar(mFoucsButton.mActionBarInfo);
                listener.onItemClick(mFoucsButton == null ? 0 : mFoucsButton.getTagIndex());
            }
        }

        return removeItem == null ? -1 : removeItem.getTagIndex();
    }

    @Override
    public void onClick(View view) {
        if (view instanceof CellItem) {
            if (view == mFoucsButton)
                return;

            setFocus((CellItem) view);
            ((CellItem) view).setHighlight(false);

            if (onListeners != null && mFoucsButton != null) {
                for (OnHotseatClickListener listener : onListeners) {
                    listener.updateActionBar(mFoucsButton.mActionBarInfo);
                    listener.onItemClick(mFoucsButton.getTagIndex());
                }
            }
        } else {
            QRomLog.d(TAG, "onClick view=" + view);
        }
    }

    public String getFouceTabClassId() {
        return mFoucsButton == null ? "" : mFoucsButton.getClassId();
    }

    public int getFouceTagIndex() {
        return mFoucsButton == null ? 0 : mFoucsButton.getTagIndex();
    }

    private void setFocus(final CellItem button) {
        QRomLog.d(TAG, "call setFocus HomeBottomButton");
        if (null == button) {
            Exception here = new Exception("call setFouce on null object");
            here.fillInStackTrace();
            QRomLog.d(TAG, "call setFouce on null object", here);
            return;
        }

        boolean matched = false;
        for (CellItem btn : mHomeBottomButtons) {
            if (btn.equals(button)) {
                button.setFocus(true);
                mFoucsButton = button;
                matched = true;
            } else {
                btn.setFocus(false);
            }
        }

        if (!matched) {
            mFoucsButton = null;
            QRomLog.d(TAG, "setFouce:HomeBottomButton - classId is " + button.getClassId()
                    + " no matching to the right!!!");
            printHomeBottomButtonsInfo();
        }
    }

    /**
     * @return FoucsButton TagIndex
     */
    public int setFocusIndex(int arrayIndex) {
        if (mHomeBottomButtons.size() < 1)
            return -1;

        int focusTagIndex = -1;
        mFoucsButton = null;

        if (arrayIndex < 0) {
            arrayIndex = 0;
        } else if (mHomeBottomButtons.size() <= arrayIndex) {
            arrayIndex = mHomeBottomButtons.size() - 1;
        }

        for (int index = 0; index < mHomeBottomButtons.size(); index++) {
            if (index == arrayIndex) {
                mFoucsButton = mHomeBottomButtons.get(index);
                mFoucsButton.setFocus(true);
                focusTagIndex = mFoucsButton.getTagIndex();
            } else {
                mHomeBottomButtons.get(index).setFocus(false);
            }
        }

        if (mFoucsButton != null && onListeners != null) {
            for (OnHotseatClickListener listener : onListeners) {
                listener.updateActionBar(mFoucsButton.mActionBarInfo);
            }
        }

        return focusTagIndex;
    }

    private void printHomeBottomButtonsInfo() {
        QRomLog.d(TAG, "===========================printHomeBottomButtonsInfo begin===========================");
        int index = 0;
        for (CellItem button : mHomeBottomButtons) {
            QRomLog.d(TAG, "[" + index + "] classId:" + button.getClassId() + " type is " + button.getComponentType());
        }
        QRomLog.d(TAG, "===========================printHomeBottomButtonsInfo end===========================");
    }

    public CellItem.ComponentName getComponentNameByTagIndex(int tagIndex) {
        if (mFoucsButton == null) {
            setFocusIndex(0);
        }

        if (mFoucsButton != null && mFoucsButton.getTagIndex() == tagIndex) {
            return mFoucsButton.getComponentName();
        }

        for (CellItem item : mHomeBottomButtons) {
            if (item.getTagIndex() == tagIndex) {
                return mFoucsButton.getComponentName();
            }
        }

        return null;
    }

    public int getPosByClassId(String classId) {
        if (TextUtils.isEmpty(classId))
            return 0;

        for (int index = 0; index < mHomeBottomButtons.size(); index++) {
            if (classId.equals(mHomeBottomButtons.get(index).getClassId()))
                return index;
        }

        return 0;
    }

    public void switchToFragment(String classId, int extras) {
        for (CellItem item : mHomeBottomButtons) {
            if (item.getClassId().equals(classId)) {
                setFocus(item);

                if (onListeners != null && mFoucsButton != null) {
                    for (OnHotseatClickListener listener : onListeners) {
                        listener.updateActionBar(mFoucsButton.mActionBarInfo);
                        listener.onItemClick(mFoucsButton.getTagIndex(), extras);
                    }
                }
                return;
            }
        }
    }

    public void setHighlightCellItem(String classId, boolean needHighlight) {
        for (CellItem item : mHomeBottomButtons) {
            if (item.getClassId().equals(classId)) {
                item.setHighlight(needHighlight);
                return;
            }
        }
    }

    public void unEstablishedDependOnForPlugin(String pid) {
        if (TextUtils.isEmpty(pid))
            return;

        for (CellItem item : mHomeBottomButtons) {
            if (pid.equals(item.getPluginPackageName())) {
                item.setVisibility(View.GONE);
            }
        }
    }

    public void establishedDependOnForPlugin(String pid) {
        if (TextUtils.isEmpty(pid))
            return;

        for (CellItem item : mHomeBottomButtons) {
            if (pid.equals(item.getPluginPackageName())) {
                item.setVisibility(View.VISIBLE);
            }
        }
    }
}
