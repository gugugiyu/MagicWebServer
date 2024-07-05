package com.github.magic.ssl.models;

import com.github.magic.core.config.Config;
import com.github.magic.core.models.routing_tries.URITries;
import com.github.magic.core.models.server.Server;
import com.github.magic.ssl.validator.SSLValidator;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;

public class SSLServer extends Server {
    public SSLServer(int port, URITries routingTree) {
        super(port, routingTree);
    }

    public SSLServer(URITries routingTree) {
        this(443, routingTree);
    }

    public SSLServer(int port) {
        this(port, new URITries());
    }

    public SSLServer() {
        this(443, new URITries());
    }

    public void setKeyStore(String path) {
        if (path == null || path.isEmpty()) return;

        path = SSLValidator.checkPath(path);

        if (path.isEmpty()) return;

        if (Config.VERBOSE) System.out.println("[+] Using keyStore at: " + path);

        System.setProperty("javax.net.ssl.keyStore", path);
    }

    public void setKeyStorePassword(String password) {
        if (password == null || password.isEmpty()) return;

        String formattedPassword = password.trim().replaceAll("\\r\\n|[\\r\\n]", "");
        System.setProperty("javax.net.ssl.keyStorePassword", formattedPassword);
    }


    @Override
    protected ServerSocket configureServer() throws IOException {
        setServerSocketFactory(SSLServerSocketFactory.getDefault());
        ServerSocket serverSocket = getServerSocket();

        ((SSLServerSocket) serverSocket).setWantClientAuth(true);
        ((SSLServerSocket) serverSocket).setEnabledProtocols(Config.SSL_PROTOCOLS);


        return serverSocket;
    }

    @Override
    public String getScheme() {
        return "https";
    }


    @Override
    public void setUpgradeInsecureRequest(String hostname, int port) {
        /* Does nothing as we're already in the HTTPS server */
    }

    @Override
    public String getUpgradeInsecureRequestURL() {
        return super.getUpgradeInsecureRequestURL();
    }
}
