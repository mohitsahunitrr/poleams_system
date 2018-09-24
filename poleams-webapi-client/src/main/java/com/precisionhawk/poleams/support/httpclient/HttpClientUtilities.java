package com.precisionhawk.poleams.support.httpclient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 *
 * @author Philip A. Chapman
 */
public class HttpClientUtilities {
    
    private final static CloseableHttpClient CLIENT = HttpClients.createDefault();
    
    private HttpClientUtilities() {}
    
    public static void postFile(URI uri, String authJWT, String contentType, File file) throws IOException
    {
        HttpPost httpPost = new HttpPost(uri);
        uploadFile(httpPost, authJWT, contentType, file);
    }
    
    public static void putFile(URI uri, String authJWT, String contentType, File file) throws IOException
    {
        HttpPost httpPut = new HttpPost(uri);
        uploadFile(httpPut, authJWT, contentType, file);
    }
    
    private static void uploadFile(HttpEntityEnclosingRequestBase req, String authJWT, String contentType, File file) throws IOException
    {
        try {
            req.setHeader("Authorized", "Bearer " + authJWT);
            req.setHeader("content-disposition", String.format("attachment; filename=\"%s\"", file.getName()));
            FileEntity content = new FileEntity(file, ContentType.create(contentType));
            req.setEntity(content);
            CLIENT.execute(req);
        } finally {
            req.reset();
        }
    }
}
