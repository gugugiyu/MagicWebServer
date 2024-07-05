package com.github.magic.core.config;

import com.github.magic.core.utils.VersionFinder;

import java.io.File;
import java.net.InetSocketAddress;

public final class Config {
    private Config(){}

    ////////////////////////////////////////
    // Server config                      //
    ////////////////////////////////////////

    //The root directory of the server (the MagicWebServer directory)
    public static final String ROOT_DIR = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParentFile().getParent();

    //Maximum time allow for each thread to process the request and produce the response (ms)
    public static final int THREAD_TIMEOUT_DURATION = 5000;

    //The min number of active thread per server (int)
    public static final int CORE_POOL_SIZE = 10;

    //The maximum number of thread to spawn when the request queue is full and require more threads to handle (int)
    public static final int MAX_POOL_SIZE = 20;

    //The time for extra threads (created when the queue is full) should stay idle for (s)
    public static final long KEEP_ALIVE_TIME = 500;

    //The time it takes to block the flow while InputStream is reading (Sent 408 Request Timeout in case this number is exceeded) (ms)
    public static final int THREAD_REQUEST_READ_TIMEOUT_DURATION = 10000;

    //Default host IP (bind to everything)
    public static final InetSocketAddress DEFAULT_HOST_IP = new InetSocketAddress("localhost", 80);

    ////////////////////////////////////////
    // SSL/HTTPS config                   //
    ////////////////////////////////////////

    //The ABSOLUTE path to the exported key (PKCS12 file) or keystore (.jks file)
    public static final String KEY_PATH = "";

    //The password of the given key
    public static final String KEY_PASSWORD = "";

    //The trust store path
    public static final String TRUST_STORE = "";

    //The protocol of which are being used
    public static final String[] SSL_PROTOCOLS = new String[]{"TLSv1.2", "TLSv1.1"};

    ////////////////////////////////////////
    // Serving config                     //
    ////////////////////////////////////////

    public static final String STATIC_DIR = ROOT_DIR + "\\data";

    //Used for "Keep-Alive" header, "max" field
    public static final int MAX_SERVE_PER_CONNECTION = 100;

    ////////////////////////////////////////
    // Encoder config                     //
    ////////////////////////////////////////

    public static final int ENCODER_BUFFER_SIZE = (1 << 11); //2048 bytes

    //Threshold used to determine whether an uncompressed file should be compressed based on its size
    public static final int COMPRESS_THRESHOLD = (1 << 15); //32768 bytes

    //Maximum bytes of content sent by server (If the file exceeds this number it'll be sent in chunk-based approach)
    public static final int BODY_BUFFER_SIZE = (1 << 20); //1048576 bytes

    public static final int MAXIMUM_CHUNK_SIZE = (1 << 16); //65536 bytes


    ////////////////////////////////////////
    // MISC config                        //
    ////////////////////////////////////////

    //Toggle verbose mode (shouldn't be used in case of server multi-threading as there's no synchronization process provided)
    public static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("verbose") )|| Boolean.parseBoolean(System.getenv("verbose")) ;

    //Toggle dump error to err stream behavior
    public static final boolean SHOW_ERROR = true;

    public static final int JAVA_VERSION = VersionFinder.getJavaMajorVersion();
}
