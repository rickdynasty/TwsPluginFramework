package qrom.component.log;

import qrom.component.log.impl.QRomLogReceiverImpl;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 用于接收和处理Log相关广播消息
 *
 */
public class QRomLogReceiver extends BroadcastReceiver {

    public final static String ACTION_FORCE_LOG = "qrom.intent.action.FORCE_LOG";

    public final static String ACTION_TRACE_LOG = "qrom.intent.action.TRACE_LOG";

    public final static String FORCE_LOG_FLAG = "FORCE_LOG_FLAG";

    public final static String TRACE_LOG_FLAG = "TRACE_LOG_FLAG";

    private QRomLogReceiverImpl mReceiver = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mReceiver == null) {
            mReceiver = new QRomLogReceiverImpl();
        }
        mReceiver.onReceive(context, intent);
    }

}
