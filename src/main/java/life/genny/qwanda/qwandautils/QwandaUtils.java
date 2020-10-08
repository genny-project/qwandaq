package life.genny.qwanda.qwandautils;

import life.genny.qwanda.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
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

    /**
     * @param baseEntAttributeCode
     * @param token
     * @return Deserialized BaseEntity model object with values for a BaseEntity code that is passed
     * @throws IOException
     */
    public static <T extends BaseEntity> T getBaseEntityByCode(String baseEntAttributeCode, String token) throws IOException {

        String attributeString = null;
        T be = null;
        try {
            attributeString = QwandaUtils
                    .apiGet(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/" + baseEntAttributeCode, token);
            be = JsonUtils.fromJson(attributeString, BaseEntity.class);
            if (be == null) {
                throw new IOException("Cannot find BE " + baseEntAttributeCode);
            }

        } catch (IOException e) {
            throw new IOException("Cannot connect to QwandaURL " + GennySettings.qwandaServiceUrl);
        }


        return be;
    }

    public static QDataBaseEntityMessage fetchResults(final BaseEntity searchBE, final String token) throws IOException {
        QDataBaseEntityMessage results = null;
        SearchEntity se = new SearchEntity(searchBE);
        log.info("se=" + se.getCode());
        //	if (searchBE.getCode().startsWith("SBE_")) {

        String jsonSearchBE = JsonUtils.toJson(searchBE);
        String result = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search", jsonSearchBE,
                token);

        results = JsonUtils.fromJson(result, QDataBaseEntityMessage.class);
        //		} else {
        //			throw new IllegalArgumentException("Must only send SearchBaseEntities - "+searchBE.getCode());
        //		}
        return results;

    }


    public static String getInitials(String[] strarr) {

        String initials = "";

        for (String str : strarr) {
            log.info("str :" + str);
            initials = str != null && str.length() > 0 ? initials.concat(str.substring(0, 2)) : initials.concat("");
        }

        return initials.toUpperCase();
    }


    public static String getUniqueId(String prefix) {
        return QwandaUtils.getUniqueId(prefix, null);
    }

    public static String getUniqueId(String prefix, String author) {

        String uniqueID = UUID.randomUUID().toString().replaceAll("-", "");

        String nameInitials = "";
        if (author != null) {
            nameInitials = getInitials(author.split("\\s+"));
        }

        if (prefix.endsWith("_")) {
            prefix = StringUtils.removeEnd(prefix, "_");
        }
        String ret = prefix + "_" + nameInitials + uniqueID;
        ret = ret.toUpperCase();

        return ret;
    }


    public static BaseEntity createBaseEntityByCode(String entityCode, String name, String qwandaUrl, String token) {
        BaseEntity beg = new BaseEntity(entityCode, name);
        GennyToken userToken = new GennyToken(token);
        beg.setRealm(userToken.getRealm());

        String jsonBE = JsonUtils.toJson(beg);
        try {
            // save BE
            String idStr = QwandaUtils.apiPostEntity(qwandaUrl + "/qwanda/baseentitys", jsonBE, token);
            Long id = Long.parseLong(idStr);
            beg.setId(id);
        } catch (Exception e) {
            log.warn("Baseentity code " + entityCode + " not found");
        }

        return beg;

    }

    public static String apiPutEntity(final String postUrl, final String entityString, final String authToken)
            throws IOException {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {

            HttpPut post = new HttpPut(postUrl);

            StringEntity postEntity = new StringEntity(entityString, "UTF-8");

            post.setEntity(postEntity);
            post.setHeader("Content-Type", "application/json; charset=UTF-8");
            if (authToken != null) {
                post.addHeader("Authorization", "Bearer " + authToken); // Authorization": `Bearer
            }

            response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            return responseString;
        } finally {
            response.close();
            httpclient.close();
            //IOUtils.closeQuietly(response);
            //IOUtils.closeQuietly(httpclient);
        }

    }

    public static QDataBaseEntityMessage getDataBEMessage(String groupCode, String linkCode, String token) {

        QDataBaseEntityMessage dataBEMessage = null;

        try {
            String attributeString = QwandaUtils.apiGet(
                    GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/" + groupCode + "/linkcodes/" + linkCode + "/attributes",
                    token);
            dataBEMessage = JsonUtils.fromJson(attributeString, QDataBaseEntityMessage.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataBEMessage;

    }

    public static String apiDelete(final String deleteUrl, final String entityString, final String authToken)
            throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpDeleteWithBody request = new HttpDeleteWithBody(deleteUrl);
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        StringEntity deleteEntity = new StringEntity(entityString, "UTF-8");
        request.setEntity(deleteEntity);
        if (authToken != null) {
            request.addHeader("Authorization", "Bearer " + authToken); // Authorization": `Bearer
        }
        request.setHeader("Content-Type", "application/json; charset=UTF-8");


        request.setHeader("Content-Type", "application/json; charset=UTF-8");

        CloseableHttpResponse response = httpclient.execute(request);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network
        // socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        try {
            HttpEntity entity1 = response.getEntity();
            String responseString = EntityUtils.toString(entity1);

            EntityUtils.consume(entity1);

            return responseString;
        } finally {
            response.close();
            httpclient.close();
            //	IOUtils.closeQuietly(response);
            //	IOUtils.closeQuietly(httpclient);
        }
    }

    public static String apiDelete(final String deleteUrl, final String authToken)
            throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpDeleteWithBody request = new HttpDeleteWithBody(deleteUrl);
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        if (authToken != null) {
            request.addHeader("Authorization", "Bearer " + authToken); // Authorization": `Bearer
        }
        request.setHeader("Content-Type", "application/json; charset=UTF-8");


        CloseableHttpResponse response = httpclient.execute(request);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network
        // socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        try {
            HttpEntity entity1 = response.getEntity();
            String responseString = EntityUtils.toString(entity1);

            EntityUtils.consume(entity1);

            return responseString;
        } finally {
            response.close();
            httpclient.close();
            //IOUtils.closeQuietly(response);
            //IOUtils.closeQuietly(httpclient);
        }
    }

    public static <T extends BaseEntity> T getBaseEntityByCodeWithAttributes(String baseEntAttributeCode, String token) throws IOException {

        String attributeString = null;
        T be = null;
        try {

            attributeString = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/" + baseEntAttributeCode.toUpperCase() + "/attributes", token);
            if (attributeString != null) {
                be = JsonUtils.fromJson(attributeString, BaseEntity.class);
            } else {
                throw new IOException("Cannot find BE " + baseEntAttributeCode);
            }

        } catch (IOException e) {
            throw new IOException("Cannot connect to QwandaURL " + GennySettings.qwandaServiceUrl);
        }


        return be;
    }
}
