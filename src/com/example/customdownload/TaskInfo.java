package com.example.customdownload;

import android.util.Log;

public class TaskInfo {
	private static final String TAG = TaskInfo.class.getSimpleName();
	
	private long mId;	//任务ID
	private String mFileName;	//文件名
	private String mSavePath;	//文件路径
	private int mTaskStatus;	//任务状态
	private String mUri;
	private long mTotalBytes;	//文件总大小
	private long mCurrentBytes;	//文件已下载大小
	private String mEtag;
	
	public TaskInfo() {}
	
	/**
	 * 打印
	 */
	public void print() {
		Log.d(TAG, "TaskInfo: Id = " + mId + ", FileName = " + mFileName + ", TaskStatus = " + mTaskStatus + ", Uri = " + mUri + ", TotalBytes = " + mTotalBytes + ", CurrentBytes = " + mCurrentBytes);
	}
	
	/**
	 * 设置任务ID
	 * @param id
	 */
	public void setId(long id) {
		mId = id;
	}	
	/**
	 * 获取任务ID
	 * @return
	 */
	public long getId() {
		return mId;
	}
	
	/**
	 * 设置文件名
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		mFileName = fileName;
	}	
	/**
	 * 获取文件名
	 * @return
	 */
	public String getFileName() {
		return mFileName;
	}
	
	/**
	 * 设置文件路径
	 * @param savePath
	 */
	public void setSavePath(String savePath) {
		mSavePath = savePath;
	}	
	/**
	 * 获取文件路径
	 * @return
	 */
	public String getSavePath() {
		return mSavePath;
	}
	
	/**
	 * 设置任务状态
	 * @param taskStatus
	 */
	public void setTaskStatus(int taskStatus) {
		mTaskStatus = taskStatus;
	}	
	/**
	 * 获取任务状态
	 * @return
	 */
	public int getTaskStatus() {
		return mTaskStatus;
	}
	
	/**
	 * 设置任务URI
	 * @param uri
	 */
	public void setTaskUri(String uri) {
		mUri = uri;
	}
	/**
	 * 获取任务URI
	 * @return
	 */
	public String getTaskUri() {
		return mUri;
	}
	
	/**
	 * 设置文件总大小
	 * @param totalBytes
	 */
	public void setTotalBytes(long totalBytes) {
		mTotalBytes = totalBytes;
	}
	/**
	 * 获取文件总大小
	 * @return
	 */
	public long getTotalBytes() {
		return mTotalBytes;
	}
	
	/**
	 * 设置已下载文件大小
	 * @param currentBytes
	 */
	public void setCurrentBytes(long currentBytes) {
		mCurrentBytes = currentBytes;
	}
	/**
	 * 获取已下载文件大小
	 * @return
	 */
	public long getCurrentBytes() {
		return mCurrentBytes;
	}
	
	/**
	 * 设置Etag
	 * @param etag
	 */
	public void setEtag(String etag) {
		mEtag = etag;
	}
	/**
	 * 获取Etag
	 * @return
	 */
	public String getEtag() {
		return mEtag;
	}
}
