package com.example.customdownload;

import org.apache.http.Header;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class DownloadMsgHandler {
	private static final String TAG = DownloadMsgHandler.class.getSimpleName();

	public static final int MSG_START = 0;  
    public static final int MSG_STOP = 1;
    public static final int MSG_PROGRESS = 2;
    public static final int MSG_CANCEL = 3;
    public static final int MSG_SUCCESS = 4;    
    public static final int MSG_FAILURE = 5;    
    public static final int MSG_RENAME = 6;
    public static final int MSG_STATUS = 7;
    public static final int MSG_URI = 8;
    public static final int MSG_NETWORK = 9;
    
    public static final String BROADCAST_TYPE = "broadcast_type";
    public static final String BROADCAST_STATUS = "broadcast_status";
    public static final String BROADCAST_TOTAL_BYTES = "broadcast_total_bytes";
    public static final String BROADCAST_CURRENT_BYTES = "broadcast_current_bytes";
    public static final String BROADCAST_NEW_NAME = "broadcast_new_name";
    
    private Context mContext;
    private Handler mThisHandler = null;
	
	public DownloadMsgHandler(Context context) {
		mContext = context;
		// Set up a handler to post events back to the correct thread if possible
        if (Looper.myLooper() != null)
        {
        	mThisHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                	DownloadMsgHandler.this.handleMessage(msg);
                }
            };
        }
	}
    
    public void sendStartMessage(long id)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_START, new Object[] {id}));
    	}        
    }
    
    public void sendStopMessage(long id)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_STOP, new Object[] {id}));
    	}        
    }
    
    public void sendProgressChangeMessage(long id, long total, long loaded)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_PROGRESS, new Object[] { id, total, loaded }));
    	}        
    }
    
    public void sendCancelMessage(long id/*int statusCode, Header[] headers, String responseBody*/)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_CANCEL, new Object[] {id}));
    	}        
    }
    
    public void sendSuccessMessage(long id, int statusCode, Header[] headers, String responseBody)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_SUCCESS, new Object[] { Integer.valueOf(statusCode), headers, responseBody }));
    	}        
    }
    
    public void sendFailureMessage(long id, String responseBody)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_FAILURE, responseBody));
    	}        
    }
    
    public void sendFailureMessage(long id, Throwable e, String responseBody)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_FAILURE, new Object[] { id, e, responseBody }));
    	}        
    }
    
    public void sendFailureMessage(long id, Throwable e, byte[] responseBody)
    {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_FAILURE, new Object[] { id, e, responseBody }));
    	}
    }
    
    public void sendStatusChanged(long id, int oldStatus, int newStatus) {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_STATUS, new Object[] {id, oldStatus, newStatus}));
    	}
    }
    
    public void sendUriMessage(long id, String uri) {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_URI, new Object[] {id, uri}));
    	}
    }
    
    public void sendNetworkMessage(long id, String etag, String mimeType) {
    	if (mThisHandler != null) {
    		sendMessage(mThisHandler.obtainMessage(MSG_NETWORK, new Object[] {id, etag, mimeType}));
    	}
    }
    
    protected void sendMessage(Message msg)
    {
        if (mThisHandler != null)
        {
        	mThisHandler.sendMessage(msg);
        }
    }
    
    protected void handleMessage(Message msg)
    {        
        Object[] obj;
        long result;
        
        switch (msg.what){
        case MSG_PROGRESS:
        	obj = (Object[])msg.obj;
        	Log.d(TAG, "handleMessage:MSG_PROGRESS: totalBytes = " + (Long)obj[1] + ", currentBytes = " + (Long)obj[2]);
        	ContentValues progressValues = new ContentValues();
        	progressValues.put(Downloads.COLUMN_TOTAL_BYTES, (Long)obj[1]);
        	progressValues.put(Downloads.COLUMN_CURRENT_BYTES, (Long)obj[2]);
        	result = DownloadDatabaseHelper.getInstance().update((Long)obj[0], progressValues);
        	
        	if (result > 0) {
        		Intent intent = new Intent("com.example.customdownloadmanager.ACTION_DOWNLOAD_CHANGED");
        		intent.putExtra(BROADCAST_TYPE, msg.what);
        		intent.putExtra(BROADCAST_TOTAL_BYTES, (Long)obj[1]);
        		intent.putExtra(BROADCAST_CURRENT_BYTES, (Long)obj[2]);
        		mContext.sendBroadcast(intent, "com.example.customdownloadmanager.permission.DOWNLOAD_CHANGED_RECV");
        	}
        	
        	break;
        case MSG_STATUS:
        	obj = (Object[])msg.obj;
        	
        	ContentValues statusValues = new ContentValues();
        	statusValues.put(Downloads.COLUMN_STATUS, (Integer)obj[2]);
        	result = DownloadDatabaseHelper.getInstance().update((Long)obj[0], statusValues);
        	
        	if (result > 0) {
        		Intent intent = new Intent("com.example.customdownloadmanager.ACTION_DOWNLOAD_CHANGED");
        		intent.putExtra(BROADCAST_TYPE, msg.what);
        		intent.putExtra(BROADCAST_STATUS, (Integer)obj[2]);
        		mContext.sendBroadcast(intent, "com.example.customdownloadmanager.permission.DOWNLOAD_CHANGED_RECV");
        	}
        	
        	break;
        case MSG_URI:
        	obj = (Object[])msg.obj;
        	
        	ContentValues uriValues = new ContentValues();
        	uriValues.put(Downloads.COLUMN_URI, obj[1].toString());
        	result = DownloadDatabaseHelper.getInstance().update((Long)obj[0], uriValues);
        	
        	break;
        case MSG_NETWORK:
        	obj = (Object[])msg.obj;
        	
        	ContentValues netWorkValues = new ContentValues();
        	if (null != obj[1]) {
        		netWorkValues.put(Downloads.COLUMN_ETAG, obj[1].toString());
        	}
        	if (null != obj[2]) {
        		netWorkValues.put(Downloads.COLUMN_MIME_TYPE, obj[2].toString());
        	}
        	result = DownloadDatabaseHelper.getInstance().update((Long)obj[0], netWorkValues);
        	
        	break;
        }
    }
}
