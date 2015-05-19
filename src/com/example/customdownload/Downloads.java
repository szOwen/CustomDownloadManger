package com.example.customdownload;

import android.provider.BaseColumns;

public class Downloads implements BaseColumns {


    /**
     * Returns whether the status is an error (i.e. 4xx or 5xx).
     * 
     * @hide
     */
    public static boolean isStatusError(int status) {
    	return (status >= 400 && status < 600);
    }

    /**
     * Returns whether the download has completed (either with success or
     * error).
     * 
     * @hide
     */
    public static boolean isStatusCompleted(int status) {
		return (status >= 200 && status < 300) || (status >= 400 && status < 600);
    }

    /**
     * This download is allowed to run.
     * 
     * @hide
     */
    public static final int CONTROL_RUN = 0;

    /**
     * This download must pause at the first opportunity.
     * 
     * @hide
     */
    public static final int CONTROL_PAUSED = 1;
    
    /**
     * This download must pause at the first opportunity.
     * 
     * @hide
     */
    public static final int CONTROL_CANCEL = 2;

    /**
     * 正在取消任务
     * 
     * @hide
     */
    public static final int STATUS_CANCEL_PENDING = 191;

    /**
     * 正在删除任务
     * 
     * @hide
     */
    public static final int STATUS_DELETE_PENDING = 192;

    /**
     * 正在停止任务
     * 
     * @hide
     */
    public static final int STATUS_PAUSE_PENDING = 193;

    /**
     * 正在开始任务
     * 
     * @hide
     */
    public static final int STATUS_START_PENDING = 194;

    /**
     * This download has started
     * 
     * @hide
     */
    public static final int STATUS_RUNNING = 194;
    
    /**
     * This download has been paused by user
     */
    public static final int STATUS_PAUSED = 195;

    /**
     * This download has been paused by the owning app.
     */
    public static final int STATUS_STOP = 196;

    /**
     * This download encountered some network error and is waiting before
     * retrying the request.
     */
    public static final int STATUS_WAITING_TO_RETRY = 197;

    /**
     * This download is waiting for network connectivity to proceed.
     */
    public static final int STATUS_WAITING_FOR_NETWORK = 198;

    /**
     * This download exceeded a size limit for mobile networks and is waiting
     * for a Wi-Fi connection to proceed.
     */
    public static final int STATUS_QUEUED_FOR_WIFI = 199;

    /**
     * This download has successfully completed. Warning: there might be other
     * status values that indicate success in the future. Use isSucccess() to
     * capture the entire category.
     * 
     * @hide
     */
    public static final int STATUS_SUCCESS = 200;

    /**
     * This download can't be performed because the content type cannot be
     * handled.
     * 
     * @hide
     */
    public static final int STATUS_NOT_ACCEPTABLE = 406;

    /**
     * The requested destination file already exists.
     */
    public static final int STATUS_FILE_ALREADY_EXISTS_ERROR = 488;

    /**
     * Some possibly transient error occurred, but we can't resume the download.
     */
    public static final int STATUS_CANNOT_RESUME = 489;

    /**
     * This download was canceled
     * 
     * @hide
     */
    public static final int STATUS_CANCELED = 490;
    
	/**
     * This download has completed with an error. Warning: there will be other
     * status values that indicate errors in the future. Use isStatusError() to
     * capture the entire category.
     * 
     * @hide
     */
    public static final int STATUS_UNKNOWN_ERROR = 491;

    /**
     * This download couldn't be completed because of a storage issue.
     * Typically, that's because the filesystem is missing or full. Use the more
     * specific {@link #STATUS_INSUFFICIENT_SPACE_ERROR} and
     * {@link #STATUS_DEVICE_NOT_FOUND_ERROR} when appropriate.
     * 
     * @hide
     */
    public static final int STATUS_FILE_ERROR = 492;

    /**
     * This download couldn't be completed because of an HTTP redirect response
     * that the download manager couldn't handle.
     * 
     * @hide
     */
    public static final int STATUS_UNHANDLED_REDIRECT = 493;

    /**
     * This download couldn't be completed because of an unspecified unhandled
     * HTTP code.
     * 
     * @hide
     */
    public static final int STATUS_UNHANDLED_HTTP_CODE = 494;

