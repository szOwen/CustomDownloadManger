package com.example.customdownload;

/**
 * Contains the internal constants that are used in the download manager.
 * As a general rule, modifying these constants should be done with care.
 */
public class Constants {
	/** The default user agent used for downloads */
    public static final String DEFAULT_USER_AGENT = "CustomDownloadManager";

    /** The buffer size used to stream the data */
    public static final int BUFFER_SIZE = 4096;

    /** The minimum amount of time that has to elapse before the progress bar gets updated, in ms */
    public static final long MIN_PROGRESS_TIME = 1000;

    /**
     * The number of times that the download manager will retry its network
     * operations when no progress is happening before it gives up.
     */
    public static final int MAX_RETRIES = 5;

    /**
     * 503 Service Unavailable 由于临时的过载或者服务器管理导致的服务器暂时不可用。这个服务器可以在应答中增加一个Retry-After来让客户端重试这个请求。如果没有Retry-After指出，客户端必须就像收到了一个500（Server Internal Error）应答一样处理。 
     * The minimum amount of time that the download manager accepts for
     * a Retry-After response header with a parameter in delta-seconds.
     */
    public static final int MIN_RETRY_AFTER = 30; // 30s

    /**
     * The maximum amount of time that the download manager accepts for
     * a Retry-After response header with a parameter in delta-seconds.
     */
    public static final int MAX_RETRY_AFTER = 24 * 60 * 60; // 24h

    /**
     * The maximum number of redirects.
     */
    public static final int MAX_REDIRECTS = 5; // can't be more than 7.
}
