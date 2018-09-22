package com.precisionhawk.poleams.support.httpclient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 *
 * @author pchapman
 */
public class HttpClientUtilities {
    
    private final static CloseableHttpClient CLIENT = HttpClients.createDefault();
    
    private HttpClientUtilities() {}
    
    public static void postFile(URI uri, String contentType, File file) throws IOException
    {
        HttpPost httpPost = new HttpPost(uri);
        uploadFile(httpPost, contentType, file);
    }
    
    public static void putFile(URI uri, String contentType, File file) throws IOException
    {
        HttpPost httpPut = new HttpPost(uri);
        uploadFile(httpPut, contentType, file);
    }
    
    private static void uploadFile(HttpEntityEnclosingRequestBase req, String contentType, File file) throws IOException
    {
        try {
            req.setHeader("content-disposition", String.format("attachment; filename=\"%s\"", file.getName()));
            FileEntity content = new FileEntity(file, contentType);
            req.setEntity(content);
            CLIENT.execute(req);
        } finally {
            req.reset();
        }
    }
}
