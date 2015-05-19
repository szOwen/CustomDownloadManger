package com.example.customdownload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.FileUtils;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

public class DownloadRunnable implements Runnable {
	private static final String TAG = DownloadRunnable.class.getSimpleName();
	
	private Context mContext;
	private DownloadMsgHandler mDownloadMsgHandler;
	private DownloadInfo mDownloadInfo;
	
	public FileOutputStream mStream;	//文件输出流
	public int mRedirectCount = 0;	//重定向次数
	public boolean mContinuingDownload = false;	//是否续传
	public String mHeaderContentLength;
	public long mTimeLastNotification = 0;

    /**
     * Raised from methods called by run() to indicate that the current request
     * should be stopped immediately.
     * 
     * Note the message passed to this exception will be logged and therefore
     * must be guaranteed not to contain any PII, meaning it generally can't
     * include any information about the request URI, headers, or destination
     * filename.
     */
    private class StopRequest extends Throwable {
		private static final long serialVersionUID = 1L;
	
		public int mFinalStatus;
	
		public StopRequest(int finalStatus, String message) {
		    super(message);
		    Log.e(TAG, "StopRequest: finalStatus = " + finalStatus + ", message = " + message);
		    mFinalStatus = finalStatus;
		}
	
		public StopRequest(int finalStatus, String message, Throwable throwable) {
		    super(message, throwable);
		    Log.e(TAG, "StopRequest: finalStatus = " + finalStatus + ", message = " + message);
		    mFinalStatus = finalStatus;
		}
    }

    /**
     * Raised from methods called by executeDownload() to indicate that the
     * download should be retried immediately.
     */
    private class RetryDownload extends Throwable {
    	private static final long serialVersionUID = 1L;
    }
	
	public DownloadRunnable(Context context, DownloadMsgHandler downloadMsgHandler, DownloadInfo downloadInfo) {
		mContext = context;
		mDownloadMsgHandler = downloadMsgHandler;
		mDownloadInfo = downloadInfo;
		
		mDownloadInfo.mBytesSoFar = 0;
		mDownloadInfo.mHasActiveThread = true;
	}
	
	/**
	 * 设置控制状态
	 * @param control
	 */
	public void setControl(int control) {
		mDownloadInfo.mControl = control;
	}

	/**
     * Executes the download in a separate thread
     */
	@Override
	public void run() {		
		reportStatusChanged(0, Downloads.STATUS_RUNNING);
		
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		AndroidHttpClient client = null;
		int finalStatus = Downloads.STATUS_UNKNOWN_ERROR;
		
		try {	
		    client = AndroidHttpClient.newInstance(userAgent(), mContext);			
		    HttpGet request = new HttpGet(mDownloadInfo.mUri);
			
			try {
			    executeDownload(client, request);
			} catch (RetryDownload exc) {
			    exc.printStackTrace();
			} finally {
			    request.abort();
			    request = null;
			}
			
		    finalizeDestinationFile();
		    finalStatus = Downloads.STATUS_SUCCESS;
		} catch (StopRequest error) {
		    // remove the cause before printing, in case it contains PII
		    error.printStackTrace();
		    finalStatus = error.mFinalStatus;
		    // fall through to finally block
		} catch (Throwable ex) { // sometimes the socket code throws unchecked exceptions
		    ex.printStackTrace();
		    finalStatus = Downloads.STATUS_UNKNOWN_ERROR;
		    // falls through to the code that reports an error
		} finally {
		    if (client != null) {
				client.close();
				client = null;
		    }
		    cleanupDestination(finalStatus);
		    reportStatusChanged(0, finalStatus);
		    mDownloadInfo.mHasActiveThread = false;
		}
	}

    /**
     * Fully execute a single download request - setup and send the request,
     * handle the response, and transfer the data to the destination file.
     */
    private void executeDownload(AndroidHttpClient client, HttpGet request) throws StopRequest, RetryDownload {
    	byte data[] = new byte[Constants.BUFFER_SIZE];
	
    	//获取断点续传信息
		setupDestinationFile();
		//组装请求头
		addRequestHeaders(request);
	
		// check just before sending the request to avoid using an invalid connection at all
		checkConnectivity();
	
		//发送请求
		HttpResponse response = sendRequest(client, request);
		//处理返回的错误码
		handleExceptionalStatus(response);
		//处理返回头信息
		processResponseHeaders(response);
		
		//传输数据
		InputStream entityStream;
		try {
			entityStream = response.getEntity().getContent();
		} catch (IOException ex) {
		    throw new StopRequest(getFinalStatusForHttpError(), "while getting entity: " + ex.toString(), ex);
		}
		transferData(data, entityStream);
    }

