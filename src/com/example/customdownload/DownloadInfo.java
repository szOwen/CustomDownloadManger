package com.example.customdownload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

public class DownloadInfo {
	private static final String TAG = DownloadInfo.class.getSimpleName();
	/**
	 * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
	 * {@link ConnectivityManager#TYPE_MOBILE}.
	 */
	public static final int NETWORK_MOBILE = 1 << 0;

	/**
	 * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
	 * {@link ConnectivityManager#TYPE_WIFI}.
	 */
	public static final int NETWORK_WIFI = 1 << 1;

    /**
     * The network is usable for the given download.
     */
    public static final int NETWORK_OK = 1;

    /**
     * There is no network connectivity.
     */
    public static final int NETWORK_NO_CONNECTION = 2;

    /**
     * The download exceeds the maximum size for this network.
     */
    public static final int NETWORK_UNUSABLE_DUE_TO_SIZE = 3;

    /**
     * The download exceeds the recommended maximum size for this network, the user must confirm for
     * this download to proceed without WiFi.
     */
    public static final int NETWORK_RECOMMENDED_UNUSABLE_DUE_TO_SIZE = 4;

    /**
     * The current connection is roaming, and the download can't proceed over a roaming connection.
     */
    public static final int NETWORK_CANNOT_USE_ROAMING = 5;

    /**
     * The app requesting the download specific that it can't use the current network connection.
     */
    public static final int NETWORK_TYPE_DISALLOWED_BY_REQUESTOR = 6;
	
	private Context mContext;

    public long mId;	//代表下载任务的唯一ID
	public String mUri;	//A Uniform Resource Identifier that identifies an abstract or physical resource, as specified by RFC 2396.
	public String mUserAgent;	//to report in your HTTP requests
	public String mFileName;
    public String mFilePath;	//文件路径
    public String mMimeType;	//MIME type的缩写为(Multipurpose Internet Mail Extensions)代表互联网媒体类型(Internet media type)
    public String mETag;	//ETag是实体标签（Entity Tag）的缩写， 根据实体内容生成的一段hash字符串（类似于MD5或者SHA1之后的结果），可以标识资源的状态。 当资源发送改变时，ETag也随之发生变化。
    public long mTotalBytes;	//总大小
    public long mBytesSoFar;	//已下载大小
    public boolean mAllowRoaming;	//是否允许漫游
    public int mAllowedNetworkTypes = ~0; //允许下载的网络类型, default to all network types
    public int mBypassRecommendedSizeLimit = 0;
    public int mNumFailed = 0;	//The number of times that the download manager will retry its network operations when no progress is happening before it gives up.
    public int mControl;	//控制状态，见Downloads.CONTROL_*
    public int mStatus;	//下载状态，见Downloads.STATUS_*

    public volatile boolean mHasActiveThread;	//是否有线程正在处理这个下载任务
    
    private List<Pair<String, String>> mRequestHeaders = new ArrayList<Pair<String, String>>();
    
    public DownloadInfo(Context context) {
    	mContext = context;
    	
    	mId = Helpers.getUniqueId();
    }
    
    public DownloadInfo(Context context, long id) {
    	mContext = context;
    	mId = id;
    }
    
    /**
     * 获得唯一ID
     * @return
     */
    public long getId() {
    	return mId;
    }
       
    /**
     * 设置uri
     * @param uri
     */
    public void setUri(String uri) {
    	Log.d(TAG, "setUri: uri = " + uri);
    	
    	mUri = uri;
    }    
    
    /**
     * 设置文件名
     * @param fileName
     */
    public void setFileName(String fileName) {
    	mFileName = fileName;
    }
    
    /**
     * 设置默认文件路径
     * @param fileName
     */
    public void setFilePath(String filePath) {
    	Log.d(TAG, "setFileName: filePath = " + filePath);
    	
    	mFilePath = filePath;
    }    
    
    /**
     * 设置MIME type
     * @param mimeType
     */
    public void setMimeType(String mimeType) {
    	Log.d(TAG, "setMimeType: mimeType = " + mimeType);
    	
    	mMimeType = mimeType;
    }    
    
    /**
     * 设置ETag
     * @param etag
     */
    public void setEtag(String etag) {
    	mETag = etag;
    }      
    
    /**
     * 设置任务总大小
     * @param totalBytes
     */
    public void setTotalBytes(long totalBytes) {
    	Log.d(TAG, "setTotalBytes: totalBytes = " + totalBytes);
    	
    	mTotalBytes = totalBytes;
    }    

    public Collection<Pair<String, String>> getHeaders() {
        return Collections.unmodifiableList(mRequestHeaders);
    }

    /**
     * Returns whether this download is allowed to use the network.
     * @return one of the NETWORK_* constants
     */
    public int checkCanUseNetwork() {
    	ConnectivityManager connectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
		    return NETWORK_NO_CONNECTION;
		}
		NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
		if (activeInfo == null) {
		    return NETWORK_NO_CONNECTION;
		}
		
        Integer networkType = activeInfo.getType();
        if (networkType == null) {
            return NETWORK_NO_CONNECTION;
        }
        
        boolean isMobile = (networkType == ConnectivityManager.TYPE_MOBILE);
		final TelephonyManager mgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		boolean isRoaming = isMobile && mgr.isNetworkRoaming();
		
        if (!isRoamingAllowed() && isRoaming) {
            return NETWORK_CANNOT_USE_ROAMING;
        }
        
        return NETWORK_OK;
    }

    private boolean isRoamingAllowed() {
        return mAllowRoaming;
    }

    /**
     * Clean up a mimeType string so it can be used to dispatch an intent to
     * view a downloaded asset.
     * 
     * @param mimeType
     *            either null or one or more mime types (semi colon separated).
     * @return null if mimeType was null. Otherwise a string which represents a
     *         single mimetype in lowercase and with surrounding whitespaces
     *         trimmed.
     */
    public String sanitizeMimeType(String mimeType) {
		try {
		    mimeType = mimeType.trim().toLowerCase(Locale.ENGLISH);
	
		    final int semicolonIndex = mimeType.indexOf(';');
		    if (semicolonIndex != -1) {
		    	mimeType = mimeType.substring(0, semicolonIndex);
		    }
		    return mimeType;
		} catch (NullPointerException npe) {
		    return null;
		}
    }
}
