package com.eka.middleware.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;

public class TestServer {
	public static void main(String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "76.65.110.110")
                .setHandler(new BlockingHandler(new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange)
                            throws Exception {
                    	exchange.getResponseHeaders()
                        .put(Headers.CONTENT_TYPE, "text/plain");
                    	OutputStream os= exchange.getOutputStream();
                    	FileInputStream fis=new FileInputStream(new File("D:/Middleware/gui/middleware/pub/server/ui/welcome/index.html"));
                    	IOUtils.copy(fis, os);
                    	File f=new File("");
                    	f.getName();
                    	URLConnection connection = f.toURL().openConnection();
                    	connection.getInputStream().close();
                        os.flush();
                        os.close();
                        fis.close();
                        exchange.endExchange();
                    }
                })).build();
        server.start();
    }
}
