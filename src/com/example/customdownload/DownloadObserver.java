package com.example.customdownload;

public class DownloadObserver {
	/**
	 * 任务创建事件
	 * @param taskId
	 */
	public void onCreate(long taskId) {}
	
	/**
	 * 任务删除事件
	 * @param taskId
	 */
	public void onDelete(long taskId) {}
	
	/**
	 * 下载状态改变事件
	 * @param downloadStatus
	 */
	public void onStatusChanged(int downloadStatus) {}
	
	/**
	 * 下载进度改变事件
	 * @param downloadProgress
	 */
	public void onProgressChanged(long currentBytes, long totalBytes) {}
}
