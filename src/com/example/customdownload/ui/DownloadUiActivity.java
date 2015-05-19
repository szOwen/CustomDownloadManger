package com.example.customdownload.ui;

import java.io.File;
import java.util.ArrayList;

import com.example.customdownload.CustomDownloadManager;
import com.example.customdownload.DownloadDatabaseHelper;
import com.example.customdownload.DownloadObserver;
import com.example.customdownload.Downloads;
import com.example.customdownload.TaskInfo;
import com.example.customdownload.util.CommonTool;
import com.example.customdownloadmanager.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DownloadUiActivity extends Activity {
	private static final String TAG = DownloadUiActivity.class.getSimpleName();
	
	private static final String mUrl = "http://down.sandai.net/thunder7/Thunder_dl_7.9.36.4940.exe";
	private static final String mMimeType = "application/octet-stream";
	private static final String mFileName = "Thunder_dl_7.9.36.4940.exe";
	
	private Button mNewBtn;
	
	//ListView适配器
	public class DownloadUiListViewAdapter extends BaseAdapter {
		
		private ArrayList<TaskInfo> mTaskInfoList;
		
		/**
		 * 存储ListViewItem
		 * @author XlOwen
		 *
		 */
		class ListViewItemHolder {
			TextView fileNameView;
			TextView progressView;
			TextView statusView;
		}
		
		public DownloadUiListViewAdapter() {
			mTaskInfoList = DownloadDatabaseHelper.getInstance().getTaskInfoList();
			if (null == mTaskInfoList) {
				mTaskInfoList = new ArrayList<TaskInfo>();
			}
			
			CustomDownloadManager.getInstance().addDownloadObserver(new DownloadObserver() {
				public void onProgressChanged(long currentBytes, long totalBytes) {
					mTaskInfoList = DownloadDatabaseHelper.getInstance().getTaskInfoList();
					if (null == mTaskInfoList) {
						mTaskInfoList = new ArrayList<TaskInfo>();
					}
					notifyDataSetChanged();
				}
				public void onStatusChanged(int taskStatus) {
					mTaskInfoList = DownloadDatabaseHelper.getInstance().getTaskInfoList();
					if (null == mTaskInfoList) {
						mTaskInfoList = new ArrayList<TaskInfo>();
					}
					notifyDataSetChanged();
				}
			});
		}

		@Override
		public int getCount() {
			Log.d(TAG, "getCount: TaskInfoListSize = " + mTaskInfoList.size());
			return mTaskInfoList.size();
		}

		@Override
		public Object getItem(int position) {
			return mTaskInfoList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return ((TaskInfo)getItem(position)).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.d(TAG, "getView: position = " + position);
			//设置Item元素
			ListViewItemHolder itemHolder;
			if (null == convertView) {
				convertView = LayoutInflater.from(DownloadUiActivity.this).inflate(R.layout.layout_downloadui_listview_item, null);
				
				itemHolder = new ListViewItemHolder();
				itemHolder.fileNameView = (TextView)convertView.findViewById(R.id.downloadui_listview_item_filename);
				itemHolder.progressView = (TextView)convertView.findViewById(R.id.downloadui_listview_item_progress);
				itemHolder.statusView = (TextView)convertView.findViewById(R.id.downloadui_listview_item_status);
				convertView.setTag(itemHolder);
			} else {
				itemHolder = (ListViewItemHolder)convertView.getTag();
			}
			
			//获得任务数据
			TaskInfo taskInfo = mTaskInfoList.get(position);
			
			//填充Item数据
			itemHolder.fileNameView.setText(taskInfo.getTaskUri());
			itemHolder.progressView.setText(CommonTool.convertBSize2String(taskInfo.getCurrentBytes(), 0) + "/" + CommonTool.convertBSize2String(taskInfo.getTotalBytes(), 0));
			itemHolder.statusView.setText(convertStatusToText(taskInfo.getTaskStatus()));
			
			return convertView;
		}
		
		private String convertStatusToText(int taskStatus) {
			String statusText = String.valueOf(taskStatus);
			switch (taskStatus) {
			case Downloads.STATUS_PAUSE_PENDING:
				statusText = "STATUS_PENDING";
				break;
			case Downloads.STATUS_RUNNING:
				statusText = "STATUS_RUNNING";
				break;
			case Downloads.STATUS_PAUSED:
				statusText = "STATUS_PAUSED";
				break;
			case Downloads.STATUS_WAITING_TO_RETRY:
				statusText = "STATUS_WAITING_TO_RETRY";
				break;
			case Downloads.STATUS_WAITING_FOR_NETWORK:
				statusText = "STATUS_WAITING_FOR_NETWORK";
				break;
			case Downloads.STATUS_QUEUED_FOR_WIFI:
				statusText = "STATUS_QUEUED_FOR_WIFI";
				break;
			case Downloads.STATUS_SUCCESS:
				statusText = "STATUS_SUCCESS";
				break;
			case Downloads.STATUS_NOT_ACCEPTABLE:
				statusText = "STATUS_NOT_ACCEPTABLE";
				break;
			case Downloads.STATUS_FILE_ALREADY_EXISTS_ERROR:
				statusText = "STATUS_FILE_ALREADY_EXISTS_ERROR";
				break;
			case Downloads.STATUS_CANNOT_RESUME:
				statusText = "STATUS_CANNOT_RESUME";
				break;
			case Downloads.STATUS_CANCELED:
				statusText = "STATUS_CANCELED";
				break;
			case Downloads.STATUS_UNKNOWN_ERROR:
				statusText = "STATUS_UNKNOWN_ERROR";
				break;
			case Downloads.STATUS_FILE_ERROR:
				statusText = "STATUS_FILE_ERROR";
				break;
			case Downloads.STATUS_UNHANDLED_REDIRECT:
				statusText = "STATUS_UNHANDLED_REDIRECT";
				break;
			case Downloads.STATUS_UNHANDLED_HTTP_CODE:
				statusText = "STATUS_UNHANDLED_HTTP_CODE";
				break;
			case Downloads.STATUS_TOO_MANY_REDIRECTS:
				statusText = "STATUS_TOO_MANY_REDIRECTS";
				break;
			case Downloads.STATUS_INSUFFICIENT_SPACE_ERROR:
				statusText = "STATUS_INSUFFICIENT_SPACE_ERROR";
				break;
			case Downloads.STATUS_DEVICE_NOT_FOUND_ERROR:
				statusText = "STATUS_DEVICE_NOT_FOUND_ERROR";
				break;
			}
			return statusText;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: Enter");
		
		setContentView(R.layout.activity_downloadui);
		
		//初始化下载管理模块
		CustomDownloadManager.getInstance().init(this);
		
		//新建
		mNewBtn = (Button)findViewById(R.id.downloadui_newbtn);
		mNewBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "mCreateBtn:onClick: Enter");
				
				File downloadDirFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				if (!downloadDirFile.exists()) {
					downloadDirFile.mkdirs();
				}
				String filePath = downloadDirFile.getPath() + "/";
				
				long id = CustomDownloadManager.getInstance().createTask(mUrl, mFileName, filePath, mMimeType);
			}
		});
		
		//初始化列表
		ListView listView = (ListView)findViewById(R.id.downloadui_listview);
		DownloadUiListViewAdapter downloadUiListViewAdapter = new DownloadUiListViewAdapter();
		listView.setAdapter(downloadUiListViewAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d(TAG, "onItemClick: position = " + position + ", id = " + id);
				TaskInfo taskInfo = DownloadDatabaseHelper.getInstance().getTaskInfoById(id);
				if (Downloads.STATUS_RUNNING == taskInfo.getTaskStatus()) {
					CustomDownloadManager.getInstance().pauseTask(id);
				} else {
					CustomDownloadManager.getInstance().startTask(id);
				}
			}			
		});
	}
}
