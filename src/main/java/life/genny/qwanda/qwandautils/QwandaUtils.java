package life.genny.qwanda.qwandautils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;


public class QwandaUtils {


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BOLD = "\u001b[1m";

    private static final Logger log = Logger.getLogger(QwandaUtils.class);

    public static String apiGet(String getUrl, final String authToken, final int timeout) throws ClientProtocolException, IOException {

        //	log.debug("GET:" + getUrl + ":");


        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        HttpGet request = new HttpGet(getUrl);
        if (authToken != null) {
            request.addHeader("Authorization", "Bearer " + authToken); // Authorization": `Bearer
        }

        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(request);
            // The underlying HTTP connection is still held by the response object
            // to allow the response content to be streamed directly from the network
            // socket.
            // In order to ensure correct deallocation of system resources
            // the user MUST call CloseableHttpResponse#close() from a finally clause.
            // Please note that if response content is not fully consumed the underlying
            // connection cannot be safely re-used and will be shut down and discarded
            // by the connection manager.

            HttpEntity entity1 = response.getEntity();
            if (entity1 == null) {
                return "";
            }
            String responseString = EntityUtils.toString(entity1);

            if (StringUtils.isBlank(responseString)) {
                return "";
            }

            EntityUtils.consume(entity1);

            return responseString;
        } catch (java.net.SocketTimeoutException e) {
            log.error("API Get call timeout - " + timeout + " secs to " + getUrl);
            return null;
        } catch (Exception e) {
            log.error("API Get exception -for  " + getUrl + " :");
            return "";
        } finally {
            if (response != null) {
                response.close();
            }
            httpclient.close();
            //IOUtils.closeQuietly(response);  removed commons-io
            //IOUtils.closeQuietly(httpclient);
        }

    }


    public static String apiGet(String getUrl, final String authToken) throws ClientProtocolException, IOException {

        return apiGet(getUrl, authToken, 200);
    }


    public static String apiPostEntity(final String postUrl, final String entityString, final String authToken, final Consumer<String> callback)
            throws IOException {
        String responseString = null;
        if (StringUtils.isBlank(postUrl)) {
            log.error("Blank url in apiPostEntity");
        }
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {

            HttpPost post = new HttpPost(postUrl);

            StringEntity postEntity = new StringEntity(entityString, "UTF-8");

            post.setEntity(postEntity);
            post.setHeader("Content-Type", "application/json; charset=UTF-8");
            if (authToken != null) {
                post.addHeader("Authorization", "Bearer " + authToken); // Authorization": `Bearer
            }

            response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);
            if (callback != null) {
                callback.accept(responseString);
            }
            return responseString;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            } else {
                log.error("postApi response was null");
            }
            httpclient.close();
            //	IOUtils.closeQuietly(response);
            //	IOUtils.closeQuietly(httpclient);
        }
        return responseString;
    }


    public static String apiPostNote(final String postUrl, final String sourceCode, final String tag, final String targetCode, final String content, final String authToken, final Consumer<String> callback)
            throws IOException {
        String responseString = null;
        if (StringUtils.isBlank(postUrl)) {
            log.error("Blank url in apiPostNote");
        }
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {

            HttpPost post = new HttpPost(postUrl);

            String jsonString = String.format("{\"id\":0,\"content\":\"" + content + "\",\"sourceCode\":\"" + sourceCode + "\",\"tags\":[{\"name\":\"" + sourceCode + "\",\"value\":0}, {\"name\":\"sys\",\"value\":0}],\"targetCode\":\"" + targetCode + "\"}");

            StringEntity noteContent = new StringEntity(jsonString, "UTF-8");

            post.setEntity(noteContent);
            post.setHeader("Content-Type", "application/json; charset=UTF-8");
            if (authToken != null) {
                post.addHeader("Authorization", "Bearer " + authToken); // Authorization": `Bearer
            }

            response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();
            responseString = EntityUtils.toString(entity);
            if (callback != null) {
                callback.accept(responseString);
            }
            return responseString;
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            } else {
                log.error("postApi response was null");
            }
            httpclient.close();
            //	IOUtils.closeQuietly(response);
            //	IOUtils.closeQuietly(httpclient);
        }
        return responseString;
    }

    public static String apiPostEntity(final String postUrl, final String entityString, final String authToken) throws IOException {
        return apiPostEntity(postUrl, entityString, authToken, null);
    }


    public static String apiPost(final String postUrl, final List<BasicNameValuePair> nameValuePairs, final String authToken) throws IOException {
        return apiPostEntity(postUrl, new UrlEncodedFormEntity(nameValuePairs).toString(), authToken, null);
    }


    public static String getNormalisedUsername(final String rawUsername) {
        if (rawUsername == null) {
            return null;
        }
        String username = rawUsername.replaceAll("\\&", "_AND_").replaceAll("@", "_AT_").replaceAll("\\.", "_DOT_")
                .replaceAll("\\+", "_PLUS_").toUpperCase();
        // remove bad characters
        username = username.replaceAll("[^a-zA-Z0-9_]", "");
        return username;

    }
}
