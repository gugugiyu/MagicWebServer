package com.github.magic.core.middleware;

import com.github.magic.core.consts.HttpCode;
import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.models.Request;
import com.github.magic.core.models.Response;
import com.github.magic.core.models.header.Headers;
import com.github.magic.core.models.server.Server;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Cors implements Middleware {
    private final CorsOption corsOption;
    private final Server serverInstance;

    public Cors(Server serverInstance, CorsOption corsOption) {
        this.serverInstance = serverInstance;
        this.corsOption = corsOption;
    }

    public Cors(Server serverInstance){
        this.serverInstance = serverInstance;
        this.corsOption = CorsOption.getDefault();
    }

    @Override
    public void handle(Request req, Response res, NextCallback next){
        URI        origin  = req.getRequestOrigin();
        HttpMethod method  = req.getMethod();
        Headers    headers = req.getHeaders();

        //If it isn't cross-site request, then ignore cors
        if (origin != null && origin.toString().isEmpty() || isIdenticalOrigin(origin, serverInstance))
            next.next();

        // Origins
        // Access-Control-Allow-Origin wildcard can't be used with credentials (https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS/Errors/CORSNotSupportingCredentials)
        if (corsOption.allowAllOrigins && !corsOption.credentials) {
            res.setHeader("Access-Control-Allow-Origin", "*");
        } else if (corsOption.allowedOrigins.contains(origin.toString())) {
            res.setHeader("Access-Control-Allow-Origin", req.getRequestOrigin().toString());
        }

        //Methods
        if (corsOption.allowAllMethods) {
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        } else if (method != null && corsOption.allowedOrigins.contains(origin.toString())) {
            res.setHeader("Access-Control-Allow-Methods", req.getMethod().toString());
        }

        //Allow headers (from client to server)
        ArrayList<String> keyList = headers.getKeyList();
        if (corsOption.allowAllHeaders) {
            res.setHeader("Access-Control-Allow-Headers", "*");
        } else if (keyList != null && new HashSet<>(corsOption.allowedOrigins).containsAll(keyList)) {
            res.setHeader("Access-Control-Allow-Headers", String.join(",", headers.getValueList()));
        }

        //Exposed header (from server to client)
        //Reason to explicitly add "authorization" header (https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers)
        //However, Authorization isn't supported by this server (yet)
        if (corsOption.exposedAllHeaders){
            res.setHeader("Access-Control-Expose-Headers", "*");
        }else if (!corsOption.exposedHeaders.isEmpty()){
            res.setHeader("Access-Control-Expose-Headers", String.join(",", corsOption.exposedHeaders));
        }

        //Credential
        if (corsOption.credentials)
            res.setHeader("Access-Control-Allow-Credentials", "true");

        //maxAge
        //Default value for this is 5 seconds, please check the default initialization of cors option for more info
        res.setHeader("Access-Control-Max-Age", "" + Math.max(corsOption.maxAge, 5));

        //Option success status
        if (method == HttpMethod.OPTIONS && isOptionStatusValid()){
            res.setStatus((short) corsOption.optionsSuccessStatus);
        }

        next.next();
    }

    private boolean isOptionStatusValid(){
        return corsOption.optionsSuccessStatus > 0 && corsOption.optionsSuccessStatus < 600;
    }

    public boolean isIdenticalOrigin(URI origin, Server server){
        return  origin.getPort() == server.getPort()
                && origin.getHost().equalsIgnoreCase(server.getHostname())
                && origin.getScheme().equalsIgnoreCase(server.getScheme());
    }

    public static class CorsOption {
        private List<String> allowedOrigins;
        private boolean allowAllOrigins;

        private List<HttpMethod> allowedMethods = new ArrayList<>();
        private boolean allowAllMethods;

        private List<String> allowedHeaders = new ArrayList<>();
        private boolean allowAllHeaders;

        private int optionsSuccessStatus;

        //WIP field
        private int maxAge;
        private boolean credentials;

        //Expose more headers to the safe list CORS request
        private List<String> exposedHeaders = new ArrayList<>();
        private boolean exposedAllHeaders;


        public static CorsOption getDefault(){
            CorsOption defaultCorsOption = new CorsOption();

            defaultCorsOption.allowAllOrigins = true;
            defaultCorsOption.allowAllHeaders = true;
            defaultCorsOption.allowAllMethods = true;

            defaultCorsOption.credentials = false; //Don't send cookies (or any form of credential) in the request

            defaultCorsOption.exposedAllHeaders = false;

            /**
             * According to the MDN:
             * - Maximum number of seconds the results can be cached, as an unsigned non-negative integer.
             * - Firefox caps this at 24 hours (86400 seconds).
             * - Chromium (prior to v76) caps at 10 minutes (600 seconds).
             * - Chromium (starting in v76) caps at 2 hours (7200 seconds).
             * The default value is 5 seconds.
             */
            defaultCorsOption.maxAge = 5;

            //More info about this field, visit expressJs site
            //https://expressjs.com/en/resources/middleware/cors.html

            //TL;DR: The status will be use for CORS preflight request as some legacy browsers don't support code of 204 No Content
            defaultCorsOption.optionsSuccessStatus = HttpCode.OK;

            return defaultCorsOption;
        }


        public CorsOption setAllowedOrigins(String origins) {
            this.allowedOrigins = new ArrayList<>();
            allowedOrigins.add(origins);
            return this;
        }

        public CorsOption setAllowedMethods(ArrayList<HttpMethod> allowedMethods) {
            this.allowedMethods = allowedMethods;
            return this;
        }

        public CorsOption setAllowedOrigins(ArrayList<String> origins) {
            this.allowedOrigins = origins;
            return this;
        }

        public CorsOption allowAllOrigins(boolean allowAllOrigins) {
            this.allowAllOrigins = allowAllOrigins;
            return this;
        }

        public CorsOption allowAllHeaders(boolean allowAllHeaders) {
            this.allowAllHeaders = allowAllHeaders;
            return this;
        }

        public CorsOption allowAllMethods(boolean allowAllMethods) {
            this.allowAllMethods = allowAllMethods;
            return this;
        }

        public CorsOption setAllowedHeaders(ArrayList<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
            return this;
        }

        public CorsOption setOptionsSuccessStatus(int optionsSuccessStatus) {
            this.optionsSuccessStatus = optionsSuccessStatus;
            return this;
        }

        public CorsOption setMaxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public CorsOption setCredentials(boolean credentials) {
            this.credentials = credentials;
            return this;
        }

        public CorsOption setExposedHeaders(ArrayList<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
            return this;
        }

        public CorsOption exposeAllHeader(boolean exposedAllHeaders) {
            this.exposedAllHeaders = exposedAllHeaders;
            return this;
        }
    }
}
