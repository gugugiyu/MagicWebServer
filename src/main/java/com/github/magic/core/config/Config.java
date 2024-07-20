package com.github.magic.core.config;

import com.github.magic.core.utils.VersionFinder;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class Config {
    private Config(){}
    
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

    //The root directory of the server (the MagicWebServer directory)
    public static String ROOT_DIR = URLDecoder.decode(new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParent(), StandardCharsets.UTF_8);

    public static final String STATIC_DIR = ROOT_DIR + "\\src\\main\\resources\\data";

    //Used for "Keep-Alive" header, "max" field
    public static final int MAX_SERVE_PER_CONNECTION = 100;

    ////////////////////////////////////////
    // Encoder config                     //
    ////////////////////////////////////////

    public static final int ENCODER_BUFFER_SIZE = (1 << 15); //8192 bytes

    //Threshold used to determine whether an uncompressed file should be compressed based on its size
    public static final int COMPRESS_THRESHOLD = (1 << 15); //32768 bytes

    //Maximum bytes of content sent by server (If the file exceeds this number it'll be sent in chunk-based approach)
    public static final int BODY_BUFFER_SIZE = (1 << 20); //1048576 bytes

    public static final int MAXIMUM_CHUNK_SIZE = (1 << 16); //65536 bytes


    ////////////////////////////////////////
    // MISC config                        //
    ////////////////////////////////////////

    //Toggle verbose mode (shouldn't be used in case of server multi-threading as there's no synchronization process provided)
    public static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("verbose") ) || Boolean.parseBoolean(System.getenv("verbose"));

    //Toggle dump error to err stream behavior
    public static final boolean SHOW_ERROR = Boolean.parseBoolean(System.getProperty("error") )|| Boolean.parseBoolean(System.getenv("error")) ;

    public static final int JAVA_VERSION = VersionFinder.getJavaMajorVersion();
}