    /**
     * Prepare the destination file to receive data. If the file already exists,
     * we'll set up appropriately for resumption.
     */
    private void setupDestinationFile() throws StopRequest {
    	if (!TextUtils.isEmpty(mDownloadInfo.mFilePath)) { // only true if we've already run a thread for this download
		    if (!Helpers.isFilenameValid(mDownloadInfo.mFilePath)) {
				// this should never happen
				throw new StopRequest(Downloads.STATUS_FILE_ERROR, "found invalid internal destination filename");
		    }
		    
		    // We're resuming a download that got interrupted
		    File f = new File(mDownloadInfo.mFilePath);
		    if (f.exists()) {
				long fileLength = f.length();
				if (fileLength == 0) {
				    // The download hadn't actually started, we can restart from scratch
				    f.delete();
				} else if (mDownloadInfo.mETag == null) {
				    // This should've been caught upon failure
				    f.delete();
				    throw new StopRequest(Downloads.STATUS_CANNOT_RESUME, "Trying to resume a download that can't be resumed");
				} else {
				    // All right, we'll be able to resume this download
				    mDownloadInfo.mBytesSoFar = fileLength;
				    if (mDownloadInfo.mTotalBytes != -1) {
						mHeaderContentLength = Long.toString(mDownloadInfo.mTotalBytes);
				    }
				    mContinuingDownload = true;
				}
		    }
		}
    }

    /**
     * Add custom headers for this download to the HTTP request.
     */
    private void addRequestHeaders(HttpGet request) {
    	for (Pair<String, String> header : mDownloadInfo.getHeaders()) {
		    request.addHeader(header.first, header.second);
		}
	
		if (mContinuingDownload) {
		    if (mDownloadInfo.mETag != null) {
		    	request.addHeader("If-Match", mDownloadInfo.mETag); //If-Match:只有请求内容与实体相匹配才有效
		    }
		    request.addHeader("Range", "bytes=" + mDownloadInfo.mBytesSoFar + "-");
		}
    }

    /**
     * Check if current connectivity is valid for this request.
     */
    private void checkConnectivity() throws StopRequest {
		int networkUsable = mDownloadInfo.checkCanUseNetwork();
		if (networkUsable != DownloadInfo.NETWORK_OK) {
		    throw new StopRequest(Downloads.STATUS_WAITING_FOR_NETWORK, "no network usable");
		}
    }

    /**
     * Send the request to the server, handling any I/O exceptions.
     */
    private HttpResponse sendRequest(AndroidHttpClient client, HttpGet request) throws StopRequest {
		try {
		    return client.execute(request);
		} catch (IllegalArgumentException ex) {
		    throw new StopRequest(Downloads.STATUS_HTTP_DATA_ERROR, "while trying to execute request: " + ex.toString(), ex);
		} catch (IOException ex) {
		    throw new StopRequest(getFinalStatusForHttpError(), "while trying to execute request: " + ex.toString(), ex);
		}
    }

