package life.genny.qwanda.utils;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusMock;
import life.genny.qwanda.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.MessageData;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.QCmdMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.qwandautils.GennyCacheInterface;
import life.genny.qwanda.qwandautils.GennySettings;
import life.genny.qwanda.qwandautils.JsonUtils;
import life.genny.qwanda.qwandautils.QwandaUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VertxUtils {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    static public boolean cachedEnabled = false;

    static public EventBusInterface eb;

    static final String DEFAULT_TOKEN = "DUMMY";
    static final String[] DEFAULT_FILTER_ARRAY = {"PRI_FIRSTNAME", "PRI_LASTNAME", "PRI_MOBILE", "PRI_IMAGE_URL",
            "PRI_CODE", "PRI_NAME", "PRI_USERNAME"};

    public enum ESubscriptionType {
        DIRECT, TRIGGER;

    }

    static public List<Answer> answerBuffer = new ArrayList<Answer>();

    public static GennyCacheInterface cacheInterface = null;

    public static GennyCacheInterface getCacheInterface() {
        return cacheInterface;
    }

    public static void init(EventBusInterface eventBusInterface, GennyCacheInterface gennyCacheInterface) {
        if (gennyCacheInterface == null) {
            log.error("NULL CACHEINTERFACE SUPPLUED IN INIT");
        }
        eb = eventBusInterface;
        cacheInterface = gennyCacheInterface;
        if (eb instanceof EventBusMock) {
            GennySettings.forceCacheApi = true;
            GennySettings.forceEventBusApi = true;
        }

    }

    static Map<String, String> localCache = new ConcurrentHashMap<String, String>();
    static Map<String, MessageProducer<JsonObject>> localMessageProducerCache = new ConcurrentHashMap<String, MessageProducer<JsonObject>>();

    static public void setRealmFilterArray(final String realm, final String[] filterArray) {
        putStringArray(realm, "FILTER", "PRIVACY", filterArray);
    }

    static public String[] getRealmFilterArray(final String realm) {
        String[] result = getStringArray(realm, "FILTER", "PRIVACY");
        if (result == null) {
            return DEFAULT_FILTER_ARRAY;
        } else {
            return result;
        }
    }

    static public <T> T getObject(final String realm, final String keyPrefix, final String key, final Class clazz) {
        return getObject(realm, keyPrefix, key, clazz, DEFAULT_TOKEN);
    }

    static public <T> T getObject(final String realm, final String keyPrefix, final String key, final Class clazz,
                                  final String token) {
        T item = null;
        String prekey = (StringUtils.isBlank(keyPrefix)) ? "" : (keyPrefix + ":");
        JsonObject json = readCachedJson(realm, prekey + key, token);
        if (json.getString("status").equalsIgnoreCase("ok")) {
            String data = json.getString("value");
            try {
                item = (T) JsonUtils.fromJson(data, clazz);
            } catch (Exception e) {
                log.error("Bad JsonUtils " + realm + ":" + key + ":" + clazz.getTypeName());
            }
            return item;
        } else {
            return null;
        }

    }

    static public <T> T getObject(final String realm, final String keyPrefix, final String key, final Type clazz) {
        return getObject(realm, keyPrefix, key, clazz, DEFAULT_TOKEN);
    }

    static public <T> T getObject(final String realm, final String keyPrefix, final String key, final Type clazz,
                                  final String token) {
        T item = null;
        String prekey = (StringUtils.isBlank(keyPrefix)) ? "" : (keyPrefix + ":");
        JsonObject json = readCachedJson(realm, prekey + key, token);
        if (json.getString("status").equalsIgnoreCase("ok")) {
            String data = json.getString("value");
            try {
                item = (T) JsonUtils.fromJson(data, clazz);
            } catch (Exception e) {
                log.info("Bad JsonUtils " + realm + ":" + key + ":" + clazz.getTypeName());
            }
            return item;
        } else {
            return null;
        }
    }

    static public void putObject(final String realm, final String keyPrefix, final String key, final Object obj) {
        putObject(realm, keyPrefix, key, obj, DEFAULT_TOKEN);
    }

    static public void putObject(final String realm, final String keyPrefix, final String key, final Object obj,
                                 final String token) {
        String data = JsonUtils.toJson(obj);
        String prekey = (StringUtils.isBlank(keyPrefix)) ? "" : (keyPrefix + ":");

        writeCachedJson(realm, prekey + key, data, token);
    }

    static public void clearCache(final String realm, final String token) {
        if (!GennySettings.forceCacheApi) {
            cacheInterface.clear(realm);
        } else {
            if (cachedEnabled) {
                localCache.clear();
            } else {
                try {
                    QwandaUtils.apiGet(GennySettings.ddtUrl + "/clear/" + realm, token);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    static public JsonObject readCachedJson(final String realm, final String key) {
        return readCachedJson(realm, key, DEFAULT_TOKEN);
    }

    static public JsonObject readCachedJson(String realm, final String key, final String token) {
        JsonObject result = null;

        if (!GennySettings.forceCacheApi) {
            String ret = null;
            try {
                // log.info("VERTX READING DIRECTLY FROM CACHE! USING
                // "+(GennySettings.isCacheServer?" LOCAL DDT":"CLIENT "));
                if (key == null) {
                    log.error("Cache is  null");

                    return null;
                }
                ret = (String) cacheInterface.readCache(realm, key, token);
            } catch (Exception e) {
                log.error("Cache is  null");
                e.printStackTrace();
            }
            if (ret != null) {
                result = new JsonObject().put("status", "ok").put("value", ret);
            } else {
                result = new JsonObject().put("status", "error").put("value", ret);
            }
        } else {
            String resultStr = null;
            try {
                //	log.info("VERTX READING FROM CACHE API!");
                if (cachedEnabled) {
                    if ("DUMMY".equals(token)) {
                        // leave realm as it
                    } else {
                        GennyToken temp = new GennyToken(token);
                        realm = temp.getRealm();
                    }

                    resultStr = (String) localCache.get(realm + ":" + key);
                    if ((resultStr != null) && (!"\"null\"".equals(resultStr))) {
                        String resultStr6 = null;
                        if (false) {
                            // ugly way to fix json
                            resultStr6 = VertxUtils.fixJson(resultStr);
                        } else {
                            resultStr6 = resultStr;
                        }
                        // JsonObject rs2 = new JsonObject(resultStr6);
                        // String resultStr2 = resultStr.replaceAll("\\","");
                        // .replace("\\\"","\"");
                        JsonObject resultJson = new JsonObject().put("status", "ok").put("value", resultStr6);
                        resultStr = resultJson.toString();
                    } else {
                        resultStr = null;
                    }

                } else {
                    log.info("DEBUGINFO, DDT URL:" + GennySettings.ddtUrl + ", realm:" + realm + "key:" + key + "token:" + token);
//					resultStr = QwandaUtils.apiGet(GennySettings.ddtUrl + "/service/cache/read/" + realm + "/" + key, token);
                    resultStr = QwandaUtils.apiGet(GennySettings.ddtUrl + "/read/" + realm + "/" + key, token);
                    if (("<html><head><title>Error</title></head><body>Not Found</body></html>".equals(resultStr)) || ("<html><body><h1>Resource not found</h1></body></html>".equals(resultStr))) {
                        resultStr = QwandaUtils.apiGet(GennySettings.ddtUrl + "/service/cache/read/" + key, token);
                    }
//					resultStr =  readFromDDT(realm, key, token).toString();
                }
                if (resultStr != null) {
                    try {
                        result = new JsonObject(resultStr);
                    } catch (Exception e) {
                        log.error("JsonDecode Error " + resultStr);
                    }
                } else {
                    result = new JsonObject().put("status", "error");
                }

            } catch (IOException e) {
                log.error("Could not read " + key + " from cache");
            }

        }

        return result;
    }

    static public JsonObject writeCachedJson(final String realm, final String key, final String value) {
        return writeCachedJson(realm, key, value, DEFAULT_TOKEN);
    }

    static public JsonObject writeCachedJson(final String realm, final String key, final String value,
                                             final String token) {
        return writeCachedJson(realm, key, value, token, 0L);
    }

    static public JsonObject writeCachedJson(String realm, final String key, String value, final String token,
                                             long ttl_seconds) {
        if (!GennySettings.forceCacheApi) {
            cacheInterface.writeCache(realm, key, value, token, ttl_seconds);
        } else {
            try {
                if (cachedEnabled) {
                    // force
                    if ("DUMMY".equals(token)) {

                    } else {
                        GennyToken temp = new GennyToken(token);
                        realm = temp.getRealm();
                    }
                    if (value == null) {
                        localCache.remove(realm + ":" + key);
                    } else {
                        localCache.put(realm + ":" + key, value);
                    }
                } else {

                    log.debug("WRITING TO CACHE USING API! " + key);
                    JsonObject json = new JsonObject();
                    json.put("key", key);
                    json.put("json", value);
                    json.put("ttl", ttl_seconds + "");
                    QwandaUtils.apiPostEntity(GennySettings.ddtUrl + "/write", json.toString(), token);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonObject ok = new JsonObject().put("status", "ok");
        return ok;

    }

    static public void clearDDT(String realm) {

        cacheInterface.clear(realm);
    }


    static public <T extends BaseEntity> T readFromDDT(String realm, final String code, final boolean withAttributes,
                                                       final String token, Class clazz) {
        T be = null;

        JsonObject json = readCachedJson(realm, code, token);

        if ("ok".equals(json.getString("status"))) {
            be = JsonUtils.fromJson(json.getString("value"), clazz);
            if (be != null) {
                if (be.getCode() == null) {
                    log.error("readFromDDT baseEntity for realm " +
                            realm + " has null code! json is [" + json.getString("value") + "]");
                } else {
                    be.setFromCache(true);
                }
            }
        } else {
            // fetch normally
            // log.info("Cache MISS for " + code + " with attributes in realm " +
            // realm);
            if (cachedEnabled) {
                log.debug("Local Cache being used.. this is NOT production");
                // force
                GennyToken temp = new GennyToken(token);
                realm = temp.getRealm();
                String ddtvalue = localCache.get(realm + ":" + code);
                if (ddtvalue == null) {
                    return null;
                } else {
                    be = JsonUtils.fromJson(ddtvalue, BaseEntity.class);
                }
            } else {

                try {
                    if (withAttributes) {
                        be = QwandaUtils.getBaseEntityByCodeWithAttributes(code, token);
                    } else {
                        be = QwandaUtils.getBaseEntityByCode(code, token);
                    }
                } catch (Exception e) {
                    // Okay, this is bad. Usually the code is not in the database but in keycloak
                    // So lets leave it to the rules to sort out... (new user)
                    log.error(
                            "BE " + code + " for realm " + realm + " is NOT IN CACHE OR DB " + e.getLocalizedMessage());
                    return null;

                }

                if (be != null)
                    writeCachedJson(realm, code, JsonUtils.toJson(be));
            }
            if (be != null)
                be.setFromCache(false);
            else
                log.error(String.format("BaseEntity: %s fetched is null", code));
        }
        return be;
    }


    static public <T extends BaseEntity> T readFromDDT(String realm, final String code, final boolean withAttributes,
                                                       final String token) {
        return readFromDDT(realm, code, withAttributes, token, BaseEntity.class);
    }

    static boolean cacheDisabled = GennySettings.noCache;

    static public <T extends BaseEntity> T readFromDDT(final String realm, final String code, final String token) {
        // if ("PER_SHARONCROW66_AT_GMAILCOM".equals(code)) {
        // log.info("DEBUG");
        // }

        return readFromDDT(realm, code, true, token);

    }

    static public void subscribeAdmin(final String realm, final String adminUserCode) {
        final String SUBADMIN = "SUBADMIN";
        // Subscribe to a code
        Set<String> adminSet = getSetString(realm, SUBADMIN, "ADMINS");
        adminSet.add(adminUserCode);
        putSetString(realm, SUBADMIN, "ADMINS", adminSet);
    }

    static public void unsubscribeAdmin(final String realm, final String adminUserCode) {
        final String SUBADMIN = "SUBADMIN";
        // Subscribe to a code
        Set<String> adminSet = getSetString(realm, SUBADMIN, "ADMINS");
        adminSet.remove(adminUserCode);
        putSetString(realm, SUBADMIN, "ADMINS", adminSet);
    }

    static public void subscribe(final String realm, final String subscriptionCode, final String userCode) {
        final String SUB = "SUB";
        // Subscribe to a code
        Set<String> subscriberSet = getSetString(realm, SUB, subscriptionCode);
        subscriberSet.add(userCode);
        putSetString(realm, SUB, subscriptionCode, subscriberSet);
    }

    static public void subscribe(final String realm, final List<BaseEntity> watchList, final String userCode) {
        final String SUB = "SUB";
        // Subscribe to a code
        for (BaseEntity be : watchList) {
            Set<String> subscriberSet = getSetString(realm, SUB, be.getCode());
            subscriberSet.add(userCode);
            putSetString(realm, SUB, be.getCode(), subscriberSet);
        }
    }

    static public void subscribe(final String realm, final BaseEntity be, final String userCode) {
        final String SUB = "SUB";
        // Subscribe to a code
        Set<String> subscriberSet = getSetString(realm, SUB, be.getCode());
        subscriberSet.add(userCode);
        putSetString(realm, SUB, be.getCode(), subscriberSet);

    }

    /*
     * Subscribe list of users to the be
     */
    static public void subscribe(final String realm, final BaseEntity be, final String[] SubscribersCodeArray) {
        final String SUB = "SUB";
        // Subscribe to a code
        // Set<String> subscriberSet = getSetString(realm, SUB, be.getCode());
        // subscriberSet.add(userCode);
        Set<String> subscriberSet = new HashSet<String>(Arrays.asList(SubscribersCodeArray));
        putSetString(realm, SUB, be.getCode(), subscriberSet);

    }

    static public void unsubscribe(final String realm, final String subscriptionCode, final Set<String> userSet) {
        final String SUB = "SUB";
        // Subscribe to a code
        Set<String> subscriberSet = getSetString(realm, SUB, subscriptionCode);
        subscriberSet.removeAll(userSet);

        putSetString(realm, SUB, subscriptionCode, subscriberSet);
    }

    static public String[] getSubscribers(final String realm, final String subscriptionCode) {
        final String SUB = "SUB";
        // Subscribe to a code
        String[] resultArray = getObject(realm, SUB, subscriptionCode, String[].class);

        String[] resultAdmins = getObject(realm, "SUBADMIN", "ADMINS", String[].class);
        String[] result = (String[]) ArrayUtils.addAll(resultArray, resultAdmins);
        return result;

    }

    static public void subscribeEvent(final String realm, final String subscriptionCode, final QEventMessage msg) {
        final String SUBEVT = "SUBEVT";
        // Subscribe to a code
        Set<String> subscriberSet = getSetString(realm, SUBEVT, subscriptionCode);
        subscriberSet.add(JsonUtils.toJson(msg));
        putSetString(realm, SUBEVT, subscriptionCode, subscriberSet);
    }

    static public QEventMessage[] getSubscribedEvents(final String realm, final String subscriptionCode) {
        final String SUBEVT = "SUBEVT";
        // Subscribe to a code
        String[] resultArray = getObject(realm, SUBEVT, subscriptionCode, String[].class);
        QEventMessage[] msgs = new QEventMessage[resultArray.length];
        int i = 0;
        for (String result : resultArray) {
            msgs[i] = JsonUtils.fromJson(result, QEventMessage.class);
            i++;
        }
        return msgs;
    }

    static public Set<String> getSetString(final String realm, final String keyPrefix, final String key) {
        String[] resultArray = getObject(realm, keyPrefix, key, String[].class);
        if (resultArray == null) {
            return new HashSet<String>();
        }
        return Sets.newHashSet(resultArray);
    }

    static public void putSetString(final String realm, final String keyPrefix, final String key, final Set set) {
        String[] strArray = (String[]) FluentIterable.from(set).toArray(String.class);
        putObject(realm, keyPrefix, key, strArray);
    }

    static public void putStringArray(final String realm, final String keyPrefix, final String key,
                                      final String[] string) {
        putObject(realm, keyPrefix, key, string);
    }

    static public String[] getStringArray(final String realm, final String keyPrefix, final String key) {
        String[] resultArray = getObject(realm, keyPrefix, key, String[].class);
        if (resultArray == null) {
            return null;
        }

        return resultArray;
    }

    static public void putMap(final String realm, final String keyPrefix, final String key,
                              final Map<String, String> map) {
        putObject(realm, keyPrefix, key, map);
    }

    static public Map<String, String> getMap(final String realm, final String keyPrefix, final String key) {
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> myMap = getObject(realm, keyPrefix, key, type);
        return myMap;
    }

    public static void putMessageProducer(String sessionState, MessageProducer<JsonObject> toSessionChannel) {
        localMessageProducerCache.put(sessionState, toSessionChannel);

    }

    public static MessageProducer<JsonObject> getMessageProducer(String sessionState) {

        return localMessageProducerCache.get(sessionState);

    }

    static public void publish(BaseEntity user, String channel, Object payload) {

        publish(user, channel, payload, DEFAULT_FILTER_ARRAY);
    }

    static public JsonObject publish(BaseEntity user, String channel, Object payload, final String[] filterAttributes) {

        eb.publish(user, channel, payload, filterAttributes);

        JsonObject ok = new JsonObject().put("status", "ok");
        return ok;

    }

    static public void publish(BaseEntity user, String channel, BaseEntity be, String aliasCode, String token) {

        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be, aliasCode);
        msg.setToken(token);
        eb.publish(user, channel, msg, null);
    }

    static public JsonObject writeMsg(String channel, Object payload) {
        JsonObject result = null;

        if ((payload instanceof String) || (payload == null)) {
            if ("null".equals((String) payload)) {
                return new JsonObject().put("status", "error");
            }
        }

        if ("webdata".equals(channel) || "webcmds".equals(channel) || "events".equals(channel) || "data".equals(channel)) {
            // This is a standard session only

        } else {
            // This looks like we are sending data to a subscription channel

            if (payload instanceof String) {
                JsonObject msg = (JsonObject) new JsonObject((String) payload);
                log.info(msg.getValue("event_type"));
                JsonArray jsonArray = msg.getJsonArray("recipientCodeArray");
                Set<String> rxList = new HashSet<String>();

                jsonArray.forEach(object -> {
                    if (object instanceof JsonObject) {
                        JsonObject jsonObject = (JsonObject) object;
                        rxList.add(jsonObject.toString());
                    } else if (object instanceof String) {
                        rxList.add(object.toString());
                    }

                });

                rxList.add(channel);
                JsonArray finalArray = new JsonArray();

                for (String ch : rxList) {
                    finalArray.add(ch);
                }

                msg.put("recipientCodeArray", finalArray);
                payload = msg.toString();
                channel = "webdata";

            } else if (payload instanceof QDataBaseEntityMessage) {
                QDataBaseEntityMessage msg = (QDataBaseEntityMessage) payload;
                Set<String> rxList = new HashSet<String>();
                rxList.add(channel);
                String[] rx = msg.getRecipientCodeArray();
                if (rx != null) {
                    Set<String> rx2 = Arrays.stream(rx).collect(Collectors.toSet());
                    rxList.addAll(rx2);
                }
                rx = rxList.toArray(new String[0]);
                msg.setRecipientCodeArray(rx);
                channel = "webdata";
            } else if (payload instanceof QBulkMessage) {
                QBulkMessage msg = (QBulkMessage) payload;
                Set<String> rxList = new HashSet<String>();
                rxList.add(channel);
                String[] rx = msg.getRecipientCodeArray();
                if (rx != null) {
                    Set<String> rx2 = Arrays.stream(rx).collect(Collectors.toSet());
                    rxList.addAll(rx2);
                }
                rx = rxList.toArray(new String[0]);
                msg.setRecipientCodeArray(rx);
                channel = "webdata";
            } else if (payload instanceof JsonObject) {
                JsonObject msg = (JsonObject) payload;
                log.info(msg.getValue("code"));
                JsonArray jsonArray = msg.getJsonArray("recipientCodeArray");
                Set<String> rxList = new HashSet<String>();

                jsonArray.forEach(object -> {
                    if (object instanceof JsonObject) {
                        JsonObject jsonObject = (JsonObject) object;
                        rxList.add(jsonObject.toString());
                    } else if (object instanceof String) {
                        rxList.add(object.toString());
                    }

                });

                rxList.add(channel);
                JsonArray finalArray = new JsonArray();

                for (String ch : rxList) {
                    finalArray.add(ch);
                }

                msg.put("recipientCodeArray", finalArray);
                payload = msg;
                channel = "webdata";

            }

        }

        eb.writeMsg(channel, payload);

        result = new JsonObject().put("status", "ok");
        return result;

    }


    static public JsonObject writeMsg(String channel, BaseEntity baseentity, String aliasCode) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(baseentity, aliasCode);
        return writeMsg(channel, msg);
    }


    static public void writeMsgEnd(GennyToken userToken) {
        QCmdMessage msgend = new QCmdMessage("END_PROCESS", "END_PROCESS");
        msgend.setToken(userToken.getToken());
        msgend.setSend(true);
        VertxUtils.writeMsg("webcmds", msgend);
    }


    static public QEventMessage sendEvent(final String code) {
        QEventMessage msg = new QEventMessage("UPDATE", code);
        writeMsg("events", msg);
        return msg;
    }


    static public QEventMessage sendEvent(final String code, final String source, final String target, final GennyToken serviceToken) {
        QEventMessage msg = new QEventMessage("UPDATE", code);
        MessageData data = new MessageData(code);
        data.setParentCode(source);
        data.setTargetCode(target);
        msg.setData(data);
        msg.setToken(serviceToken.getToken());
        writeMsg("events", msg);
        return msg;
    }

    static public Object privacyFilter(BaseEntity user, Object payload, final String[] filterAttributes) {
        if (payload instanceof QDataBaseEntityMessage) {
            return JsonUtils.toJson(privacyFilter(user, (QDataBaseEntityMessage) payload,
                    new HashMap<String, BaseEntity>(), filterAttributes));
        } else if (payload instanceof QBulkMessage) {
            return JsonUtils.toJson(privacyFilter(user, (QBulkMessage) payload, filterAttributes));
        } else
            return payload;
    }

    static public QDataBaseEntityMessage privacyFilter(BaseEntity user, QDataBaseEntityMessage msg,
                                                       Map<String, BaseEntity> uniquePeople, final String[] filterAttributes) {
        ArrayList<BaseEntity> bes = new ArrayList<BaseEntity>();
        for (BaseEntity be : msg.getItems()) {
            if (uniquePeople != null && be.getCode() != null && !uniquePeople.containsKey(be.getCode())) {

                be = privacyFilter(user, be, filterAttributes);
                uniquePeople.put(be.getCode(), be);
                bes.add(be);
            } else {
                /*
                 * Avoid sending the attributes again for the same BaseEntity, so sending
                 * without attributes
                 */
                BaseEntity slimBaseEntity = new BaseEntity(be.getCode(), be.getName());
                /*
                 * Setting the links again but Adam don't want it to be send as it increasing
                 * the size of BE. Frontend should create links based on the parentCode of
                 * baseEntity not the links. This requires work in the frontend. But currently
                 * the GRP_NEW_ITEMS are being sent without any links so it doesn't show any
                 * internships.
                 */
                slimBaseEntity.setLinks(be.getLinks());
                bes.add(slimBaseEntity);
            }
        }
        msg.setItems(bes.toArray(new BaseEntity[bes.size()]));
        return msg;
    }

    static public QBulkMessage privacyFilter(BaseEntity user, QBulkMessage msg, final String[] filterAttributes) {
        Map<String, BaseEntity> uniqueBes = new HashMap<String, BaseEntity>();
        for (QDataBaseEntityMessage beMsg : msg.getMessages()) {
            beMsg = privacyFilter(user, beMsg, uniqueBes, filterAttributes);
        }
        return msg;
    }

    static public BaseEntity privacyFilter(BaseEntity user, BaseEntity be) {
        final String[] filterStrArray = {"PRI_FIRSTNAME", "PRI_LASTNAME", "PRI_MOBILE", "PRI_EMAIL", "PRI_PHONE",
                "PRI_IMAGE_URL", "PRI_CODE", "PRI_NAME", "PRI_USERNAME"};

        return privacyFilter(user, be, filterStrArray);
    }

    static public <T extends BaseEntity> T privacyFilter(BaseEntity user, T be, final String[] filterAttributes) {
        Set<EntityAttribute> allowedAttributes = new HashSet<EntityAttribute>();
        for (EntityAttribute entityAttribute : be.getBaseEntityAttributes()) {
            // log.info("ATTRIBUTE:"+entityAttribute.getAttributeCode()+(entityAttribute.getPrivacyFlag()?"PRIVACYFLAG=TRUE":"PRIVACYFLAG=FALSE"));
            if ((be.getCode().startsWith("PER_")) && (!be.getCode().equals(user.getCode()))) {
                String attributeCode = entityAttribute.getAttributeCode();

                if (Arrays.stream(filterAttributes).anyMatch(x -> x.equals(attributeCode))) {

                    allowedAttributes.add(entityAttribute);
                } else {
                    if (attributeCode.startsWith("PRI_IS_")) {
                        allowedAttributes.add(entityAttribute);// allow all roles
                    }
                    if (attributeCode.startsWith("LNK_")) {
                        allowedAttributes.add(entityAttribute);// allow attributes that starts with "LNK_"
                    }
                }
            } else {
                if (!entityAttribute.getPrivacyFlag()) { // don't allow privacy flag attributes to get through
                    allowedAttributes.add(entityAttribute);
                }
            }
            if (entityAttribute.getAttributeCode().equals("PRI_INTERVIEW_URL")) {
                log.info("My Interview");
            }
        }
        be.setBaseEntityAttributes(allowedAttributes);

        return be;
    }

    static public BaseEntity privacyFilter(BaseEntity be, final String[] filterAttributes) {
        Set<EntityAttribute> allowedAttributes = new HashSet<EntityAttribute>();
        for (EntityAttribute entityAttribute : be.getBaseEntityAttributes()) {
            String attributeCode = entityAttribute.getAttributeCode();
            if (Arrays.stream(filterAttributes).anyMatch(x -> x.equals(attributeCode))) {
                allowedAttributes.add(entityAttribute);
            }
        }
        be.setBaseEntityAttributes(allowedAttributes);

        return be;
    }

    public static Boolean checkIfAttributeValueContainsString(BaseEntity baseentity, String attributeCode,
                                                              String checkIfPresentStr) {

        Boolean isContainsValue = false;

        if (baseentity != null && attributeCode != null && checkIfPresentStr != null) {
            String attributeValue = baseentity.getValue(attributeCode, null);

            if (attributeValue != null && attributeValue.toLowerCase().contains(checkIfPresentStr.toLowerCase())) {
                return true;
            }
        }

        return isContainsValue;
    }

    public static String apiPostEntity(final String postUrl, final String entityString, final String authToken,
                                       final Consumer<String> callback) throws IOException {
        {
            String responseString = "ok";

            return responseString;
        }
    }

    public static String apiPostEntity(final String postUrl, final String entityString, final String authToken)
            throws IOException {
        {
            return apiPostEntity(postUrl, entityString, authToken, null);
        }
    }

    public static Set<String> fetchRealmsFromApi() {
        List<String> activeRealms = new ArrayList<String>();
        JsonObject ar = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, "REALMS");
        String ars = ar.getString("value");

        if (ars == null) {
            try {
                ars = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/utils/realms", "NOTREQUIRED");
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Type listType = new TypeToken<List<String>>() {
        }.getType();
        ars = ars.replaceAll("\\\"", "\"");
        activeRealms = JsonUtils.fromJson(ars, listType);
        Set<String> realms = new HashSet<>(activeRealms);
        return realms;
    }

    static public String fixJson(String resultStr) {
        String resultStr2 = resultStr.replaceAll(Pattern.quote("\\\""),
                Matcher.quoteReplacement("\""));
        String resultStr3 = resultStr2.replaceAll(Pattern.quote("\\n"),
                Matcher.quoteReplacement("\n"));
        String resultStr4 = resultStr3.replaceAll(Pattern.quote("\\\n"),
                Matcher.quoteReplacement("\n"));
//		String resultStr5 = resultStr4.replaceAll(Pattern.quote("\"{"),
//				Matcher.quoteReplacement("{"));
//		String resultStr6 = resultStr5.replaceAll(Pattern.quote("\"["),
//				Matcher.quoteReplacement("["));
//		String resultStr7 = resultStr6.replaceAll(Pattern.quote("]\""),
//				Matcher.quoteReplacement("]"));
//		String resultStr8 = resultStr5.replaceAll(Pattern.quote("}\""), Matcher.quoteReplacement("}"));
        String ret = resultStr4.replaceAll(Pattern.quote("\\\""
                        + ""),
                Matcher.quoteReplacement("\""));
        return ret;

    }

}


