package com.github.magic.core.consts;

import java.util.Arrays;

public class HttpDes {
    //Table from jHttp to map between status code and status description
    public static final String[] statuses = new String[600];

    static {
        // initialize status descriptions lookup table
        Arrays.fill(statuses, "Unknown Status");
        statuses[100] = "Continue";
        statuses[200] = "OK";
        statuses[201] = "Created";
        statuses[204] = "No Content";
        statuses[206] = "Partial Content";
        statuses[301] = "Moved Permanently";
        statuses[302] = "Found";
        statuses[304] = "Not Modified";
        statuses[307] = "Temporary Redirect";
        statuses[308] = "Permanent Redirect";
        statuses[400] = "Bad Request";
        statuses[401] = "Unauthorized";
        statuses[403] = "Forbidden";
        statuses[404] = "Not Found";
        statuses[405] = "Method Not Allowed";
        statuses[408] = "Request Timeout";
        statuses[412] = "Precondition Failed";
        statuses[413] = "Content Too Large";
        statuses[414] = "URI Too Long";
        statuses[416] = "Range Not Satisfiable";
        statuses[417] = "Expectation Failed";
        statuses[500] = "Internal Server Error";
        statuses[501] = "Not Implemented";
        statuses[502] = "Bad Gateway";
        statuses[503] = "Service Unavailable";
        statuses[504] = "Gateway Timeout";
        statuses[505] = "HTTP Version Not Supported";
    }
}
