/**
 * RemoteViewsAdapter class wrapper, public some non-public methods so that can be accessed from com.tencent.tws.assistant.widget
 * hendysu, 2013-05-28
 */

// tws-start fix hide class problem in android4.4::2014-07-19
package com.tencent.tws.assistant.widget;
// tws-end fix hide class problem in android4.4::2014-07-19

import android.content.Context;
import android.content.Intent;

public class RemoteViewsAdapterWrapper extends RemoteViewsAdapter {

    // public constructor
    public RemoteViewsAdapterWrapper(Context context, Intent intent, 
		RemoteAdapterConnectionCallback callback) {
		super(context, intent, callback);
	}

    // public superNotifyDataSetChanged
    public void superNotifyDataSetChanged() {
        super.superNotifyDataSetChanged();
    }
}