/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

/**
 *
 * @author xavierluiz
 */
public class SoftwareHttpManager implements Callable<HttpResponse> {
    
    public static final Logger LOG = Logger.getLogger("Software");
    
    private String payload;
    private String api;
    private String httpMethodName;
    private HttpClient httpClient;
    private String overridingJwt;
    
    public SoftwareHttpManager(String api, String httpMethodName, String payload, String overridingJwt, HttpClient httpClient) {
        this.payload = payload;
        this.api = api;
        this.httpMethodName = httpMethodName;
        this.httpClient = httpClient;
        this.overridingJwt = overridingJwt;
    }
    
    @Override
    public HttpResponse call() {
        HttpUriRequest req = null;
        try {
            HttpResponse response = null;

            switch (httpMethodName) {
                case HttpPost.METHOD_NAME:
                    req = new HttpPost("" + SoftwareUtil.API_ENDPOINT + this.api);
                    if (payload != null) {
                        //
                        // add the json payload
                        //
                        StringEntity params = new StringEntity(payload);
                        ((HttpPost)req).setEntity(params);
                    }   break;
                case HttpDelete.METHOD_NAME:
                    req = new HttpDelete(SoftwareUtil.API_ENDPOINT + "" + this.api);
                    break;
                default:
                    req = new HttpGet(SoftwareUtil.API_ENDPOINT + "" + this.api);
                    break;
            }

            String jwtToken = (overridingJwt != null) ? overridingJwt : FileManager.getItem("jwt");
            // obtain the jwt session token if we have it
            if (jwtToken != null) {
                req.addHeader("Authorization", jwtToken);
            }
            
            SoftwareUtil.TimesData timesData = SoftwareUtil.getTimesData();
            req.addHeader("X-SWDC-Plugin-Id", String.valueOf(SoftwareUtil.PLUGIN_ID));
            req.addHeader("X-SWDC-Plugin-Name", "Code Time");
            req.addHeader("X-SWDC-Plugin-Version", SoftwareUtil.getVersion());
            req.addHeader("X-SWDC-Plugin-OS", SoftwareUtil.getOs());
            req.addHeader("X-SWDC-Plugin-TZ", timesData.timezone);
            req.addHeader("X-SWDC-Plugin-Offset", String.valueOf(timesData.offset));

            req.addHeader("Content-type", "application/json");
            
            if (payload != null) {
                LOG.log(Level.INFO, "Sofware.com: Sending API request: {0}, payload: {1}", new Object[]{api, payload});
            } else {
                LOG.log(Level.INFO, "Sofware.com: Sending API request: {0}", api);
            }

            // execute the request
            response = httpClient.execute(req);

            //
            // Return the response
            //
            return response;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Code Time: Unable to make api request.{0}", e.getMessage());
        }

        return null;
    }
}