    /**
     * This download couldn't be completed because of an error receiving or
     * processing data at the HTTP level.
     * 
     * @hide
     */
    public static final int STATUS_HTTP_DATA_ERROR = 495;

    /**
     * This download couldn't be completed because there were too many
     * redirects.
     * 
     * @hide
     */
    public static final int STATUS_TOO_MANY_REDIRECTS = 497;

    /**
     * This download couldn't be completed due to insufficient storage space.
     * Typically, this is because the SD card is full.
     * 
     * @hide
     */
    public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 498;

    /**
     * This download couldn't be completed because no external storage device
     * was found. Typically, this is because the SD card is not mounted.
     * 
     * @hide
     */
    public static final int STATUS_DEVICE_NOT_FOUND_ERROR = 499;
    
    /**
     * The name of the column containing the URI of the data being downloaded.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init/Read
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_URI = "uri";

    /**
     * The name of the column containing application-specific data.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init/Read/Write
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_APP_DATA = "entity";

    /**
     * The name of the column containing the filename that the initiating
     * application recommends. When possible, the download manager will attempt
     * to use this filename, or a variation, as the actual name for the file.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_FILE_NAME = "filename";

    /**
     * 文件存储路径
     * 
     * @hide
     */
    public static final String COLUMN_FILE_PATH = "filepath";

    /**
     * The name of the column containing the MIME type of the downloaded data.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init/Read
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_MIME_TYPE = "mimetype";

    /**
     * The name of the column containing the flags that controls whether the
     * download is displayed by the UI. See the VISIBILITY_* constants for a
     * list of legal values.
     * <P>
     * Type: INTEGER
     * </P>
     * <P>
     * Owner can Init/Read/Write
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_VISIBILITY = "visibility";

    /**
     * The name of the column containing the current status of the download.
     * Applications can read this to follow the progress of each download. See
     * the STATUS_* constants for a list of legal values.
     * <P>
     * Type: INTEGER
     * </P>
     * <P>
     * Owner can Read
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_STATUS = "status";

    /**
     * The name of the column containing the date at which some interesting
     * status changed in the download. Stored as a System.currentTimeMillis()
     * value.
     * <P>
     * Type: BIGINT
     * </P>
     * <P>
     * Owner can Read
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_LAST_MODIFICATION = "lastmod";

    /**
     * The name of the column contain the values of the cookie to be used for
     * the download. This is used directly as the value for the Cookie: HTTP
     * header that gets sent with the request.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_COOKIE_DATA = "cookiedata";

    /**
     * The name of the column containing the user agent that the initiating
     * application wants the download manager to use for this download.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_USER_AGENT = "useragent";

    /**
     * The name of the column containing the referer (sic) that the initiating
     * application wants the download manager to use for this download.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_REFERER = "referer";

    /**
     * The name of the column containing the total size of the file being
     * downloaded.
     * <P>
     * Type: INTEGER
     * </P>
     * <P>
     * Owner can Read
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_TOTAL_BYTES = "total_bytes";

    /**
     * The name of the column containing the size of the part of the file that
     * has been downloaded so far.
     * <P>
     * Type: INTEGER
     * </P>
     * <P>
     * Owner can Read
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_CURRENT_BYTES = "current_bytes";

    /** The column that is used for the downloads's ETag */
    public static final String COLUMN_ETAG = "etag";

    /**
     * The name of the column where the initiating application can provide the
     * description of this download. The description will be displayed to the
     * user in the list of downloads.
     * <P>
     * Type: TEXT
     * </P>
     * <P>
     * Owner can Init/Read/Write
     * </P>
     * 
     * @hide
     */
    public static final String COLUMN_DESCRIPTION = "description";

    /**
     * The name of the column indicating whether the download was requesting
     * through the public API. This controls some differences in behavior.
     * <P>
     * Type: BOOLEAN
     * </P>
     * <P>
     * Owner can Init/Read
     * </P>
     */
    public static final String COLUMN_IS_PUBLIC_API = "is_public_api";

    /**
     * The name of the column indicating whether roaming connections can be
     * used. This is only used for public API downloads.
     * <P>
     * Type: BOOLEAN
     * </P>
     * <P>
     * Owner can Init/Read
     * </P>
     */
    public static final String COLUMN_ALLOW_ROAMING = "allow_roaming";
}
