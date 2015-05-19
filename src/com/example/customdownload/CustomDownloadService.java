package com.example.customdownload;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CustomDownloadService extends Service {
	private static final String TAG = CustomDownloadService.class.getSimpleName();
    
    private ThreadPoolExecutor mThreadPool;
    private Map<Long, ThreadInfo> mThreadInfoMap;
    private DownloadMsgHandler mDownloadMsgHandler;
    
    //存储线程信息
    public class ThreadInfo {
    	public DownloadRunnable downloadRunnable;
    	public Future<?> future;
    }
	
	//自定义的Binder类
	public class ServiceBinder extends Binder {
		public CustomDownloadService getService() {
			return CustomDownloadService.this;
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate: Enter");
		
		mThreadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		mThreadInfoMap = new HashMap<Long, ThreadInfo>();
		mDownloadMsgHandler = new DownloadMsgHandler(this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand: flags = " + flags + ", startId = " + startId);
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy: Enter");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return new ServiceBinder();
	}
	
	/**
	 * 新建下载任务
	 * @param url
	 * @param fileName
	 * @param mimeType
	 * @return
	 */
	public long createTask(String url, String fileName, String savePath, String mimeType) {
		Log.d(TAG, "createTask: url = " + url + ", fileName = " + fileName + ", savePath = " + savePath + ", mimeType = " + mimeType);
		
		//生成任务ID
		long taskId = Helpers.getUniqueId();
		Log.d(TAG, "createTask: taskId = " + taskId);
		
		//把任务数据存数据库
		ContentValues createTaskValues = new ContentValues();
		createTaskValues.put(Downloads._ID, taskId);
		createTaskValues.put(Downloads.COLUMN_URI, Uri.parse(url).toString());
		createTaskValues.put(Downloads.COLUMN_FILE_NAME, fileName);
		createTaskValues.put(Downloads.COLUMN_FILE_PATH, savePath + fileName);
		createTaskValues.put(Downloads.COLUMN_MIME_TYPE, mimeType);
		createTaskValues.put(Downloads.COLUMN_CURRENT_BYTES, 0);
		createTaskValues.put(Downloads.COLUMN_STATUS, Downloads.STATUS_STOP);
		DownloadDatabaseHelper.getInstance().insert(createTaskValues);
		
		//触发状态变化
		mDownloadMsgHandler.sendStatusChanged(taskId, 0, Downloads.STATUS_STOP);
		
		return taskId;
	}
	
	/**
	 * 开始下载
	 * @param id
	 * @return
	 */
	public long startTask(long taskId) {
		Log.d(TAG, "startTask: taskId = " + taskId);
		
		//触发状态变化
		mDownloadMsgHandler.sendStatusChanged(taskId, 0, Downloads.STATUS_START_PENDING);

		TaskInfo taskInfo = DownloadDatabaseHelper.getInstance().getTaskInfoById(taskId);
		
		DownloadInfo downloadInfo = new DownloadInfo(getApplicationContext(), taskInfo.getId());
		downloadInfo.setUri(taskInfo.getTaskUri());
		downloadInfo.setFileName(taskInfo.getFileName());
		downloadInfo.setFilePath(taskInfo.getSavePath());
		downloadInfo.setTotalBytes(taskInfo.getTotalBytes());
		downloadInfo.setMimeType("application/octet-stream");
		downloadInfo.setEtag(taskInfo.getEtag());
		
		ThreadInfo threadInfo = mThreadInfoMap.get(taskId);
		if (null == threadInfo || threadInfo.future.isDone() || threadInfo.future.isCancelled()) {
			threadInfo = new ThreadInfo();
			threadInfo.downloadRunnable = new DownloadRunnable(getApplicationContext(), mDownloadMsgHandler, downloadInfo);
			threadInfo.future = mThreadPool.submit(threadInfo.downloadRunnable);
			mThreadInfoMap.put(taskId, threadInfo);
		} else {
			threadInfo.downloadRunnable.setControl(Downloads.CONTROL_RUN);
		}
		
		return 0;
	}
	
	/**
	 * 暂停任务
	 * @param id
	 * @return
	 */
	public long pauseTask(long taskId) {
		Log.d(TAG, "pause: taskId = " + taskId);
		
		ThreadInfo threadInfo = mThreadInfoMap.get(taskId);
		if (null == threadInfo) {
			return -1;
		}	
		
		//触发状态变化
		mDownloadMsgHandler.sendStatusChanged(taskId, 0, Downloads.STATUS_PAUSE_PENDING);
		
		threadInfo.downloadRunnable.setControl(Downloads.CONTROL_PAUSED);
		
		return 0;
	}
	
	/**
	 * 取消任务
	 * @param id
	 * @return
	 */
	public long cancelTask(long taskId) {
		Log.d(TAG, "cancelTask: taskId = " + taskId);

		ThreadInfo threadInfo = mThreadInfoMap.get(taskId);
		if (null == threadInfo) {
			return -1;
		}
		
		//触发状态变化
		mDownloadMsgHandler.sendStatusChanged(taskId, 0, Downloads.STATUS_CANCEL_PENDING);
		
		threadInfo.downloadRunnable.setControl(Downloads.CONTROL_CANCEL);
		
		return 0;
	}
	
	/**
	 * 删除任务
	 * @param id
	 * @return
	 */
	public long deleteTask(long taskId, boolean deleteFile) {
		Log.d(TAG, "deleteTask: taskId = " + taskId + ", deleteFile = " + deleteFile);

		ThreadInfo threadInfo = mThreadInfoMap.get(taskId);
		if (null == threadInfo) {
			return -1;
		}
		
		//触发状态变化
		mDownloadMsgHandler.sendStatusChanged(taskId, 0, Downloads.STATUS_DELETE_PENDING);
		
		threadInfo.downloadRunnable.setControl(Downloads.CONTROL_CANCEL);
		threadInfo.future.cancel(true);
		mThreadInfoMap.remove(threadInfo);
		
		if (deleteFile) {
			//(new File(downloadInfo.mFilePath)).delete();
		}
		
		return 0;
	}
}
