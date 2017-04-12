package android.app;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Downloads;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * TOS扩展的DownloadManager，主要支持暂停、继续等功能
 *
 */
public class TwsDownloadManager extends DownloadManager {
    
    /**
     * download provider的authority
     */
    public static final String AUTHORITY = "downloads";
    
    /**
     * 广播任务删除
     */
    public static final String ACTION_DOWNLOAD_DELETED = "com.tencent.intent.action.DOWNLOAD_DELETED";
    
    /**
     * 广播任务暂停
     */
    public static final String ACTION_DOWNLOAD_PAUSED = "com.tencent.intent.action.DOWNLOAD_PAUSED";
    
    /**
     * 广播任务继续
     */
    public static final String ACTION_DOWNLOAD_RESUMED = "com.tencent.intent.action.DOWNLOAD_RESUMED";
    
    /**
     * 广播任务更新
     */
    public static final String ACTION_DOWNLOAD_UPDATED = "com.tencent.intent.action.DOWNLOAD_UPDATED";
    
    /**
     * 任务的总大小
     */
    public static final String EXTRA_DOWNLOAD_TOTAL_SIZE = "extra_download_total_size";
    
    /**
     * 任务已经接受到的大小
     */
    public static final String EXTRA_DOWNLOAD_RECEIVED_SIZE = "extra_download_received_size";
    
    /**
     * 当前下载速度
     */
    public static final String EXTRA_DOWNLOAD_SPEED = "extra_download_speed";
    
    /**
     * 定制UI的Activity 名
     */
    public static final ComponentName CUSTOMIZED_DOWNLOAD_ACTIVITY = new ComponentName("com.tencent.tws.assistant.download", "com.tencent.tws.assistant.download.DownloadCustomerActivity");
    
    /**
     * 定制UI的title
     */
    public static final String CUSTOMIZED_TITLE_EXTRA = "customer_title_extra";
    
    
    /**
     * 应用设置的图标icon
     */
    public static final String COLUMN_TASK_ICON = "task_icon_url";
    
    /**
     * 任务开始时间
     */
    public static final String COLUMN_TASK_CREATE_TIME = "task_create_time";
    
    /**
     * 下载速度
     */
    public static final String COLUMN_DOWNLOADING_SPEED = "downloading_speed";
    
    /**
     * 删除任务时是否同时删除文件
     */
    public static final String COLUMN_DELETE_FILE_WHEN_DELETE_TASK = "delelte_file";
    
    /**
     * 是否广播进度、暂停、继续、删除
     */
    public static final String COLUMN_NEED_BROADCAST = "need_broadcast";
    
    /**
     * 用于自定义界面的排序
     */
    public static final String COLUMN_SORT_DOWNLOAD_STATUS = "sort_status";
    
    /**
     * 下载完成后，不要显示在DownloadUi中
     */
    public static final String COLUMN_UNVISIBLE_WHEN_COMPLETE = "unvisible_when_complete";
    
    /**
     * 更改数据库status为pending时，等待DownloadService处理
     */
    public static final String COLUMN_WAIT_INIT = "wait_init";
    
    /**
     * 应用商店下发的文件大小，防止url劫持
     */
    public static final String COLUMN_PUSH_TOTAL_SIZE = "push_total_size";
    
    /**
     * 在query()中返回更多的数据
     */
    public static final String[] TWS_UNDERLYING_COLUMNS = (String[])concatenateArray(DownloadManager.UNDERLYING_COLUMNS,
            new String[] { Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE, COLUMN_TASK_ICON, COLUMN_TASK_CREATE_TIME, COLUMN_DOWNLOADING_SPEED, COLUMN_WAIT_INIT},
            String.class);
    
    private ContentResolver mResolver;
    private Uri mBaseUri = Downloads.Impl.CONTENT_URI;
    private static TwsDownloadManager sInstance = null;

    private TwsDownloadManager(ContentResolver resolver, String packageName) {
        super(resolver, packageName);
        mResolver = resolver;
    }
    
