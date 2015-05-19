package com.example.customdownload;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class CustomDownloadManager {
	private static final String TAG = CustomDownloadManager.class.getSimpleName();
	
	private Context mContext = null;
	private CustomDownloadService mCustomDownloadService = null;
	private DownloadStatusBroadcastReceiver mDownloadStatusBroadcastReceiver = null;
	private ArrayList<DownloadObserver> mDownloadObserverList = null;
	
	private static class CustomDownloadManagerHolder {
		private static final CustomDownloadManager mInstance = new CustomDownloadManager();
	}
	
	//下载状态改变监听者
	private class DownloadStatusBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Log.d(TAG, "DownloadStatusBroadcastReceiver:onReceiver: Enter");
			int type = arg1.getIntExtra(DownloadMsgHandler.BROADCAST_TYPE, -1);
			Log.d(TAG, "DownloadStatusBroadcastReceiver:onReceiver: type = " + type);
			
			switch (type) {
			case DownloadMsgHandler.MSG_PROGRESS:
				long totalBytes = arg1.getLongExtra(DownloadMsgHandler.BROADCAST_TOTAL_BYTES, 0);
				long currentBytes = arg1.getLongExtra(DownloadMsgHandler.BROADCAST_CURRENT_BYTES, 0);
				Log.d(TAG, "DownloadStatusBroadcastReceiver:onReceiver: totalBytes = " + totalBytes + ", currentBytes = " + currentBytes);
				for (DownloadObserver downloadObserver : mDownloadObserverList) {
					downloadObserver.onProgressChanged(currentBytes, totalBytes);
				}
				break;
			case DownloadMsgHandler.MSG_STATUS:
				int taskStatus = arg1.getIntExtra(DownloadMsgHandler.BROADCAST_STATUS, 0);
				for (DownloadObserver downloadObserver : mDownloadObserverList) {
					downloadObserver.onStatusChanged(taskStatus);
				}
			}
		}		
	}
	
	private CustomDownloadManager() {
		
	}
	
	public static final CustomDownloadManager getInstance() {
		return CustomDownloadManagerHolder.mInstance;
	}
	
	/**
	 * 初始化
	 * @param context
	 */
	public void init(Context context) {
		if (null == mContext) {
			mContext = context.getApplicationContext();
			
			//初始化下载监听者
			mDownloadObserverList = new ArrayList<DownloadObserver>();
			
			//初始化数据库工具类
			DownloadDatabaseHelper.getInstance().init(mContext);
			
			//注册下载状态改变监听者
			mDownloadStatusBroadcastReceiver = new DownloadStatusBroadcastReceiver();
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction("com.example.customdownloadmanager.ACTION_DOWNLOAD_CHANGED");
			mContext.registerReceiver(mDownloadStatusBroadcastReceiver, intentFilter);
			
			bindService();
		}		
	}
	
	/**
	 * 反初始化
	 */
	public void uninit() {
		//反注册下载状态改变监听者
		mContext.unregisterReceiver(mDownloadStatusBroadcastReceiver);
	}
	
	/**
	 * 添加下载监听者
	 * @param downloadObserver
	 */
	public void addDownloadObserver(DownloadObserver downloadObserver) {
		if (!mDownloadObserverList.contains(downloadObserver)) {
			mDownloadObserverList.add(downloadObserver);
		}
	}
	
	/**
	 * 删除下载监听者
	 * @param downloadObserver
	 */
	public void removeDownloadObserver(DownloadObserver downloadObserver) {
		if (mDownloadObserverList.contains(downloadObserver)) {
			mDownloadObserverList.remove(downloadObserver);
		}
	}
	
	public long createTask(String url, String fileName, String savePath, String mimeType) {
		Log.d(TAG, "createTask: url = " + url + ", fileName = " + fileName + ", savePath = " + savePath + ", mimeType");
		
		if (null != mCustomDownloadService) {
			return mCustomDownloadService.createTask(url, fileName, savePath, mimeType);
		} else {
			return -1;
		}
	}
	
	public long startTask(long id) {
		Log.d(TAG, "startTask: id = " + id);
		
		if (null != mCustomDownloadService) {
			return mCustomDownloadService.startTask(id);
		} else {
			return -1;
		}
	}
	
	public long pauseTask(long id) {
		Log.d(TAG, "pauseTask: id = " + id);
		
		if (null != mCustomDownloadService) {
			return mCustomDownloadService.pauseTask(id);
		} else {
			return -1;
		}
	}
	
	public long cancelTask(long id) {
		Log.d(TAG, "cancelTask: id = " + id);
		
		if (null != mCustomDownloadService) {
			return mCustomDownloadService.cancelTask(id);
		} else {
			return -1;
		}
	}
	
	public long deleteTask(long id, boolean deleteFile) {
		Log.d(TAG, "deleteTask: id = " + id + ", deleteFile = " + deleteFile);
		
		if (null != mCustomDownloadService) {
			return mCustomDownloadService.deleteTask(id, deleteFile);
		} else {
			return -1;
		}
	}
	
	private void bindService() {
		Log.d(TAG, "bindService: Enter");
		if (null == mCustomDownloadService) {
			Intent intent = new Intent(mContext, CustomDownloadService.class);
			boolean bindResult = mContext.bindService(intent, new ServiceConnection() {				
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					Log.d(TAG, "onServiceConnected: name = " + name);
					mCustomDownloadService = ((CustomDownloadService.ServiceBinder)service).getService();
				}
				
				@Override
				public void onServiceDisconnected(ComponentName name) {
					Log.d(TAG, "onServiceConnected: name = " + name);					
				}
			}, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "bindService: bindResult = " + bindResult);
		}
	}
}
