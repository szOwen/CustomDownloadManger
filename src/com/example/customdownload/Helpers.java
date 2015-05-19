package com.example.customdownload;

import java.io.File;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Log;

public class Helpers {
	private static final String TAG = Helpers.class.getSimpleName();

    public static Random sRandom = new Random(SystemClock.uptimeMillis());
    
    private static AtomicInteger msAtomicInteger = new AtomicInteger(0);
    
    /**
     * 获取唯一ID
     * @return
     */
    public static long getUniqueId() {
    	if (msAtomicInteger.get() > 999999) {
    		msAtomicInteger.set(0);
    	}
    	
    	long time = System.currentTimeMillis();
    	return time * 100 + msAtomicInteger.incrementAndGet();
    }

    /**
     * Checks whether the filename looks legitimate
     */
    public static boolean isFilenameValid(String filename) {
		filename = filename.replaceFirst("/+", "/"); // normalize leading slashes
		return filename.startsWith(Environment.getDownloadCacheDirectory().toString()) || filename.startsWith(Environment.getExternalStorageDirectory().toString());
    }

    /**
     * Returns whether the network is available
     */
    public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
		    Log.w(TAG, "isNetworkAvailable: couldn't get connectivity manager");
		    return false;
		}

		NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
		if (activeInfo == null) {
			Log.v(TAG, "isNetworkAvailable: network is not available");
		    return false;
		}
		return true;
    }

    public static boolean isExternalMediaMounted() {
		if (!Environment.getExternalStorageState().equals(
			Environment.MEDIA_MOUNTED)) {
		    // No SD card found.
		    Log.d(TAG, "isExternalMediaMounted: no external storage");
		    return false;
		}
		return true;
    }

    /**
     * @return the root of the filesystem containing the given path
     */
    public static File getFilesystemRoot(String path) {
		File cache = Environment.getDownloadCacheDirectory();
		if (path.startsWith(cache.getPath())) {
		    return cache;
		}
		File external = Environment.getExternalStorageDirectory();
		if (path.startsWith(external.getPath())) {
		    return external;
		}
		throw new IllegalArgumentException("Cannot determine filesystem root for " + path);
    }

    /**
     * @return the number of bytes available on the filesystem rooted at the
     *         given File
     */
    public static long getAvailableBytes(File root) {
		StatFs stat = new StatFs(root.getPath());
		// put a bit of margin (in case creating the file grows the system by a
		// few blocks)
		long availableBlocks = (long) stat.getAvailableBlocks() - 4;
		return stat.getBlockSize() * availableBlocks;
    }
}