    public synchronized static TwsDownloadManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TwsDownloadManager(context.getContentResolver(), context.getPackageName());
        }
        
        return sInstance;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T[] concatenateArray(T[] arrayA, T[] arrayB, Class<T> className)
    {
      Object[] arrayOfObject = (Object[])Array.newInstance(className, arrayA.length + arrayB.length);
      System.arraycopy(arrayA, 0, arrayOfObject, 0, arrayA.length);
      System.arraycopy(arrayB, 0, arrayOfObject, arrayA.length, arrayB.length);
      return (T[]) arrayOfObject;
    }
    
    @Override
    public void setAccessAllDownloads(boolean accessAllDownloads) {
        super.setAccessAllDownloads(accessAllDownloads);
        if (accessAllDownloads) {
            mBaseUri = Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI;
        } else {
            mBaseUri = Downloads.Impl.CONTENT_URI;
        }
    }
    
    @Override
    public Cursor query(DownloadManager.Query query) {
        Cursor underlyingCursor = query.runQuery(mResolver, TWS_UNDERLYING_COLUMNS, mBaseUri);
        if (underlyingCursor == null) {
            return null;
        }
        return new CursorTranslator(underlyingCursor, mBaseUri);
    }
    
    /**
     * Pause the given running download by manual.
     *
     * @param id the ID of the download to be paused
     * @return the number of downloads actually updated
     * @hide
     */
    public int pauseDownload(long... ids) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PAUSED_BY_APP);
        values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_PAUSED);
        values.put(COLUMN_DOWNLOADING_SPEED, 0L);
        if (ids.length == 1) {
            return mResolver.update(ContentUris.withAppendedId(mBaseUri, ids[0]), values,
                    getWhereCluaseForStatus(new String[] {"=", "="}, new String[] {"OR"}),
                    getWhereArgsForStatus(new int[] {Downloads.Impl.STATUS_PENDING, Downloads.Impl.STATUS_RUNNING}));
        }
        
        String where = "(" + getWhereClauseForIds(ids) + " AND " + getWhereCluaseForStatus(new String[] {"=", "="}, new String[] {"OR"}) + ")";
        String[] whereArgs = (String[])concatenateArray(getWhereArgsForIds(ids),
                getWhereArgsForStatus(new int[] {Downloads.Impl.STATUS_PENDING, Downloads.Impl.STATUS_RUNNING}), String.class);
        return mResolver.update(mBaseUri, values, where, whereArgs);
    }

    /**
     * Resume the given paused download by manual.
     *
     * @param id the IDs of the download to be resumed
     * @return the number of downloads actually updated
     * @hide
     */
    public int resumeDownload(long... ids) {
       ContentValues values = new ContentValues();
       //这里先设置为STATUS_PENDING，等DownloadService正在下载的时候，再设置为STATUS_RUNNING
       values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PENDING);
       values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_RUN);
       //设置数据流量可以下载，check弹窗的动作全部放在app来做，走到这里来，只要resume，无论什么网络接下来就直接下载了
       values.put(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT, true);
       //设置等待处理
       values.put(COLUMN_WAIT_INIT, true);
       if (ids.length == 1) {
           return mResolver.update(ContentUris.withAppendedId(mBaseUri, ids[0]), values,
                   getWhereCluaseForStatus(new String[] {"=", "=", "=", "="}, new String[] {"OR", "OR", "OR"}),
                   getWhereArgsForStatus(new int[] {Downloads.Impl.STATUS_PAUSED_BY_APP, Downloads.Impl.STATUS_WAITING_TO_RETRY,
                           Downloads.Impl.STATUS_WAITING_FOR_NETWORK, Downloads.Impl.STATUS_QUEUED_FOR_WIFI}));
       }

       String where = "(" + getWhereClauseForIds(ids) + " AND " + getWhereCluaseForStatus(new String[] {"=", "=", "=", "="}, new String[] {"OR", "OR", "OR"}) + ")";
       String[] whereArgs = (String[])concatenateArray(getWhereArgsForIds(ids),
                   getWhereArgsForStatus(new int[] {Downloads.Impl.STATUS_PAUSED_BY_APP, Downloads.Impl.STATUS_WAITING_TO_RETRY,
                       Downloads.Impl.STATUS_WAITING_FOR_NETWORK, Downloads.Impl.STATUS_QUEUED_FOR_WIFI}), String.class);
       return mResolver.update(mBaseUri, values, where, whereArgs);
    }
    
    /**
     * 删除任务接口（增加是否删除文件标示）
     * @param deleteFile 是否同时删除下载文件
     * @param ids the IDs of the downloads to remove
     * @return
     */
    public int remove(boolean deleteFile, long... ids) {
        if (ids == null || ids.length == 0) {
            // called with nothing to remove!
            throw new IllegalArgumentException("input param 'ids' can't be null");
        }
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_DELETED, 1);
        values.put(COLUMN_DELETE_FILE_WHEN_DELETE_TASK, deleteFile);
        // if only one id is passed in, then include it in the uri itself.
        // this will eliminate a full database scan in the download service.
        if (ids.length == 1) {
            return mResolver.update(ContentUris.withAppendedId(mBaseUri, ids[0]), values,
                    null, null);
        } 
        return mResolver.update(mBaseUri, values, getWhereClauseForIds(ids),
                getWhereArgsForIds(ids));
    }
    
    /**
     * 注册是否需要广播下载进度等信息的变化
     * @param context
     * @param registerIds
     * @param unRegisterIds
     */
    public void updateNeedBroadcastArray(long[] registerIds, long[] unRegisterIds)
    {
        if ((registerIds == null || registerIds.length == 0)
                && (unRegisterIds == null || unRegisterIds.length == 0)) {
            // called with nothing to remove!
            throw new IllegalArgumentException("input param 'ids' can't be null");
        }
        
        ArrayList<ContentProviderOperation> updates = new ArrayList<ContentProviderOperation>(2);
        ContentProviderOperation.Builder register = null;
        ContentProviderOperation.Builder unrester = null;
        ContentValues values = new ContentValues();
        if (registerIds != null && registerIds.length > 0) {
            values.put(COLUMN_NEED_BROADCAST, 1);
            if (registerIds.length == 1) {
                register = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(mBaseUri, registerIds[0]));
                register.withValues(values);
                
            } else {
                register = ContentProviderOperation.newUpdate(mBaseUri);
                register.withValues(values);
                register.withSelection(getWhereClauseForIds(registerIds), getWhereArgsForIds(registerIds));
            }
        }
        
        if (unRegisterIds != null && unRegisterIds.length >0) {
            values.put(COLUMN_NEED_BROADCAST, 0);
            if (unRegisterIds.length == 1) {
                unrester = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(mBaseUri, unRegisterIds[0]));
                unrester.withValues(values);
            } else {
                unrester = ContentProviderOperation.newUpdate(mBaseUri);
                unrester.withValues(values);
                unrester.withSelection(getWhereClauseForIds(unRegisterIds), getWhereArgsForIds(unRegisterIds));
            }
        }
        
        
        if (register != null) {
            updates.add(register.build());
        }
        if (unrester != null) {
            updates.add(unrester.build());
        }
        
        try {
            mResolver.applyBatch(AUTHORITY, updates);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * 设置允许的网络类型
     * @param flags 参考DownloadManager.Request的setAllowedNetworkTypes(int flags) 的参数
     * @param ids
     * @return
     */
    public int setAllowedNetworkTypes(int flags, long... ids) {
        if (ids == null || ids.length == 0) {
            // called with nothing to remove!
            throw new IllegalArgumentException("input param 'ids' can't be null");
        }
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, flags);
        // if only one id is passed in, then include it in the uri itself.
        // this will eliminate a full database scan in the download service.
        if (ids.length == 1) {
            return mResolver.update(ContentUris.withAppendedId(mBaseUri, ids[0]), values,
                    null, null);
        } 
        return mResolver.update(mBaseUri, values, getWhereClauseForIds(ids),
                getWhereArgsForIds(ids));
    }
    
    /**
     * 设置最大允许非Wi-Fi下载的流量大小
     * @param context
     * @return
     */
    public static boolean setRecommendedMaxBytesOverMobile(Context context, long maxBytes) {
        return Settings.Global.putLong(context.getContentResolver(),
                Settings.Global.DOWNLOAD_RECOMMENDED_MAX_BYTES_OVER_MOBILE, maxBytes);
    }
    
    /**
     * 设置COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT为true，可以跳过非Wi-Fi下流量大小检查
     * @param ids
     * @return
     */
    public int setBypassRecommenderSizeLimit(long... ids) {
        if (ids == null || ids.length == 0) {
            // called with nothing to remove!
            throw new IllegalArgumentException("input param 'ids' can't be null");
        }
        
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT, true);
        if (ids.length == 1) {
            return mResolver.update(ContentUris.withAppendedId(mBaseUri, ids[0]), values,
                    null, null);
        } 
        return mResolver.update(mBaseUri, values, getWhereClauseForIds(ids),
                getWhereArgsForIds(ids));
    }
    
    private static String getWhereCluaseForStatus(String[] operators, String[] joiner) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(");
        for (int i = 0; i < operators.length; i++) {
            if (i > 0) {
                whereClause.append(joiner[i-1] + " ");
            }
            whereClause.append(Downloads.Impl.COLUMN_STATUS);
            whereClause.append(" " + operators[i] + " ? ");
        }
        whereClause.append(")");
        return whereClause.toString();
    }
    
    private static String[] getWhereArgsForStatus(int[] status) {
        String[] whereArgs = new String[status.length];
        for (int i = 0; i < status.length; i++) {
            whereArgs[i] = Integer.toString(status[i]);
        }
        return whereArgs;
    }
    
    public static class Request extends DownloadManager.Request {

        private String mIconUrl; //任务icon url
        private boolean mNeedBroadcast = false; //广播下载进度、暂停、继续、删除
        private boolean mUnVisibleWhenComplete = false; //下载完成后，不要显示在downloadUi中，为主题添加
        private boolean mBypassRecommenderSizeLimit = false; //跳过数据流量检查
        private long mPushedTotalSize = -1L;
        
        public Request(Uri uri) {
            super(uri);
        }
        
        public Request(String uriString) {
            super(uriString);
        }
        
        public Request setIconUrl(String iconUrl) {
            mIconUrl = iconUrl;
            return this;
        }
        
        public Request setNeedBroadcast(boolean need) {
            mNeedBroadcast = need;
            return this;
        }
        
        public Request setUnVisibleWhenComplete(boolean isUnvisible) {
            mUnVisibleWhenComplete = isUnvisible;
            return this;
        }
        
        public Request setBypassRecommenderSizeLimit(boolean byPass) {
            mBypassRecommenderSizeLimit = byPass;
            return this;
        }
        
        public Request setTotalLength (long length) {
            mPushedTotalSize = length;
            return this;
        }
        
        @Override
        protected ContentValues toContentValues(String packageName) {
            ContentValues values = super.toContentValues(packageName);
            values.put(COLUMN_TASK_CREATE_TIME, System.currentTimeMillis());
            values.put(COLUMN_NEED_BROADCAST, mNeedBroadcast);
            values.put(COLUMN_UNVISIBLE_WHEN_COMPLETE, mUnVisibleWhenComplete);
            values.put(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT, mBypassRecommenderSizeLimit);
            
            if (mIconUrl != null) {
                values.put(COLUMN_TASK_ICON, mIconUrl);
            }
            if (mPushedTotalSize > 0L) {
                values.put(COLUMN_PUSH_TOTAL_SIZE, mPushedTotalSize);
            }
            return values;
        }
    }
    
    /**
     * This class may be used to filter download manager queries.
     */
    public static class Query extends DownloadManager.Query {
        
        private String mCallingPackageName;
        
        public Query setCallingPakageName(String callingName) {
            mCallingPackageName = callingName;
            return this;
        }
        
        /**
         * 按照两个字段排序
         * @param column1
         * @param column2 
         * @param direction
         * @return
         */
        public Query orderBy(String column1, String column2, int direction1, int direction2) {
            if (TextUtils.isEmpty(column2)) {
                setValue(this, "mOrderByColumn", column1);
                setValue(this, "mOrderDirection", direction1);
            } else{
                String orderDirection = (direction1 == ORDER_ASCENDING ? "ASC" : "DESC");
                setValue(this, "mOrderByColumn", column1 + " " + orderDirection + ", " + column2);
                setValue(this, "mOrderDirection", direction2);
            }
            return this;
        }

        /**
         * Run this query using the given ContentResolver.
         * @param projection the projection to pass to ContentResolver.query()
         * @return the Cursor returned by ContentResolver.query()
         */
        @Override
        Cursor runQuery(ContentResolver resolver, String[] projection, Uri baseUri) {
            Uri uri = baseUri;
            List<String> selectionParts = new ArrayList<String>();
            String[] selectionArgs = null;

            long[] mIds = (long[])getValue(this, "mIds");
            if (mIds != null) {
                selectionParts.add(getWhereClauseForIds(mIds));
                selectionArgs = getWhereArgsForIds(mIds);
            }

            Integer mStatusFlags = (Integer)getValue(this, "mStatusFlags");
            if (mStatusFlags != null) {
                List<String> parts = new ArrayList<String>();
                if ((mStatusFlags & STATUS_PENDING) != 0) {
                    parts.add(statusClause("=", Downloads.Impl.STATUS_PENDING));
                }
                if ((mStatusFlags & STATUS_RUNNING) != 0) {
                    parts.add(statusClause("=", Downloads.Impl.STATUS_RUNNING));
                }
                if ((mStatusFlags & STATUS_PAUSED) != 0) {
                    parts.add(statusClause("=", Downloads.Impl.STATUS_PAUSED_BY_APP));
                    parts.add(statusClause("=", Downloads.Impl.STATUS_WAITING_TO_RETRY));
                    parts.add(statusClause("=", Downloads.Impl.STATUS_WAITING_FOR_NETWORK));
                    parts.add(statusClause("=", Downloads.Impl.STATUS_QUEUED_FOR_WIFI));
                }
                if ((mStatusFlags & STATUS_SUCCESSFUL) != 0) {
                    parts.add(statusClause("=", Downloads.Impl.STATUS_SUCCESS));
                }
                if ((mStatusFlags & STATUS_FAILED) != 0) {
                    parts.add("(" + statusClause(">=", 400)
                              + " AND " + statusClause("<", 600) + ")");
                }
                selectionParts.add(joinStrings(" OR ", parts));
            }

            Boolean mOnlyIncludeVisibleInDownloadsUi = (Boolean)getValue(this, "mOnlyIncludeVisibleInDownloadsUi");
            if (mOnlyIncludeVisibleInDownloadsUi != null && mOnlyIncludeVisibleInDownloadsUi) {
                selectionParts.add(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI + " != '0'");
            }

            // only return rows which are not marked 'deleted = 1'
            selectionParts.add(Downloads.Impl.COLUMN_DELETED + " != '1'");
            
            if (!TextUtils.isEmpty(mCallingPackageName)) {
                selectionParts.add(Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE + " = '" + mCallingPackageName + "'");
            }

            String selection = joinStrings(" AND ", selectionParts);
            
            int mOrderDirection = (Integer)getValue(this, "mOrderDirection");
            String mOrderByColumn = (String)getValue(this, "mOrderByColumn");
            String orderDirection = (mOrderDirection == ORDER_ASCENDING ? "ASC" : "DESC");
            String orderBy = mOrderByColumn + " " + orderDirection;

            return resolver.query(uri, projection, selection, selectionArgs, orderBy);
        }

        private String joinStrings(String joiner, Iterable<String> parts) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String part : parts) {
                if (!first) {
                    builder.append(joiner);
                }
                builder.append(part);
                first = false;
            }
            return builder.toString();
        }

        private String statusClause(String operator, int value) {
            return Downloads.Impl.COLUMN_STATUS + operator + "'" + value + "'";
        }
        
        private static Object getValue(Query query, String name) {
            try {
                Field field = DownloadManager.Query.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(query);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            return null;
        }
        
        private static void setValue(Query query, String name, Object value) {
            try {
                Field field = DownloadManager.Query.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(query, value);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    /**
     * This class wraps a cursor returned by DownloadProvider -- the "underlying cursor" -- and
     * presents a different set of columns, those defined in the DownloadManager.COLUMN_* constants.
     * Some columns correspond directly to underlying values while others are computed from
     * underlying data.
     */
    private static class CursorTranslator extends CursorWrapper {
        private Uri mBaseUri;

        public CursorTranslator(Cursor cursor, Uri baseUri) {
            super(cursor);
            mBaseUri = baseUri;
        }

        @Override
        public int getInt(int columnIndex) {
            return (int) getLong(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            if (getColumnName(columnIndex).equals(COLUMN_REASON)) {
                return getReason(super.getInt(getColumnIndex(Downloads.Impl.COLUMN_STATUS)));
            } else if (getColumnName(columnIndex).equals(COLUMN_STATUS)) {
                return translateStatus(super.getInt(getColumnIndex(Downloads.Impl.COLUMN_STATUS)));
            } else {
                return super.getLong(columnIndex);
            }
        }

        @Override
        public String getString(int columnIndex) {
            return (getColumnName(columnIndex).equals(COLUMN_LOCAL_URI)) ? getLocalUri() :
                    super.getString(columnIndex);
        }

        private String getLocalUri() {
            long destinationType = getLong(getColumnIndex(Downloads.Impl.COLUMN_DESTINATION));
            if (destinationType == Downloads.Impl.DESTINATION_FILE_URI ||
                    destinationType == Downloads.Impl.DESTINATION_EXTERNAL ||
                    destinationType == Downloads.Impl.DESTINATION_NON_DOWNLOADMANAGER_DOWNLOAD) {
                String localPath = getString(getColumnIndex(COLUMN_LOCAL_FILENAME));
                if (localPath == null) {
                    return null;
                }
                return Uri.fromFile(new File(localPath)).toString();
            }

            // return content URI for cache download
            long downloadId = getLong(getColumnIndex(Downloads.Impl._ID));
            return ContentUris.withAppendedId(mBaseUri, downloadId).toString();
        }

        private long getReason(int status) {
            switch (translateStatus(status)) {
                case STATUS_FAILED:
                    return getErrorCode(status);

                case STATUS_PAUSED:
                    return getPausedReason(status);

                default:
                    return 0; // arbitrary value when status is not an error
            }
        }

        private long getPausedReason(int status) {
            switch (status) {
                case Downloads.Impl.STATUS_WAITING_TO_RETRY:
                    return PAUSED_WAITING_TO_RETRY;

                case Downloads.Impl.STATUS_WAITING_FOR_NETWORK:
                    return PAUSED_WAITING_FOR_NETWORK;

                case Downloads.Impl.STATUS_QUEUED_FOR_WIFI:
                    return PAUSED_QUEUED_FOR_WIFI;

                default:
                    return PAUSED_UNKNOWN;
            }
        }

        private long getErrorCode(int status) {
            if ((400 <= status && status < Downloads.Impl.MIN_ARTIFICIAL_ERROR_STATUS)
                    || (500 <= status && status < 600)) {
                // HTTP status code
                return status;
            }

            switch (status) {
                case Downloads.Impl.STATUS_FILE_ERROR:
                    return ERROR_FILE_ERROR;

                case Downloads.Impl.STATUS_UNHANDLED_HTTP_CODE:
                case Downloads.Impl.STATUS_UNHANDLED_REDIRECT:
                    return ERROR_UNHANDLED_HTTP_CODE;

                case Downloads.Impl.STATUS_HTTP_DATA_ERROR:
                    return ERROR_HTTP_DATA_ERROR;

                case Downloads.Impl.STATUS_TOO_MANY_REDIRECTS:
                    return ERROR_TOO_MANY_REDIRECTS;

                case Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR:
                    return ERROR_INSUFFICIENT_SPACE;

                case Downloads.Impl.STATUS_DEVICE_NOT_FOUND_ERROR:
                    return ERROR_DEVICE_NOT_FOUND;

                case Downloads.Impl.STATUS_CANNOT_RESUME:
                    return ERROR_CANNOT_RESUME;

                case Downloads.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR:
                    return ERROR_FILE_ALREADY_EXISTS;

                default:
                    return ERROR_UNKNOWN;
            }
        }

        private int translateStatus(int status) {
            switch (status) {
                case Downloads.Impl.STATUS_PENDING:
                    return STATUS_PENDING;

                case Downloads.Impl.STATUS_RUNNING:
                    return STATUS_RUNNING;

                case Downloads.Impl.STATUS_PAUSED_BY_APP:
                case Downloads.Impl.STATUS_WAITING_TO_RETRY:
                case Downloads.Impl.STATUS_WAITING_FOR_NETWORK:
                case Downloads.Impl.STATUS_QUEUED_FOR_WIFI:
                    return STATUS_PAUSED;

                case Downloads.Impl.STATUS_SUCCESS:
                    return STATUS_SUCCESSFUL;

                default:
                    assert Downloads.Impl.isStatusError(status);
                    return STATUS_FAILED;
            }
        }
    }
}