    /**
     * Check the HTTP response status and handle anything unusual (e.g. not
     * 200/206).
     */
    private void handleExceptionalStatus(HttpResponse response) throws StopRequest, RetryDownload {
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 503 && mDownloadInfo.mNumFailed < Constants.MAX_RETRIES) {
		    handleServiceUnavailable(response);
		}
		if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307) {
		    handleRedirect(response, statusCode);
		}
	
		int expectedStatus = mContinuingDownload ? 206 : Downloads.STATUS_SUCCESS;
		if (statusCode != expectedStatus) {
			int finalStatus;
			if (Downloads.isStatusError(statusCode)) {
			    finalStatus = statusCode;
			} else if (statusCode >= 300 && statusCode < 400) {
			    finalStatus = Downloads.STATUS_UNHANDLED_REDIRECT;
			} else if (mContinuingDownload
				&& statusCode == Downloads.STATUS_SUCCESS) {
			    finalStatus = Downloads.STATUS_CANNOT_RESUME;
			} else {
			    finalStatus = Downloads.STATUS_UNHANDLED_HTTP_CODE;
			}
			throw new StopRequest(finalStatus, "http error " + statusCode);
		}
    }

    /**
     * Read HTTP response headers and take appropriate action, including setting
     * up the destination file and updating the database.
     */
    private void processResponseHeaders(HttpResponse response) throws StopRequest {
		if (mContinuingDownload) {
		    // ignore response headers on resume requests
		    return;
		}
	
		Header header;
		/*Header header = response.getFirstHeader("Content-Disposition");
		if (header != null) {
		    innerState.mHeaderContentDisposition = header.getValue();
		}*/
		/*header = response.getFirstHeader("Content-Location");
		if (header != null) {
		    innerState.mHeaderContentLocation = header.getValue();
		}*/
		if (mDownloadInfo.mMimeType == null) {
		    header = response.getFirstHeader("Content-Type");
		    if (header != null) {
		    	mDownloadInfo.mMimeType = mDownloadInfo.sanitizeMimeType(header.getValue());
		    }
		}
		header = response.getFirstHeader("ETag");
		if (header != null) {
			mDownloadInfo.mETag = header.getValue();
		}
		String headerTransferEncoding = null;
		header = response.getFirstHeader("Transfer-Encoding");
		if (header != null) {
		    headerTransferEncoding = header.getValue();
		}
		if (headerTransferEncoding == null) {
		    header = response.getFirstHeader("Content-Length");
		    if (header != null) {
		    	mHeaderContentLength = header.getValue();
		    	mDownloadInfo.mTotalBytes = Long.parseLong(mHeaderContentLength);
		    }
		} else {
		    // Ignore content-length with transfer-encoding - 2616 4.4 3
		}
		
	    //Log.v(TAG, "readResponseHeaders: Content-Disposition = " + innerState.mHeaderContentDisposition);
	    Log.v(TAG, "readResponseHeaders: Content-Length = " + mHeaderContentLength);
	    //Log.v(TAG, "readResponseHeaders: Content-Location = " + innerState.mHeaderContentLocation);
	    Log.v(TAG, "readResponseHeaders: Content-Type = " + mDownloadInfo.mMimeType);
	    Log.v(TAG, "readResponseHeaders: ETag = " + mDownloadInfo.mETag);
	    Log.v(TAG, "readResponseHeaders: Transfer-Encoding = " + headerTransferEncoding);
	
		boolean noSizeInfo = mHeaderContentLength == null && (headerTransferEncoding == null || !headerTransferEncoding.equalsIgnoreCase("chunked"));
		if (noSizeInfo) {
		    throw new StopRequest(Downloads.STATUS_HTTP_DATA_ERROR, "can't know size of download, giving up");
		}
	
		try {
		    mStream = new FileOutputStream(mDownloadInfo.mFilePath);
		} catch (FileNotFoundException exc) {
		    throw new StopRequest(Downloads.STATUS_FILE_ERROR, "while opening destination file: " + exc.toString(), exc);
		}
		
		reportNetworkChanged(mDownloadInfo.mETag, mDownloadInfo.mMimeType);
    	
		// check connectivity again now that we know the total size
		checkConnectivity();
    }

    /**
     * Transfer as much data as possible from the HTTP response to the
     * destination file.
     * 
     * @param data
     *            buffer to use to read data
     * @param entityStream
     *            stream for reading the HTTP response entity
     */
    private void transferData(byte[] data, InputStream entityStream) throws StopRequest {
		for (;;) {
		    int bytesRead = readFromResponse(data, entityStream);
		    if (bytesRead == -1) { // success, end of stream already reached
		    	boolean lengthMismatched = (mHeaderContentLength != null) && (mDownloadInfo.mBytesSoFar != Integer.parseInt(mHeaderContentLength));
				if (lengthMismatched) {
				    if (cannotResume()) {
						throw new StopRequest(Downloads.STATUS_CANNOT_RESUME, "mismatched content length");
				    } else {
						throw new StopRequest(getFinalStatusForHttpError(), "closed socket before end of file");
				    }
				}
				return;
		    }
	
		    writeDataToDestination(data, bytesRead);
		    mDownloadInfo.mBytesSoFar += bytesRead;
		    
		    reportProgress();	
		    
		    checkPausedOrCanceled();
		}
    }

    /**
     * Close the destination output stream.
     */
    private void closeDestination() {
		try {
		    // close the file
		    if (mStream != null) {
				mStream.close();
				mStream = null;
		    }
		} catch (IOException ex) {
			Log.e(TAG, "closeDestination: exception when closing the file after download : " + ex);
		    // nothing can really be done if the file can't be closed
		}
    }

    private int getFinalStatusForHttpError() {
		if (!Helpers.isNetworkAvailable(mContext)) {
		    return Downloads.STATUS_WAITING_FOR_NETWORK;
		} else if (mDownloadInfo.mNumFailed < Constants.MAX_RETRIES) {
		    return Downloads.STATUS_WAITING_TO_RETRY;
		} else {
		    Log.e(TAG, "reached max retries for " + mDownloadInfo.mId);
		    return Downloads.STATUS_HTTP_DATA_ERROR;
		}
    }

    /**
     * Handle a 503 Service Unavailable status by processing the Retry-After
     * header.
     */
    private void handleServiceUnavailable(HttpResponse response) throws StopRequest {
		Header header = response.getFirstHeader("Retry-After");
		if (header != null) {
		    try {
				int retryAfter = Integer.parseInt(header.getValue());
				if (retryAfter < 0) {
				    retryAfter = 0;
				} else {
				    if (retryAfter < Constants.MIN_RETRY_AFTER) {
				    	retryAfter = Constants.MIN_RETRY_AFTER;
				    } else if (retryAfter > Constants.MAX_RETRY_AFTER) {
				    	retryAfter = Constants.MAX_RETRY_AFTER;
				    }
				    retryAfter += Helpers.sRandom.nextInt(Constants.MIN_RETRY_AFTER + 1);
				    retryAfter *= 1000;
				}
		    } catch (NumberFormatException ex) {
		    	// ignored - retryAfter stays 0 in this case.
		    }
		}
		throw new StopRequest(Downloads.STATUS_WAITING_TO_RETRY, "got 503 Service Unavailable, will retry later");
    }

    /**
     * Handle a 3xx redirect status.
     */
    private void handleRedirect(HttpResponse response, int statusCode) throws StopRequest, RetryDownload {
		if (mRedirectCount >= Constants.MAX_REDIRECTS) {
		    throw new StopRequest(Downloads.STATUS_TOO_MANY_REDIRECTS, "too many redirects");
		}
		Header header = response.getFirstHeader("Location");
		if (header == null) {
		    return;
		}
		
		String newUri;
		try {
		    newUri = new URI(mDownloadInfo.mUri).resolve(new URI(header.getValue())).toString();
		} catch (URISyntaxException ex) {
		    Log.e(TAG, "handleRedirect: Couldn't resolve redirect URI " + header.getValue() + " for " + mDownloadInfo.mUri);
		    
		    throw new StopRequest(Downloads.STATUS_HTTP_DATA_ERROR, "Couldn't resolve redirect URI");
		}
		++mRedirectCount;
		mDownloadInfo.mUri = newUri;
		if (statusCode == 301 || statusCode == 303) {
		    // use the new URI for all future requests (should a retry/resume be
		    // necessary)
		    reportUriChanged(newUri);
		}
		throw new RetryDownload();
    }

    /**
     * Read some data from the HTTP response stream, handling I/O errors.
     * 
     * @param data
     *            buffer to use to read data
     * @param entityStream
     *            stream for reading the HTTP response entity
     * @return the number of bytes actually read or -1 if the end of the stream
     *         has been reached
     */
    private int readFromResponse(byte[] data, InputStream entityStream) throws StopRequest {
		try {
		    return entityStream.read(data);
		} catch (IOException ex) {
		    reportProgress();	
		    if (cannotResume()) {
				String message = "while reading response: " + ex.toString() + ", can't resume interrupted download with no ETag";
				throw new StopRequest(Downloads.STATUS_CANNOT_RESUME, message, ex);
		    } else {
				throw new StopRequest(getFinalStatusForHttpError(), "while reading response: " + ex.toString(), ex);
		    }
		}
    }

    /**
     * 判断是否可以断点续传
     * @return
     */
    private boolean cannotResume() {
		return mDownloadInfo.mBytesSoFar > 0 && mDownloadInfo.mETag == null;
    }

    /**
     * Write a data buffer to the destination file.
     * 
     * @param data
     *            buffer containing the data to write
     * @param bytesRead
     *            how many bytes to write from the buffer
     */
    private void writeDataToDestination(byte[] data, int bytesRead) throws StopRequest {
    	for (;;) {
		    try {
				if (mStream == null) {
				    mStream = new FileOutputStream(mDownloadInfo.mFilePath, true);
				}
				mStream.write(data, 0, bytesRead);
				//closeDestination();
				return;
		    } catch (IOException ex) {
				if (!Helpers.isExternalMediaMounted()) {
				    throw new StopRequest( Downloads.STATUS_DEVICE_NOT_FOUND_ERROR, "external media not mounted while writing destination file");
				}
		
				long availableBytes = Helpers.getAvailableBytes(Helpers.getFilesystemRoot(mDownloadInfo.mFilePath));
				if (availableBytes < bytesRead) {
				    throw new StopRequest(Downloads.STATUS_INSUFFICIENT_SPACE_ERROR, "insufficient space while writing destination file", ex);
				}
				throw new StopRequest(Downloads.STATUS_FILE_ERROR, "while writing destination file: " + ex.toString(), ex);
		    }
		} 
    }

    /**
     * Check if the download has been paused or canceled, stopping the request
     * appropriately if it has been.
     */
    private void checkPausedOrCanceled() throws StopRequest {
    	synchronized (mDownloadInfo) {
		    if (mDownloadInfo.mControl == Downloads.CONTROL_PAUSED) {
		    	throw new StopRequest(Downloads.STATUS_PAUSED, "download paused by owner");
		    }
		    if (mDownloadInfo.mControl == Downloads.CONTROL_CANCEL) {
		    	throw new StopRequest(Downloads.STATUS_CANCELED, "download canceled");
		    }
		}
    }

    /**
     * Called after a successful completion to take any necessary action on the
     * downloaded file.
     */
    private void finalizeDestinationFile() {
		// make sure the file is readable
		FileUtils.setPermissions(mDownloadInfo.mFilePath, 0644, -1, -1);
		syncDestination();
    }

    /**
     * Sync the destination file to storage.
     */
    private void syncDestination() {
		FileOutputStream downloadedFileStream = null;
		try {
		    downloadedFileStream = new FileOutputStream(mDownloadInfo.mFilePath, true);
		    downloadedFileStream.getFD().sync();
		} catch (FileNotFoundException ex) {
		    Log.w(TAG, "syncDestination: file " + mDownloadInfo.mFilePath + " not found: " + ex);
		} catch (SyncFailedException ex) {
		    Log.w(TAG, "syncDestination: file " + mDownloadInfo.mFilePath + " sync failed: " + ex);
		} catch (IOException ex) {
		    Log.w(TAG, "syncDestination: IOException trying to sync " + mDownloadInfo.mFilePath + ": " + ex);
		} catch (RuntimeException ex) {
		    Log.w(TAG, "syncDestination: exception while syncing file: ", ex);
		} finally {
		    if (downloadedFileStream != null) {
				try {
				    downloadedFileStream.close();
				} catch (IOException ex) {
				    Log.w(TAG, "syncDestination: IOException while closing synced file: ", ex);
				} catch (RuntimeException ex) {
					Log.w(TAG, "syncDestination: exception while closing file: ", ex);
				}
		    }
		}
    }

    /**
     * Returns the user agent provided by the initiating app, or use the default
     * one
     */
    private String userAgent() {
		String userAgent = mDownloadInfo.mUserAgent;
		if (userAgent == null) {
		    userAgent = Constants.DEFAULT_USER_AGENT;
		}
		return userAgent;
    }

    /**
     * Called just before the thread finishes, regardless of status, to take any
     * necessary action on the downloaded file.
     */
    private void cleanupDestination(int finalStatus) {
		closeDestination();
		if (mDownloadInfo.mFilePath != null && Downloads.isStatusError(finalStatus)) {
		    new File(mDownloadInfo.mFilePath).delete();
		}
    }
    
    /**
     * ------------------------------------------------------------通知变化---------------------------------------------------------
     */
    
    /**
     * Report status changed
     */
    private void reportStatusChanged(int oldStatus, int newStatus) {
    	mDownloadInfo.mStatus = newStatus;
    	
    	mDownloadMsgHandler.sendStatusChanged(mDownloadInfo.mId, oldStatus, newStatus);
    }

    /**
     * Report download progress through the database if necessary.
     */
    private void reportProgress() {
		long now = System.currentTimeMillis();
		if (now - mTimeLastNotification > Constants.MIN_PROGRESS_TIME) {
		    mDownloadMsgHandler.sendProgressChangeMessage(mDownloadInfo.mId, mDownloadInfo.mTotalBytes, mDownloadInfo.mBytesSoFar);
		    mTimeLastNotification = now;
		}
    }
    
    /**
     * Report uri changed
     * @param uri
     */
    private void reportUriChanged(String uri) {
    	if (null != uri) {
    		mDownloadMsgHandler.sendUriMessage(mDownloadInfo.mId, uri);
    	}
    }
    
    private void reportNetworkChanged(String etag, String mimeType) {
    	mDownloadMsgHandler.sendNetworkMessage(mDownloadInfo.mId, etag, mimeType);
    }
}
