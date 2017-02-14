package com.tencent.tws.assistant.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class TwsSecureTableValue {
	private static final String TAG = "TwsSecureTableValue";
	
	public static final String AUTHORITY = "tws_secure";
	
    /**
     * The content:// style URL for this provider 
     */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    
    /**
     * no public constructor since this is a utility class
     */
    private TwsSecureTableValue() {}
    
	public static final class BlackListTable implements BaseColumns {
        /**
         * no public constructor since this is a utility class
         */
		private BlackListTable() {}
		
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "contactlist");
        
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String NUMBER = "number";
		public static final String TYPE = "type";
		public static final String RING_STATUS = "ringStatus";
		public static final String SMSTATUS = "SMStatus";
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/contactlist";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/contactlist";		
	}
	
	public static final class SmsTable {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "smslog");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/smslog";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/smslog";
		
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String ADDRESS = "address";
		public static final String DATE = "date";
		public static final String AREAREASON = "areareason";
		public static final String READ = "read";
		public static final String BODY = "body";
		public static final String TYPE= "type";
	}
	
	public static final class CallLogTable {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "pimcalllog");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/pimcalllog";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/pimcalllog";
		
		public static final String ID = "id";
		public static final String NUMBER = "number";
		public static final String NAME = "name";
		public static final String DATE = "date";
		public static final String AREAREASON = "areareason";
		public static final String READ = "read";
	}
	
	public static final class KeyWordTable {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "smskeyword");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/smskeyword";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/smskeyword";
		
		public static final String ID = "id";
		public static final String SMSKEYWORD = "smskeyword";
	}
	
	public static final class WhiteListTable {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "whitelist");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/whitelist";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/whitelist";
		
		public static final String ID = "id";
		public static final String NUMBER = "number";
		public static final String NAME = "name";
		public static final String TYPE = "type";
		public static final String RING_STATUS = "ringStatus";
		public static final String SMSTATUS = "SMStatus";
	}
	
	public static final class PrivateListTable {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "privatelist");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/privatelist";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/privatelist";
		
		public static final String ID = "id";
		public static final String NUMBER = "number";
		public static final String NAME = "name";
		public static final String PRIVATE_ID = "private_id";
		public static final String CONTACTS_ID = "contacts_id";
	}
}
