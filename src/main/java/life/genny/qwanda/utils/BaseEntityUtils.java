package life.genny.qwanda.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.Answer;
import life.genny.qwanda.GennyToken;
import life.genny.qwanda.Layout;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBulkPullMessage;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwanda.qwandautils.GennySettings;
import life.genny.qwanda.qwandautils.JsonUtils;
import life.genny.qwanda.qwandautils.QwandaUtils;
import life.genny.qwanda.utils.Layout.LayoutUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BaseEntityUtils implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private Map<String, Object> decodedMapToken;
    private String token;
    private String realm;
    private String qwandaServiceUrl;

    private CacheUtils cacheUtil;
    private GennyToken gennyToken;
    private GennyToken serviceToken;

    public BaseEntityUtils(GennyToken userToken) {
        this(GennySettings.qwandaServiceUrl, userToken);
    }

    public BaseEntityUtils(GennyToken serviceToken, GennyToken userToken) {
        this(GennySettings.qwandaServiceUrl, userToken);
        this.serviceToken = serviceToken;
    }

    public BaseEntityUtils(String qwandaServiceUrl, GennyToken gennyToken) {

        this.decodedMapToken = gennyToken.getAdecodedTokenMap();
        this.qwandaServiceUrl = qwandaServiceUrl;
        this.token = gennyToken.getToken();
        this.realm = gennyToken.getRealm();
        this.gennyToken = gennyToken;
        this.serviceToken = gennyToken; // override afterwards
        this.cacheUtil = new CacheUtils(qwandaServiceUrl, token, decodedMapToken, realm);
        this.cacheUtil.setBaseEntityUtils(this);
    }

    public BaseEntityUtils(String qwandaServiceUrl, String token, Map<String, Object> decodedMapToken, String realm) {
        this(qwandaServiceUrl, new GennyToken(token));
    }

    private String getRealm() {
        return gennyToken.getRealm();
    }

    /* =============== refactoring =============== */

    public BaseEntity create(final String uniqueCode, final String bePrefix, final String name) {

        String uniqueId = QwandaUtils.getUniqueId(bePrefix, uniqueCode);
        if (uniqueId != null) {
            return this.create(uniqueId, name);
        }

        return null;
    }

    public BaseEntity create(String baseEntityCode, String name) {

        BaseEntity newBaseEntity = null;
        if (!VertxUtils.cachedEnabled) {
            newBaseEntity = QwandaUtils.createBaseEntityByCode(baseEntityCode, name, qwandaServiceUrl, this.token);
        } else {
            GennyToken gt = new GennyToken(this.token);
            newBaseEntity = new BaseEntity(baseEntityCode, name);
            newBaseEntity.setRealm(gt.getRealm());
        }
        this.addAttributes(newBaseEntity);
        VertxUtils.writeCachedJson(newBaseEntity.getRealm(), newBaseEntity.getCode(), JsonUtils.toJson(newBaseEntity),
                this.token);
        return newBaseEntity;
    }

    public List<BaseEntity> getBaseEntityFromSelectionAttribute(BaseEntity be, String attributeCode) {

        List<BaseEntity> bes = new CopyOnWriteArrayList<>();

        String attributeValue = be.getValue(attributeCode, null);
        if (attributeValue != null) {
            if (!attributeValue.isEmpty() && !attributeValue.equals(" ")) {
                /*
                 * first we try to serialise the attriute into a JsonArray in case this is a
                 * multi-selection attribute
                 */
                try {

                    // JsonArray attributeValues = new JsonArray(attributeValue);

                    JsonParser parser = new JsonParser();
                    JsonElement tradeElement = parser.parse(attributeValue);
                    JsonArray attributeValues = tradeElement.getAsJsonArray();

                    /* we loop through the attribute values */
                    for (int i = 0; i < attributeValues.size(); i++) {

                        String beCode = attributeValues.get(i).getAsString();

                        /* we try to fetch the base entity */
                        if (beCode != null) {

                            BaseEntity baseEntity = this.getBaseEntityByCode(beCode);
                            if (baseEntity != null) {

                                /* we add the base entity to the list */
                                bes.add(baseEntity);
                            }
                        }
                    }

                    /* we return the list */
                    return bes;

                } catch (Exception e) {
                    /*
                     * serialisation did not work - we can assume this is a single selection
                     * attribute
                     */

                    /* we fetch the BaseEntity */
                    BaseEntity baseEntity = this.getBaseEntityByCode(attributeValue);

                    /* we return a list containing only this base entity */
                    if (baseEntity != null) {
                        bes.add(baseEntity);
                    }

                    /* we return */
                    return bes;
                }
            }
        }

        return bes;
    }

    /* ================================ */
    /* old code */

    public BaseEntity createRole(final String uniqueCode, final String name, String... capabilityCodes) {
        String code = "ROL_IS_" + uniqueCode.toUpperCase();
        log.info("Creating Role " + code + ":" + name);
        BaseEntity role = this.getBaseEntityByCode(code);
        if (role == null) {
            role = QwandaUtils.createBaseEntityByCode(code, name, qwandaServiceUrl, this.token);
            this.addAttributes(role);

            VertxUtils.writeCachedJson(role.getRealm(), role.getCode(), JsonUtils.toJson(role));
        }

        for (String capabilityCode : capabilityCodes) {
            Attribute capabilityAttribute = RulesUtils.attributeMap.get("CAP_" + capabilityCode);
            try {
                role.addAttribute(capabilityAttribute, 1.0, "TRUE");
            } catch (BadDataException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Now force the role to only have these capabilitys
        try {
            String result = QwandaUtils.apiPutEntity(qwandaServiceUrl + "/qwanda/baseentitys/force", JsonUtils.toJson(role),
                    this.token);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return role;
    }

    public Object get(final String key) {
        return this.decodedMapToken.get(key);
    }

    public void set(final String key, Object value) {
        this.decodedMapToken.put(key, value);
    }

    public Attribute saveAttribute(Attribute attribute, final String token) throws IOException {

        RulesUtils.attributeMap.put(attribute.getCode(), attribute);
        try {
            if (!VertxUtils.cachedEnabled) { // only post if not in junit

                String result = QwandaUtils.apiPostEntity(this.qwandaServiceUrl + "/qwanda/attributes",
                        JsonUtils.toJson(attribute), token);
            }
            return attribute;
        } catch (IOException e) {
            log.error("Socket error trying to post attribute");
            throw new IOException("Cannot save attribute");
        }

    }

    public void addAttributes(BaseEntity be) {

        if (be != null) {

            if (!(be.getCode().startsWith("SBE_") || be.getCode().startsWith("RAW_"))) { // don't bother with search be
                // or raw attributes
                for (EntityAttribute ea : be.getBaseEntityAttributes()) {
                    if (ea != null) {
                        Attribute attribute = RulesUtils.attributeMap.get(ea.getAttributeCode());
                        if (attribute != null) {
                            ea.setAttribute(attribute);
//							if (ea.getAttributeCode().equals("PRI_MIV")) {
//								log.info("Found PRI_MIV. Processing the attribute");
//								processVideoAttribute(ea);
//							}
                        } else {
                            RulesUtils.loadAllAttributesIntoCache(this.token);
                            attribute = RulesUtils.attributeMap.get(ea.getAttributeCode());
                            if (attribute != null) {
                                ea.setAttribute(attribute);
                            } else {
                                log.error("Cannot get Attribute - " + ea.getAttributeCode());

                                Attribute dummy = new AttributeText(ea.getAttributeCode(), ea.getAttributeCode());
                                ea.setAttribute(dummy);

                            }
                        }
                    }
                }
            }
        }
    }

    public BaseEntity saveAnswer(Answer answer) {

        BaseEntity ret = addAnswer(answer);
        try {
            if (!VertxUtils.cachedEnabled) { // only post if not in junit
                QwandaUtils.apiPostEntity(qwandaServiceUrl + "/qwanda/answers", JsonUtils.toJson(answer), this.token);
            }
            // Now update the Cache

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public BaseEntity addAnswer(Answer answer) {

        return this.updateCachedBaseEntity(answer);
    }

    public <T extends BaseEntity> T updateBaseEntity(T be, Answer answer, Class clazz) {

        T be2 = this.updateCachedBaseEntity(answer, clazz);
        try {
            Attribute attr = RulesUtils.getAttribute(answer.getAttributeCode(), this.getGennyToken().getToken());
            be.setValue(attr, answer.getValue());
        } catch (BadDataException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return be2;
    }

    public BaseEntity updateBaseEntity(String baseEntityCode, Answer answer) {
        BaseEntity be = this.getBaseEntityByCode(baseEntityCode);
        return updateBaseEntity(be, answer, BaseEntity.class);
    }

    public <T extends BaseEntity> T updateBaseEntity(T be, Answer answer) {

        return updateBaseEntity(be, answer, BaseEntity.class);
    }

    public void saveAnswers(List<Answer> answers, final boolean changeEvent) {
        if (!((answers == null) || (answers.isEmpty()))) {

            if (!changeEvent) {
                for (Answer answer : answers) {
                    answer.setChangeEvent(false);
                }
            }

            // Sort answers into target Baseentitys
            Map<String, List<Answer>> answersPerTargetCodeMap = answers.stream()
                    .collect(Collectors.groupingBy(Answer::getTargetCode));

            for (String targetCode : answersPerTargetCodeMap.keySet()) {
                List<Answer> targetAnswers = answersPerTargetCodeMap.get(targetCode);
                Answer items[] = new Answer[targetAnswers.size()];
                items = targetAnswers.toArray(items);

                QDataAnswerMessage msg = new QDataAnswerMessage(items);
                this.updateCachedBaseEntity(targetAnswers);

                if (!VertxUtils.cachedEnabled) { // if not running junit, no need for api
                    String jsonAnswer = JsonUtils.toJson(msg);
                    // jsonAnswer.replace("\\\"", "\"");

                    try {
                        if (!VertxUtils.cachedEnabled) { // only post if not in junit
                            QwandaUtils.apiPostEntity(this.qwandaServiceUrl + "/qwanda/answers/bulk", jsonAnswer, token);
                        }
                    } catch (IOException e) {
                        log.error("Socket error trying to post answer");
                    }
                } else {
                    for (Answer answer : answers) {
                        log.info("Saving Answer :" + answer);
                    }
                }
            }
        }
    }

    public void saveAnswers(List<Answer> answers) {
        this.saveAnswers(answers, true);
    }

    public BaseEntity getOfferBaseEntity(String groupCode, String linkCode, String linkValue, String quoterCode,
                                         String prefix, String attributeCode) {
        return this.getOfferBaseEntity(groupCode, linkCode, linkValue, quoterCode, true, prefix, attributeCode);
    }

    public BaseEntity getOfferBaseEntity(String groupCode, String linkCode, String linkValue, String quoterCode,
                                         Boolean includeHidden, String prefix, String attributeCode) {

        /*
         * TODO : Replace with searchEntity when it will be capable of filtering based
         * on linkWeight
         */
        List linkList = this.getLinkList(groupCode, linkCode, linkValue, this.token);
        String quoterCodeForOffer = null;

        if (linkList != null) {

            try {
                for (Object linkObj : linkList) {

                    Link link = JsonUtils.fromJson(linkObj.toString(), Link.class);
                    BaseEntity offerBe = null;
                    if (!includeHidden) {
                        if (link.getWeight() != 0) {
                            offerBe = this.getBaseEntityByCode(link.getTargetCode());
                        }
                    } else {
                        offerBe = this.getBaseEntityByCode(link.getTargetCode());
                    }

                    // BaseEntity offerBe = this.getBaseEntityByCode(link.getTargetCode());

                    if (offerBe != null && offerBe.getCode().startsWith(prefix)) {

                        quoterCodeForOffer = offerBe.getValue(attributeCode, null);

                        if (quoterCode.equals(quoterCodeForOffer)) {
                            return offerBe;
                        }
                    }
                }
            } catch (Exception e) {

            }
        }

        return null;
    }

    public void updateBaseEntityAttribute(final String sourceCode, final String beCode, final String attributeCode,
                                          final String newValue) {
        List<Answer> answers = new CopyOnWriteArrayList<Answer>();
        answers.add(new Answer(sourceCode, beCode, attributeCode, newValue));
        this.saveAnswers(answers);
    }

    public SearchEntity getSearchEntityByCode(final String code) {

        SearchEntity searchBe = null;
        try {
            JsonObject cachedJsonObject = VertxUtils.readCachedJson(this.realm, code, this.token);
            if (cachedJsonObject != null) {
                String data = cachedJsonObject.getString("value");
                log.info("json recieved :" + data);

                if (data != null) {
                    searchBe = JsonUtils.fromJson(data, SearchEntity.class);
                    log.info("searchEntity :: " + JsonUtils.toJson(searchBe));
                }
            }

        } catch (Exception e) {
            log.error("Failed to read cache for search " + code);
        }
        return searchBe;
    }

    public <T extends BaseEntity> T getBaseEntityByCode(final String code) {
        return this.getBaseEntityByCode(code, true);
    }

    public Optional<BaseEntity> getOptionalBaseEntityByCode(final String code) {
        return this.getOptionalBaseEntityByCode(code, true);
    }

    public <T extends BaseEntity> T getBaseEntityByCode(final String code, Boolean withAttributes, Class clazz) {

        return BaseEntityByCode(code, withAttributes, clazz, null);
    }

    public <T extends BaseEntity> T getFilteredBaseEntityByCode(final String code, final String questionCode) {
        String[] filteredStrings = null;
        String askMsgs2Str = (String) VertxUtils.cacheInterface.readCache(this.getRealm(), "FRM_" + code + "_ASKS",
                this.getGennyToken().getToken());

        if (askMsgs2Str != null) {
            // extract the 'PRI_ and LNKs
            Set<String> allMatches = new HashSet<String>();
            Matcher m = Pattern.compile("(PRI_\\[A-Z0-9\\_])").matcher(askMsgs2Str);
            while (m.find()) {
                allMatches.add(m.group());
            }

            filteredStrings = allMatches.toArray(new String[0]);
        }

        // ugly TODO HAC

        T be = BaseEntityByCode(code, true, BaseEntity.class, filteredStrings);

        for (EntityAttribute ea : be.getBaseEntityAttributes()) {
            if (ea.getAttributeCode().equals("PRI_VIDEO_URL")) {
                String value = ea.getValueString();
                if (value.startsWith("http")) {
                    // normal video
                } else {
                    log.info("My Interview");

                    BaseEntity project = this.getBaseEntityByCode("PRJ_" + this.getGennyToken().getRealm().toUpperCase());
                    String apiKey = project.getValueAsString("ENV_API_KEY_MY_INTERVIEW");
                    String secretToken = project.getValueAsString("ENV_SECRET_MY_INTERVIEW");
                    long unixTimestamp = Instant.now().getEpochSecond();
                    String apiSecret = apiKey + secretToken + unixTimestamp;
                    String hashed = BCrypt.hashpw(apiSecret, BCrypt.gensalt(10));
                    String videoId = ea.getValueString();
                    String url = "https://api.myinterview.com/2.21.2/getVideo?apiKey=" + apiKey + "&hashTimestamp="
                            + unixTimestamp + "&hash=" + hashed + "&video=" + videoId;
                    String url2 = "https://embed.myinterview.com/player.v3.html?apiKey=" + apiKey + "&hashTimestamp="
                            + unixTimestamp + "&hash=" + hashed + "&video=" + videoId + "&autoplay=1&fs=0";

                    log.info("MyInterview Hash is " + url);
                    log.info("MyInterview Hash2 is " + url2);
                    if ("PER_DOMENIC_AT_GADATECHNOLOGY_DOT_COM_DOT_AU".equals(code)) {
                        ea.setValue("https://www.youtube.com/watch?v=dQw4w9WgXcQ"); // needed a demo video
                    } else {
                        ea.setValue(url2);
                    }
                }
            }

        }

        return be;

    }

    public <T extends BaseEntity> T BaseEntityByCode(final String code, Boolean withAttributes, Class clazz,
                                                     final String[] filterAttributes) {

        T be = null;

        if (code == null) {
            log.error("Cannot pass a null code");
            return null;
        }

        try {
            // log.info("Fetching BaseEntityByCode, code="+code);
            be = VertxUtils.readFromDDT(getRealm(), code, withAttributes, this.token, clazz);
            if (be == null) {
                if (!VertxUtils.cachedEnabled) { // because during junit it annoys me
                    log.info("be (" + code + ") fetched is NULL ");
                }
            } else {
                this.addAttributes(be);
            }
        } catch (Exception e) {
            log.info("Failed to read cache for baseentity " + code);
        }

        if (filterAttributes != null) {
            BaseEntity user = this.getBaseEntityByCode(this.gennyToken.getUserCode());
            be = VertxUtils.privacyFilter(user, be, filterAttributes);
        }

        return be;
    }

    public <T extends BaseEntity> T getBaseEntityByCode(final String code, Boolean withAttributes) {

        return getBaseEntityByCode(code, withAttributes, BaseEntity.class);
    }

    public Optional<BaseEntity> getOptionalBaseEntityByCode(final String code, Boolean withAttributes) {

        Optional<BaseEntity> be = Optional.empty();

        try {
            // log.info("Fetching BaseEntityByCode, code="+code);
            be = Optional.ofNullable(VertxUtils.readFromDDT(getRealm(), code, withAttributes, this.token));
            if (be.isPresent()) {
                this.addAttributes(be.orElse(null));
            }
        } catch (Exception e) {
            log.info("Failed to read cache for baseentity " + code);
        }

        return be;
    }

    public BaseEntity getBaseEntityByAttributeAndValue(final String attributeCode, final String value) {

        BaseEntity be = null;
        be = RulesUtils.getBaseEntityByAttributeAndValue(this.qwandaServiceUrl, this.decodedMapToken, this.token,
                attributeCode, value);
        if (be != null) {
            this.addAttributes(be);
        }
        return be;
    }

    public List<BaseEntity> getBaseEntitysByAttributeAndValue(final String attributeCode, final String value) {

        List<BaseEntity> bes = null;
        bes = RulesUtils.getBaseEntitysByAttributeAndValue(this.qwandaServiceUrl, this.decodedMapToken, this.token,
                attributeCode, value);
        return bes;
    }

    public void clearBaseEntitysByParentAndLinkCode(final String parentCode, final String linkCode, Integer pageStart,
                                                    Integer pageSize) {

        String key = parentCode + linkCode + "-" + pageStart + "-" + pageSize;
        VertxUtils.putObject(this.realm, "LIST", key, null);
    }

    public List<BaseEntity> getBaseEntitysByParentAndLinkCode(final String parentCode, final String linkCode,
                                                              Integer pageStart, Integer pageSize, Boolean cache) {
        cache = false;
        List<BaseEntity> bes = new CopyOnWriteArrayList<BaseEntity>();
        String key = parentCode + linkCode + "-" + pageStart + "-" + pageSize;
        if (cache) {
            Type listType = new TypeToken<List<BaseEntity>>() {
            }.getType();
            List<String> beCodes = VertxUtils.getObject(this.realm, "LIST", key, (Class) listType);
            if (beCodes == null) {
                bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributes(qwandaServiceUrl, this.decodedMapToken,
                        this.token, parentCode, linkCode, pageStart, pageSize);
                beCodes = new CopyOnWriteArrayList<String>();
                for (BaseEntity be : bes) {
                    VertxUtils.putObject(this.realm, "", be.getCode(), JsonUtils.toJson(be));
                    beCodes.add(be.getCode());
                }
                VertxUtils.putObject(this.realm, "LIST", key, beCodes);
            } else {
                for (String beCode : beCodes) {
                    BaseEntity be = getBaseEntityByCode(beCode);
                    bes.add(be);
                }
            }
        } else {

            bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributes(qwandaServiceUrl, this.decodedMapToken,
                    this.token, parentCode, linkCode, pageStart, pageSize);
        }

        return bes;
    }

    /* added because of the bug */
    public List<BaseEntity> getBaseEntitysByParentAndLinkCode2(final String parentCode, final String linkCode,
                                                               Integer pageStart, Integer pageSize, Boolean cache) {

        List<BaseEntity> bes = null;

        // if (isNull("BES_" + parentCode.toUpperCase() + "_" + linkCode)) {

        bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributes2(qwandaServiceUrl, this.decodedMapToken,
                this.token, parentCode, linkCode, pageStart, pageSize);

        // } else {
        // bes = getAsBaseEntitys("BES_" + parentCode.toUpperCase() + "_" + linkCode);
        // }

        return bes;
    }

    // Adam's speedup
    public List<BaseEntity> getBaseEntitysByParentAndLinkCode3(final String parentCode, final String linkCode,
                                                               Integer pageStart, Integer pageSize, Boolean cache) {
        cache = false;
        List<BaseEntity> bes = new CopyOnWriteArrayList<BaseEntity>();

        BaseEntity parent = getBaseEntityByCode(parentCode);
        for (EntityEntity ee : parent.getLinks()) {
            if (ee.getLink().getAttributeCode().equalsIgnoreCase(linkCode)) {
                BaseEntity child = getBaseEntityByCode(ee.getLink().getTargetCode());

                bes.add(child);
            }
        }
        return bes;
    }

    public List<BaseEntity> getBaseEntitysByParentLinkCodeAndLinkValue(final String parentCode, final String linkCode,
                                                                       final String linkValue, Integer pageStart, Integer pageSize, Boolean cache) {

        List<BaseEntity> bes = null;

        bes = RulesUtils.getBaseEntitysByParentAndLinkCodeAndLinkValueWithAttributes(qwandaServiceUrl, this.decodedMapToken,
                this.token, parentCode, linkCode, linkValue, pageStart, pageSize);
        return bes;
    }

    public List<BaseEntity> getBaseEntitysByParentAndLinkCode(final String parentCode, final String linkCode,
                                                              Integer pageStart, Integer pageSize, Boolean cache, final String stakeholderCode) {
        List<BaseEntity> bes = null;

        bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributesAndStakeholderCode(qwandaServiceUrl,
                this.decodedMapToken, this.token, parentCode, linkCode, stakeholderCode);
        if (cache) {
            set("BES_" + parentCode.toUpperCase() + "_" + linkCode, bes); // WATCH THIS!!!
        }

        return bes;
    }

    public String moveBaseEntity(String baseEntityCode, String sourceCode, String targetCode) {
        return this.moveBaseEntity(baseEntityCode, sourceCode, targetCode, "LNK_CORE", "LINK");
    }

    public String moveBaseEntity(String baseEntityCode, String sourceCode, String targetCode, String linkCode) {
        return this.moveBaseEntity(baseEntityCode, sourceCode, targetCode, linkCode, "LINK");
    }

    public String moveBaseEntity(String baseEntityCode, String sourceCode, String targetCode, String linkCode,
                                 final String linkValue) {

        Link link = new Link(sourceCode, baseEntityCode, linkCode, linkValue);

        try {

            /* we call the api */
            QwandaUtils.apiPostEntity(qwandaServiceUrl + "/qwanda/baseentitys/move/" + targetCode, JsonUtils.toJson(link),
                    this.token);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public Object getBaseEntityValue(final String baseEntityCode, final String attributeCode) {
        BaseEntity be = getBaseEntityByCode(baseEntityCode);
        Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
        if (ea.isPresent()) {
            return ea.get().getObject();
        } else {
            return null;
        }
    }

    public static String getBaseEntityAttrValueAsString(BaseEntity be, String attributeCode) {

        String attributeVal = null;
        for (EntityAttribute ea : be.getBaseEntityAttributes()) {
            try {
                if (ea.getAttributeCode().equals(attributeCode)) {
                    attributeVal = ea.getObjectAsString();
                }
            } catch (Exception e) {
            }
        }

        return attributeVal;
    }

    public String getBaseEntityValueAsString(final String baseEntityCode, final String attributeCode) {

        String attrValue = null;

        if (baseEntityCode != null) {

            BaseEntity be = getBaseEntityByCode(baseEntityCode);
            attrValue = getBaseEntityAttrValueAsString(be, attributeCode);
        }

        return attrValue;
    }

    public LocalDateTime getBaseEntityValueAsLocalDateTime(final String baseEntityCode, final String attributeCode) {
        BaseEntity be = getBaseEntityByCode(baseEntityCode);
        Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
        if (ea.isPresent()) {
            return ea.get().getValueDateTime();
        } else {
            return null;
        }
    }

    public LocalDate getBaseEntityValueAsLocalDate(final String baseEntityCode, final String attributeCode) {
        BaseEntity be = getBaseEntityByCode(baseEntityCode);
        Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
        if (ea.isPresent()) {
            return ea.get().getValueDate();
        } else {
            return null;
        }
    }

    public LocalTime getBaseEntityValueAsLocalTime(final String baseEntityCode, final String attributeCode) {

        BaseEntity be = getBaseEntityByCode(baseEntityCode);
        Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
        if (ea.isPresent()) {
            return ea.get().getValueTime();
        } else {
            return null;
        }
    }

    public BaseEntity getParent(String targetCode, String linkCode) {
        return this.getParent(targetCode, linkCode, null);
    }

    public BaseEntity getParent(String targetCode, String linkCode, String prefix) {

        List<BaseEntity> parents = this.getParents(targetCode, linkCode, prefix);
        if (parents != null && !parents.isEmpty()) {
            return parents.get(0);
        }

        return null;
    }

    public List<BaseEntity> getParents(final String targetCode, final String linkCode) {
        return this.getParents(targetCode, linkCode, null);
    }

    public List<BaseEntity> getParents(String targetCode, String linkCode, String prefix) {

        List<BaseEntity> parents = null;
        long sTime = System.nanoTime();
        try {

            String beJson = QwandaUtils.apiGet(
                    this.qwandaServiceUrl + "/qwanda/entityentitys/" + targetCode + "/linkcodes/" + linkCode + "/parents",
                    this.token);
            Link[] linkArray = JsonUtils.fromJson(beJson, Link[].class);
            if (linkArray != null && linkArray.length > 0) {

                List<Link> arrayList = new CopyOnWriteArrayList<Link>(Arrays.asList(linkArray));
                parents = new CopyOnWriteArrayList<BaseEntity>();
                for (Link lnk : arrayList) {

                    BaseEntity linkedBe = getBaseEntityByCode(lnk.getSourceCode());
                    if (linkedBe != null) {

                        if (prefix == null) {
                            parents.add(linkedBe);
                        } else {
                            if (linkedBe.getCode().startsWith(prefix)) {
                                parents.add(linkedBe);
                            }
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        double difference = (System.nanoTime() - sTime) / 1e6; // get ms
        return parents;
    }

    public List<EntityEntity> getLinks(BaseEntity be) {
        return this.getLinks(be.getCode());
    }

    public List<EntityEntity> getLinks(String beCode) {

        List<EntityEntity> links = new CopyOnWriteArrayList<EntityEntity>();
        BaseEntity be = this.getBaseEntityByCode(beCode);
        if (be != null) {

            Set<EntityEntity> linkSet = be.getLinks();
            links.addAll(linkSet);
        }

        return links;
    }

    public BaseEntity getLinkedBaseEntity(String beCode, String linkCode, String linkValue) {

        List<BaseEntity> bes = this.getLinkedBaseEntities(beCode, linkCode, linkValue);
        if (bes != null && bes.size() > 0) {
            return bes.get(0);
        }

        return null;
    }

    public List<BaseEntity> getLinkedBaseEntities(BaseEntity be) {
        return this.getLinkedBaseEntities(be.getCode(), null, null);
    }

    public List<BaseEntity> getLinkedBaseEntities(String beCode) {
        return this.getLinkedBaseEntities(beCode, null, null);
    }

    public List<BaseEntity> getLinkedBaseEntities(BaseEntity be, String linkCode) {
        return this.getLinkedBaseEntities(be.getCode(), linkCode, null);
    }

    public List<BaseEntity> getLinkedBaseEntities(String beCode, String linkCode) {
        return this.getLinkedBaseEntities(beCode, linkCode, null);
    }

    public List<BaseEntity> getLinkedBaseEntities(BaseEntity be, String linkCode, String linkValue) {
        return this.getLinkedBaseEntities(be.getCode(), linkCode, linkValue);
    }

    public List<BaseEntity> getLinkedBaseEntities(String beCode, String linkCode, String linkValue) {
        return this.getLinkedBaseEntities(beCode, linkCode, linkValue, 1);
    }

    public List<BaseEntity> getLinkedBaseEntities(String beCode, String linkCode, String linkValue, Integer level) {

        List<BaseEntity> linkedBaseEntities = new CopyOnWriteArrayList<BaseEntity>();
        try {

            /* We grab all the links from the node passed as a parameter "beCode" */
            List<EntityEntity> links = this.getLinks(beCode);

            /* We loop through all the links */
            for (EntityEntity link : links) {

                if (link != null && link.getLink() != null) {

                    Link entityLink = link.getLink();

                    /* We get the targetCode */
                    String targetCode = entityLink.getTargetCode();
                    if (targetCode != null) {

                        /* We use the targetCode to get the base entity */
                        BaseEntity targetBe = this.getBaseEntityByCode(targetCode);
                        if (targetBe != null) {

                            /* If a linkCode is passed we filter using its value */
                            if (linkCode != null) {
                                if (entityLink.getAttributeCode() != null && entityLink.getAttributeCode().equals(linkCode)) {

                                    /* If a linkValue is passed we filter using its value */
                                    if (linkValue != null) {
                                        if (entityLink.getLinkValue() != null && entityLink.getLinkValue().equals(linkValue)) {
                                            linkedBaseEntities.add(targetBe);
                                        }
                                    } else {

                                        /* If no link value was provided we just pass the base entity */
                                        linkedBaseEntities.add(targetBe);
                                    }
                                }
                            } else {

                                /* If not linkCode was provided we just pass the base entity */
                                linkedBaseEntities.add(targetBe);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (level > 1) {

            List<List<BaseEntity>> nextLevels = linkedBaseEntities.stream()
                    .map(item -> this.getLinkedBaseEntities(item.getCode(), linkCode, linkValue, (level - 1)))
                    .collect(Collectors.toList());
            for (List<BaseEntity> nextLevel : nextLevels) {
                linkedBaseEntities.addAll(nextLevel);
            }
        }

        return linkedBaseEntities;
    }

    public List<BaseEntity> getBaseEntityWithChildren(String beCode, Integer level) {

        if (level == 0) {
            return null; // exit point;
        }

        level--;
        BaseEntity be = this.getBaseEntityByCode(beCode);
        if (be != null) {

            List<BaseEntity> beList = new CopyOnWriteArrayList<>();

            Set<EntityEntity> entityEntities = be.getLinks();

            // we interate through the links
            for (EntityEntity entityEntity : entityEntities) {

                Link link = entityEntity.getLink();
                if (link != null) {

                    // we get the target BE
                    String targetCode = link.getTargetCode();
                    if (targetCode != null) {

                        // recursion
                        beList.addAll(this.getBaseEntityWithChildren(targetCode, level));
                    }
                }
            }

            return beList;
        }

        return null;
    }

    public Boolean checkIfLinkExists(String parentCode, String linkCode, String childCode) {

        Boolean isLinkExists = false;
        QDataBaseEntityMessage dataBEMessage = QwandaUtils.getDataBEMessage(parentCode, linkCode, this.token);

        if (dataBEMessage != null) {
            BaseEntity[] beArr = dataBEMessage.getItems();

            if (beArr.length > 0) {
                for (BaseEntity be : beArr) {
                    if (be.getCode().equals(childCode)) {
                        isLinkExists = true;
                        return isLinkExists;
                    }
                }
            } else {
                isLinkExists = false;
                return isLinkExists;
            }

        }
        return isLinkExists;
    }

    /* Check If Link Exists and Available */
    public Boolean checkIfLinkExistsAndAvailable(String parentCode, String linkCode, String linkValue, String childCode) {
        Boolean isLinkExists = false;
        List<Link> links = getLinks(parentCode, linkCode);
        if (links != null) {
            for (Link link : links) {
                String linkVal = link.getLinkValue();

                if (linkVal != null && linkVal.equals(linkValue) && link.getTargetCode().equalsIgnoreCase(childCode)) {
                    Double linkWeight = link.getWeight();
                    if (linkWeight >= 1.0) {
                        isLinkExists = true;
                        return isLinkExists;
                    }
                }
            }
        }
        return isLinkExists;
    }

    /* returns a duplicated BaseEntity from an existing beCode */
    public BaseEntity duplicateBaseEntityAttributesAndLinks(final BaseEntity oldBe, final String bePrefix,
                                                            final String name) {

        BaseEntity newBe = this.create(oldBe.getCode(), bePrefix, name);
        duplicateAttributes(oldBe, newBe);
        duplicateLinks(oldBe, newBe);
        return getBaseEntityByCode(newBe.getCode());
    }

    public BaseEntity duplicateBaseEntityAttributes(final BaseEntity oldBe, final String bePrefix, final String name) {

        BaseEntity newBe = this.create(oldBe.getCode(), bePrefix, name);
        duplicateAttributes(oldBe, newBe);
        return getBaseEntityByCode(newBe.getCode());
    }

    public BaseEntity duplicateBaseEntityLinks(final BaseEntity oldBe, final String bePrefix, final String name) {

        BaseEntity newBe = this.create(oldBe.getCode(), bePrefix, name);
        duplicateLinks(oldBe, newBe);
        return getBaseEntityByCode(newBe.getCode());
    }

    public void duplicateAttributes(final BaseEntity oldBe, final BaseEntity newBe) {

        List<Answer> duplicateAnswerList = new CopyOnWriteArrayList<>();

        for (EntityAttribute ea : oldBe.getBaseEntityAttributes()) {
            duplicateAnswerList.add(new Answer(newBe.getCode(), newBe.getCode(), ea.getAttributeCode(), ea.getAsString()));
        }

        this.saveAnswers(duplicateAnswerList);
    }

    public void duplicateLinks(final BaseEntity oldBe, final BaseEntity newBe) {
        for (EntityEntity ee : oldBe.getLinks()) {
            createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
                    ee.getLink().getLinkValue(), ee.getLink().getWeight());
        }
    }

    public void duplicateLink(final BaseEntity oldBe, final BaseEntity newBe, final BaseEntity childBe) {
        for (EntityEntity ee : oldBe.getLinks()) {
            if (ee.getLink().getTargetCode() == childBe.getCode()) {

                createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
                        ee.getLink().getLinkValue(), ee.getLink().getWeight());
                break;
            }
        }
    }

    public void duplicateLinksExceptOne(final BaseEntity oldBe, final BaseEntity newBe, String linkValue) {
        for (EntityEntity ee : oldBe.getLinks()) {
            if (ee.getLink().getLinkValue() == linkValue) {
                continue;
            }
            createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
                    ee.getLink().getLinkValue(), ee.getLink().getWeight());
        }
    }

    public BaseEntity cloneBeg(final BaseEntity oldBe, final BaseEntity newBe, final BaseEntity childBe,
                               String linkValue) {
        duplicateLinksExceptOne(oldBe, newBe, linkValue);
        duplicateLink(oldBe, newBe, childBe);
        return getBaseEntityByCode(newBe.getCode());
    }

    /* clones links of oldBe to newBe from supplied arraylist linkValues */
    public BaseEntity copyLinks(final BaseEntity oldBe, final BaseEntity newBe, final String[] linkValues) {
        log.info("linkvalues   ::   " + Arrays.toString(linkValues));
        for (EntityEntity ee : oldBe.getLinks()) {
            log.info("old be linkValue   ::   " + ee.getLink().getLinkValue());
            for (String linkValue : linkValues) {
                log.info("a linkvalue   ::   " + linkValue);
                if (ee.getLink().getLinkValue().equals(linkValue)) {
                    createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
                            ee.getLink().getLinkValue(), ee.getLink().getWeight());
                    log.info("creating link for   ::   " + linkValue);
                }
            }
        }
        return getBaseEntityByCode(newBe.getCode());
    }

    /*
     * clones all links of oldBe to newBe except the linkValues supplied in
     * arraylist linkValues
     */
    public BaseEntity copyLinksExcept(final BaseEntity oldBe, final BaseEntity newBe, final String[] linkValues) {
        log.info("linkvalues   ::   " + Arrays.toString(linkValues));
        for (EntityEntity ee : oldBe.getLinks()) {
            log.info("old be linkValue   ::   " + ee.getLink().getLinkValue());
            for (String linkValue : linkValues) {
                log.info("a linkvalue   ::   " + linkValue);
                if (ee.getLink().getLinkValue().equals(linkValue)) {
                    continue;
                }
                createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
                        ee.getLink().getLinkValue(), ee.getLink().getWeight());
                log.info("creating link for   ::   " + linkValue);
            }
        }
        return getBaseEntityByCode(newBe.getCode());
    }

    public void updateBaseEntityStatus(BaseEntity be, String status) {
        this.updateBaseEntityStatus(be.getCode(), status);
    }

    public void updateBaseEntityStatus(String beCode, String status) {

        String attributeCode = "STA_STATUS";
        this.updateBaseEntityAttribute(beCode, beCode, attributeCode, status);
    }

    public void updateBaseEntityStatus(BaseEntity be, String userCode, String status) {
        this.updateBaseEntityStatus(be.getCode(), userCode, status);
    }

    public void updateBaseEntityStatus(String beCode, String userCode, String status) {

        String attributeCode = "STA_" + userCode;
        this.updateBaseEntityAttribute(userCode, beCode, attributeCode, status);

        /* new status for v3 */
        switch (status) {
            case "green":
            case "red":
            case "orange":
            case "yellow": {

                BaseEntity be = this.getBaseEntityByCode(beCode);
                if (be != null) {

                    String attributeCodeStatus = "STA_" + status.toUpperCase();
                    String existingValueArray = be.getValue(attributeCodeStatus, "[]");
                    JsonParser jsonParser = new JsonParser();
                    JsonArray existingValues = jsonParser.parse(existingValueArray).getAsJsonArray();
                    existingValues.add(userCode);
                    this.saveAnswer(new Answer(beCode, beCode, attributeCodeStatus, JsonUtils.toJson(existingValues)));
                }
            }
            default: {
            }
        }
    }

    public void updateBaseEntityStatus(BaseEntity be, List<String> userCodes, String status) {
        this.updateBaseEntityStatus(be.getCode(), userCodes, status);
    }

    public void updateBaseEntityStatus(String beCode, List<String> userCodes, String status) {

        for (String userCode : userCodes) {
            this.updateBaseEntityStatus(beCode, userCode, status);
        }
    }

    public List<Link> getLinks(final String parentCode, final String linkCode) {
        List<Link> links = RulesUtils.getLinks(this.qwandaServiceUrl, this.decodedMapToken, this.token, parentCode,
                linkCode);
        return links;
    }

    public String updateBaseEntity(BaseEntity be) {
        try {
            VertxUtils.writeCachedJson(getRealm(), be.getCode(), JsonUtils.toJson(be));
            return QwandaUtils.apiPutEntity(this.qwandaServiceUrl + "/qwanda/baseentitys", JsonUtils.toJson(be), this.token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String saveSearchEntity(SearchEntity se) { // TODO: Ugly
        String ret = null;
        try {
            if (se != null) {
                if (!se.hasCode()) {
                    log.error("ERROR! searchEntity se has no code!");
                }
                if (se.getId() == null) {
                    BaseEntity existing = VertxUtils.readFromDDT(getRealm(), se.getCode(), this.token);
                    if (existing != null) {
                        se.setId(existing.getId());
                    }
                }
                VertxUtils.writeCachedJson(getRealm(), se.getCode(), JsonUtils.toJson(se));
                if (se.getId() != null) {
                    ret = QwandaUtils.apiPutEntity(this.qwandaServiceUrl + "/qwanda/baseentitys", JsonUtils.toJson(se),
                            this.token);

                } else {
                    ret = QwandaUtils.apiPostEntity(this.qwandaServiceUrl + "/qwanda/baseentitys", JsonUtils.toJson(se),
                            this.token);
                }
                saveBaseEntityAttributes(se);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return ret;
    }

    public void saveBaseEntityAttributes(BaseEntity be) {
        if ((be == null) || (be.getCode() == null)) {
            throw new NullPointerException("Cannot save be because be is null or be.getCode is null");
        }
        List<Answer> answers = new CopyOnWriteArrayList<Answer>();

        for (EntityAttribute ea : be.getBaseEntityAttributes()) {
            Answer attributeAnswer = new Answer(be.getCode(), be.getCode(), ea.getAttributeCode(), ea.getAsString());
            attributeAnswer.setChangeEvent(false);
            answers.add(attributeAnswer);
        }
        this.saveAnswers(answers);
    }

    public <T extends BaseEntity> T updateCachedBaseEntity(final Answer answer, Class clazz) {
        T cachedBe = this.getBaseEntityByCode(answer.getTargetCode(), true, clazz);
        // Add an attribute if not already there
        try {
            String attributeCode = answer.getAttributeCode();
            if (!attributeCode.startsWith("RAW_")) {
                Attribute attribute = answer.getAttribute();

                if (RulesUtils.attributeMap != null) {
                    if (RulesUtils.attributeMap.isEmpty()) {
                        RulesUtils.loadAllAttributesIntoCache(token);
                    }
                    attribute = RulesUtils.attributeMap.get(attributeCode);

                    if (attribute != null) {
                        answer.setAttribute(attribute);
                    }

                    if (answer.getAttribute() == null || cachedBe == null) {
                        log.info("Null Attribute or null BE , targetCode=[" + answer.getTargetCode() + "]");
                    } else {
                        cachedBe.addAnswer(answer);
                    }
                    VertxUtils.writeCachedJson(getRealm(), answer.getTargetCode(), JsonUtils.toJson(cachedBe), this.token);
                }
            }

            /*
             * answer.setAttribute(RulesUtils.attributeMap.get(answer.getAttributeCode()));
             * if (answer.getAttribute() == null) { log.info("Null Attribute"); } else
             * cachedBe.addAnswer(answer);
             * VertxUtils.writeCachedJson(answer.getTargetCode(),
             * JsonUtils.toJson(cachedBe));
             *
             */
        } catch (BadDataException e) {
            e.printStackTrace();
        }
        return cachedBe;
    }

    public <T extends BaseEntity> T updateCachedBaseEntity(final Answer answer) {
        return updateCachedBaseEntity(answer, BaseEntity.class);
    }

    public BaseEntity updateCachedBaseEntity(List<Answer> answers) {
        Answer firstanswer = null;
        if ((answers != null) && (!answers.isEmpty())) {
            firstanswer = answers.get(0);
        } else {
            throw new NullPointerException("Answers cannot be null or empty for updateCacheBaseEntity");
        }
        BaseEntity cachedBe = null;

        if (firstanswer != null) {
            if (firstanswer.getTargetCode() == null) {
                throw new NullPointerException("firstanswer getTargetCode cannot be null for updateCacheBaseEntity");
            }
            // log.info("firstAnswer.targetCode=" + firstanswer.getTargetCode());
            cachedBe = this.getBaseEntityByCode(firstanswer.getTargetCode());
        } else {
            return null;
        }

        if (cachedBe != null) {
            if ((cachedBe == null) || (cachedBe.getCode() == null)) {
                throw new NullPointerException("cachedBe.getCode cannot be null for updateCacheBaseEntity , targetCode="
                        + firstanswer.getTargetCode() + " cacheBe=[" + cachedBe);
            }
        } else {
            throw new NullPointerException(
                    "cachedBe cannot be null for updateCacheBaseEntity , targetCode=" + firstanswer.getTargetCode());

        }

        for (Answer answer : answers) {

            if (!answer.getAttributeCode().startsWith("RAW_")) {

                // Add an attribute if not already there
                try {
                    if (answer.getAttribute() == null) {
                        Attribute attribute = RulesUtils.getAttribute(answer.getAttributeCode(), token);

                        if (attribute != null) {
                            answer.setAttribute(attribute);
                        }
                    }
                    if (answer.getAttribute() == null) {
                        continue;
                    }
                    cachedBe.addAnswer(answer);
                } catch (BadDataException e) {
                    e.printStackTrace();
                }
            }
        }

        VertxUtils.writeCachedJson(getRealm(), cachedBe.getCode(), JsonUtils.toJson(cachedBe), this.token);

        return cachedBe;
    }

    public Link createLink(String sourceCode, String targetCode, String linkCode, String linkValue, Double weight) {

        System.out.println("CREATING LINK between " + sourceCode + "and" + targetCode + "with LINK VALUE = " + linkValue);
        Link link = new Link(sourceCode, targetCode, linkCode, linkValue);
        link.setWeight(weight);
        try {
            BaseEntity source = this.getBaseEntityByCode(sourceCode);
            BaseEntity target = this.getBaseEntityByCode(targetCode);
            Attribute linkAttribute = new AttributeLink(linkCode, linkValue);
            try {
                source.addTarget(target, linkAttribute, weight, linkValue);
                this.updateBaseEntity(source);

                QwandaUtils.apiPostEntity(qwandaServiceUrl + "/qwanda/entityentitys", JsonUtils.toJson(link), this.token);
            } catch (BadDataException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return link;
    }

    public Link updateLink(String sourceCode, String targetCode, String linkCode, String linkValue, Double weight) {

        System.out.println("UPDATING LINK between " + sourceCode + "and" + targetCode + "with LINK VALUE = " + linkValue);
        Link link = new Link(sourceCode, targetCode, linkCode, linkValue);
        link.setWeight(weight);
        try {
            BaseEntity source = this.getBaseEntityByCode(sourceCode);
            BaseEntity target = this.getBaseEntityByCode(targetCode);
            Attribute linkAttribute = new AttributeLink(linkCode, linkValue);
            source.addTarget(target, linkAttribute, weight, linkValue);

            this.updateBaseEntity(source);
            QwandaUtils.apiPutEntity(qwandaServiceUrl + "/qwanda/links", JsonUtils.toJson(link), this.token);
        } catch (IOException | BadDataException e) {
            e.printStackTrace();
        }
        return link;
    }

    public Link updateLink(String groupCode, String targetCode, String linkCode, Double weight) {

        log.info("UPDATING LINK between " + groupCode + "and" + targetCode);
        Link link = new Link(groupCode, targetCode, linkCode);
        link.setWeight(weight);
        try {
            QwandaUtils.apiPutEntity(qwandaServiceUrl + "/qwanda/links", JsonUtils.toJson(link), this.token);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return link;
    }

    public Link[] getUpdatedLink(String parentCode, String linkCode) {
        List<Link> links = this.getLinks(parentCode, linkCode);
        Link[] items = new Link[links.size()];
        items = (Link[]) links.toArray(items);
        return items;
    }

    /*
     * Gets all the attribute and their value for the given basenentity code
     */
    public Map<String, String> getMapOfAllAttributesValuesForBaseEntity(String beCode) {

        BaseEntity be = this.getBaseEntityByCode(beCode);
        log.info("The load is ::" + be);
        Set<EntityAttribute> eaSet = be.getBaseEntityAttributes();
        log.info("The set of attributes are  :: " + eaSet);
        Map<String, String> attributeValueMap = new HashMap<String, String>();
        for (EntityAttribute ea : eaSet) {
            String attributeCode = ea.getAttributeCode();
            log.info("The attribute code  is  :: " + attributeCode);
            String value = ea.getAsLoopString();
            attributeValueMap.put(attributeCode, value);
        }

        return attributeValueMap;
    }

    public List getLinkList(String groupCode, String linkCode, String linkValue, String token) {

        // String qwandaServiceUrl = "http://localhost:8280";
        List linkList = null;

        try {
            String attributeString = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/entityentitys/" + groupCode
                    + "/linkcodes/" + linkCode + "/children/" + linkValue, token);
            if (attributeString != null) {
                linkList = JsonUtils.fromJson(attributeString, List.class);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return linkList;

    }

    /*
     * Returns only non-hidden links or all the links based on the includeHidden
     * value
     */
    public List getLinkList(String groupCode, String linkCode, String linkValue, Boolean includeHidden) {

        // String qwandaServiceUrl = "http://localhost:8280";
        BaseEntity be = getBaseEntityByCode(groupCode);
        List<Link> links = getLinks(groupCode, linkCode);

        List linkList = null;

        if (links != null) {
            for (Link link : links) {
                String linkVal = link.getLinkValue();

                if (linkVal != null && linkVal.equals(linkValue)) {
                    Double linkWeight = link.getWeight();
                    if (!includeHidden) {
                        if (linkWeight >= 1.0) {
                            linkList.add(link);

                        }
                    } else {
                        linkList.add(link);
                    }
                }
            }
        }
        return linkList;

    }

    /*
     * Sorting Columns of a SearchEntity as per the weight in either Ascending or
     * descending order
     */
    public List<String> sortEntityAttributeBasedOnWeight(final List<EntityAttribute> ea, final String sortOrder) {

        if (ea.size() > 1) {
            Collections.sort(ea, new Comparator<EntityAttribute>() {

                @Override
                public int compare(EntityAttribute ea1, EntityAttribute ea2) {
                    if (ea1.getWeight() != null && ea2.getWeight() != null) {
                        if (sortOrder.equalsIgnoreCase("ASC"))
                            return (ea1.getWeight()).compareTo(ea2.getWeight());
                        else
                            return (ea2.getWeight()).compareTo(ea1.getWeight());

                    } else
                        return 0;
                }
            });
        }

        List<String> searchHeader = new CopyOnWriteArrayList<String>();
        for (EntityAttribute ea1 : ea) {
            searchHeader.add(ea1.getAttributeCode().substring("COL_".length()));
        }

        return searchHeader;
    }

    public BaseEntity baseEntityForLayout(String realm, String token, Layout layout) {

        if (GennySettings.disableLayoutLoading) {
            return null;
        }
        if (layout.getPath() == null) {
            return null;
        }

        String serviceToken = RulesUtils.generateServiceToken(realm, token);
        if (serviceToken != null) {

            BaseEntity beLayout = null;

            /* we check if the baseentity for this layout already exists */
            // beLayout =
            // RulesUtils.getBaseEntityByAttributeAndValue(RulesUtils.qwandaServiceUrl,
            // this.decodedTokenMap, this.token, "PRI_LAYOUT_URI", layout.getPath());
            if (layout.getPath().contains("bucket")) {
                log.info("bucket");
            }
            String precode = String.valueOf(layout.getPath().replaceAll("[^a-zA-Z0-9]", "").toUpperCase().hashCode());
            String layoutCode = ("LAY_" + realm + "_" + precode).toUpperCase();

            log.info("Layout - Handling " + layoutCode);
            // try {
            // Check if in cache first to save time.
            beLayout = VertxUtils.readFromDDT(realm, layoutCode, serviceToken);
            // if (beLayout==null) {
            // beLayout = QwandaUtils.getBaseEntityByCode(layoutCode, serviceToken);
            // if (beLayout != null) {
            // VertxUtils.writeCachedJson(layoutCode, JsonUtils.toJson(beLayout),
            // serviceToken);
            // }
            // }

            // } catch (IOException e) {
            // log.error(e.getMessage());
            // }

            /* if the base entity does not exist, we create it */
            if (beLayout == null) {

                log.info("Layout - Creating base entity " + layoutCode);

                /* otherwise we create it */
                beLayout = this.create(layoutCode, layout.getName());
            }

            // if (beLayout != null) {
            //
            // log.info("Layout - Creating base entity " + layoutCode);
            //
            // /* otherwise we create it */
            // beLayout = this.create(layoutCode, layout.getName());
            // }

            if (beLayout != null) {

                this.addAttributes(beLayout);

                /*
                 * we get the modified time stored in the BE and we compare it to the layout one
                 */
                String beModifiedTime = beLayout.getValue("PRI_LAYOUT_MODIFIED_DATE", null);

                log.debug("*** match layout mod date [" + layout.getModifiedDate() + "] with be layout [" + beModifiedTime);

                /* if the modified time is not the same, we update the layout BE */
                /* setting layout attributes */
                List<Answer> answers = new CopyOnWriteArrayList<>();

                /* download the content of the layout */
                String content = LayoutUtils.downloadLayoutContent(layout);
                int existingLayoutHashcode = layout.getData().trim().hashCode();
                int contentHashcode = content.trim().hashCode();

                Optional<EntityAttribute> primaryLayoutData = beLayout.findEntityAttribute("PRI_LAYOUT_DATA");
                String beData = null;
                int behc = 0;
                if (primaryLayoutData.isPresent()) {
                    log.debug("beLayout.findEntityAttribute(\"PRI_LAYOUT_DATA\").get().getAsString().trim().hashcode()="
                            + beLayout.findEntityAttribute("PRI_LAYOUT_DATA").get().getAsString().trim().hashCode());
                    beData = beLayout.findEntityAttribute("PRI_LAYOUT_DATA").get().getAsString().trim();
                    log.debug("baseentity.hashcode()=" + beData.hashCode());
                    behc = beData.hashCode();
                }
                log.debug("layout.getData().hashcode()=" + existingLayoutHashcode);
                log.debug("content.hashcode()=" + contentHashcode);

                if (!GennySettings.disableLayoutLoading && (true /* behc != contentHashcode */)) {
                    log.info("Resaving layout: " + layoutCode);

                    Answer newAnswerContent = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_DATA", content);

                    newAnswerContent.setChangeEvent(false);
                    answers.add(newAnswerContent);

                    Answer newAnswer = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_URI", layout.getPath());
                    answers.add(newAnswer);

                    Answer newAnswer2 = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_URL",
                            layout.getDownloadUrl());
                    answers.add(newAnswer2);

                    Answer newAnswer3 = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_NAME", layout.getName());
                    answers.add(newAnswer3);

                    Answer newAnswer4 = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_MODIFIED_DATE",
                            layout.getModifiedDate());
                    answers.add(newAnswer4);

                    this.saveAnswers(answers, false); // No change events required

                    /* create link between GRP_LAYOUTS and this new LAY_XX base entity */
                    this.createLink("GRP_LAYOUTS", beLayout.getCode(), "LNK_CORE", "LAYOUT", 1.0);
                } else {
                    log.info("Already have same layout data - not saving ");
                }
            }

            return beLayout;
        }

        return null;
    }

    /*
     * copy all the attributes from one BE to another BE sourceBe : FROM targetBe :
     * TO
     */
    public BaseEntity copyAttributes(final BaseEntity sourceBe, final BaseEntity targetBe) {

        Map<String, String> map = new HashMap<>();
        map = getMapOfAllAttributesValuesForBaseEntity(sourceBe.getCode());
        RulesUtils.ruleLogger("MAP DATA   ::   ", map);

        List<Answer> answers = new CopyOnWriteArrayList<Answer>();
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Answer answerObj = new Answer(sourceBe.getCode(), targetBe.getCode(), entry.getKey(), entry.getValue());
                answers.add(answerObj);
            }
            saveAnswers(answers);
        } catch (Exception e) {
        }

        return getBaseEntityByCode(targetBe.getCode());
    }

    public String removeLink(final String parentCode, final String childCode, final String linkCode) {
        Link link = new Link(parentCode, childCode, linkCode);
        try {
            return QwandaUtils.apiDelete(this.qwandaServiceUrl + "/qwanda/entityentitys", JsonUtils.toJson(link), this.token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /* Remove link with specific link Value */
    public String removeLink(final String parentCode, final String childCode, final String linkCode,
                             final String linkValue) {
        Link link = new Link(parentCode, childCode, linkCode, linkValue);
        try {
            return QwandaUtils.apiDelete(this.qwandaServiceUrl + "/qwanda/entityentitys", JsonUtils.toJson(link), this.token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String removeBaseEntity(final String baseEntityCode) {
        try {

            String result = QwandaUtils.apiDelete(this.qwandaServiceUrl + "/qwanda/baseentitys/" + baseEntityCode,
                    this.token);
            log.info("Result from remove user: " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
    }

    /*
     * Returns comma seperated list of all the childcode for the given parent code
     * and the linkcode
     */
    public String getAllChildCodes(final String parentCode, final String linkCode) {
        String childs = null;
        List<String> childBECodeList = new CopyOnWriteArrayList<String>();
        List<BaseEntity> childBE = this.getLinkedBaseEntities(parentCode, linkCode);
        if (childBE != null) {
            for (BaseEntity be : childBE) {
                childBECodeList.add(be.getCode());
            }
            childs = "\"" + String.join("\", \"", childBECodeList) + "\"";
            childs = "[" + childs + "]";
        }

        return childs;
    }

    /* Get array String value from an attribute of the BE */
    public List<String> getBaseEntityAttrValueList(BaseEntity be, String attributeCode) {

        String myLoadTypes = be.getValue(attributeCode, null);

        if (myLoadTypes != null) {
            List<String> loadTypesList = new CopyOnWriteArrayList<String>();
            /* Removing brackets "[]" and double quotes from the strings */
            String trimmedStr = myLoadTypes.substring(1, myLoadTypes.length() - 1).toString().replaceAll("\"", "");
            if (trimmedStr != null && !trimmedStr.isEmpty()) {
                loadTypesList = Arrays.asList(trimmedStr.split("\\s*,\\s*"));
                return loadTypesList;
            } else {
                return null;
            }
        } else
            return null;
    }

    public <T extends QMessage> QBulkPullMessage createQBulkPullMessage(T msg) {

        // Put the QBulkMessage into the PontoonDDT
        UUID uuid = UUID.randomUUID();
        // DistMap.getDistBE(gennyToken.getRealm()).put("PONTOON_"+uuid.toString(),
        // JsonUtils.toJson(msg), 2, TimeUnit.MINUTES);
        VertxUtils.writeCachedJson(gennyToken.getRealm(), "PONTOON_" + uuid.toString().toUpperCase(), JsonUtils.toJson(msg),
                this.getGennyToken().getToken(), GennySettings.pontoonTimeout); // 2 minutes

        // DistMap.getDistPontoonBE(gennyToken.getRealm()).put(uuid.toString(),
        // JsonUtils.toJson(msg), 2, TimeUnit.MINUTES);

        QBulkPullMessage pullMsg = new QBulkPullMessage(uuid.toString());
        pullMsg.setToken(this.getGennyToken().getToken());
        // Put the QBulkMessage into the PontoonDDT

        // then create the QBulkPullMessage
        pullMsg.setPullUrl(/* GennySettings.pontoonUrl + */ "api/pull/" + uuid.toString().toUpperCase());
        return pullMsg;

    }

    public QBulkPullMessage createQBulkPullMessage(String msg) {

        // Put the QBulkMessage into the PontoonDDT
        UUID uuid = UUID.randomUUID();
        // DistMap.getDistBE(gennyToken.getRealm()).put("PONTOON_"+uuid.toString(),
        // JsonUtils.toJson(msg), 2, TimeUnit.MINUTES);
        VertxUtils.writeCachedJson(this.getGennyToken().getRealm(), "PONTOON_" + uuid.toString().toUpperCase(), msg,
                this.getGennyToken().getToken(), GennySettings.pontoonTimeout); // 2 minutes

        QBulkPullMessage pullMsg = new QBulkPullMessage(uuid.toString());
        pullMsg.setToken(token);

        // then create the QBulkPullMessage
        pullMsg.setPullUrl(/* GennySettings.pontoonUrl + */ "api/pull/" + uuid.toString().toUpperCase());
        return pullMsg;

    }

    public static QBulkPullMessage createQBulkPullMessage(JsonObject msg) {

        UUID uuid = UUID.randomUUID();
        String token = msg.getString("token");
        String realm = msg.getString("realm");

        // Put the QBulkMessage into the PontoonDDT
        // DistMap.getDistPontoonBE(gennyToken.getRealm()).put(uuid, msg, 2,
        // TimeUnit.MINUTES);
        VertxUtils.writeCachedJson(realm, "PONTOON_" + uuid.toString().toUpperCase(), JsonUtils.toJson(msg), token,
                GennySettings.pontoonTimeout);

        // then create the QBulkPullMessage
        QBulkPullMessage pullMsg = new QBulkPullMessage(uuid.toString());
        pullMsg.setToken(token);
        pullMsg.setPullUrl(/* GennySettings.pontoonUrl + */ "api/pull/" + uuid.toString().toUpperCase());
        return pullMsg;

    }

    /**
     * @return the gennyToken
     */
    public GennyToken getGennyToken() {
        return gennyToken;
    }

    /**
     * @param gennyToken the gennyToken to set
     */
    public void setGennyToken(GennyToken gennyToken) {
        this.gennyToken = gennyToken;
    }

    @Override
    public String toString() {
        return "BaseEntityUtils [" + (realm != null ? "realm=" + realm : "") + ": "
                + StringUtils.abbreviateMiddle(token, "...", 30) + "]";
    }

    /**
     * @return the serviceToken
     */
    public GennyToken getServiceToken() {
        return serviceToken;
    }

    /**
     * @param serviceToken the serviceToken to set
     */
    public void setServiceToken(GennyToken serviceToken) {
        this.serviceToken = serviceToken;
    }

    /**
     * @param searchBE
     * @return
     */
    public List<BaseEntity> getBaseEntitys(final SearchEntity searchBE) {
        List<BaseEntity> results = new ArrayList<BaseEntity>();

        Tuple2<String, List<String>> data = getHql(searchBE);
        String hql = data._1;

        hql = Base64.getUrlEncoder().encodeToString(hql.getBytes());
        try {
            String resultJsonStr = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search24/" + hql
                            + "/" + searchBE.getPageStart(0) + "/" + searchBE.getPageSize(GennySettings.defaultPageSize),
                    serviceToken.getToken(), 120);

            JsonObject resultJson = null;

            try {
                resultJson = new JsonObject(resultJsonStr);
                io.vertx.core.json.JsonArray result = resultJson.getJsonArray("codes");
                for (int i = 0; i < result.size(); i++) {
                    String code = result.getString(i);
                    BaseEntity be = getBaseEntityByCode(code);
                    be.setIndex(i);
                    results.add(be);
                }

            } catch (Exception e1) {
                log.error("Bad Json -> [" + resultJsonStr);
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return results;
    }

    public Tuple2<String, List<String>> getHql(SearchEntity searchBE) {
        List<String> attributeFilter = new ArrayList<String>();

        String beFilter1 = null;
        String beFilter2 = null;
        String beFilter3 = null;
        String beFilter4 = null;
        String beSorted = null;
        String attributeFilterValue1 = "";
        String attributeFilterCode1 = null;
        String attributeFilterValue2 = "";
        String attributeFilterCode2 = null;
        String attributeFilterValue3 = "";
        String attributeFilterCode3 = null;
        String attributeFilterValue4 = "";
        String attributeFilterCode4 = null;
        String sortCode = null;
        String sortValue = null;
        String sortType = null;
        String wildcardValue = null;
        Integer pageStart = searchBE.getPageStart(0);
        Integer pageSize = searchBE.getPageSize(GennySettings.defaultPageSize);

        for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {

            if (ea.getAttributeCode().startsWith("PRI_CODE")) {
                if (beFilter1 == null) {
                    beFilter1 = ea.getAsString();
                } else if (beFilter2 == null) {
                    beFilter2 = ea.getAsString();
                }

            } else if ((ea.getAttributeCode().startsWith("SRT_"))) {
                if (ea.getAttributeCode().startsWith("SRT_PRI_CREATED")) {
                    beSorted = " order by ea.created";
                    sortValue = ea.getValueString();
                } else if (ea.getAttributeCode().startsWith("SRT_PRI_UPDATED")) {
                    beSorted = " order by ea.updated";
                    sortValue = ea.getValueString();
                } else if (ea.getAttributeCode().startsWith("SRT_PRI_CODE")) {
                    beSorted = " order by ea.baseEntityCode";
                    sortValue = ea.getValueString();
                } else if (ea.getAttributeCode().startsWith("SRT_PRI_NAME")) {
                    beSorted = " order by ea.pk.baseEntity.name";
                    sortValue = ea.getValueString();
                } else {
                    sortCode = (ea.getAttributeCode().substring("SRT_".length()));
                    sortValue = ea.getValueString();
                    if (ea.getValueString() != null) {
                        sortType = "ez.valueString";
                    } else if (ea.getValueBoolean() != null) {
                        sortType = "ez.valueBoolean";
                    } else if (ea.getValueDouble() != null) {
                        sortType = "ez.valueDouble";
                    } else if (ea.getValueInteger() != null) {
                        sortType = "ez.valueInteger";
                    } else if (ea.getValueLong() != null) {
                        sortType = "ez.valueLong";
                    } else if (ea.getValueDateTime() != null) {
                        sortType = "ez.valueDateTime";
                    } else if (ea.getValueDate() != null) {
                        sortType = "ez.valueDate";
                    } else if (ea.getValueTime() != null) {
                        sortType = "ez.valueTime";
                    }
                }

            } else if ((ea.getAttributeCode().startsWith("COL_")) || (ea.getAttributeCode().startsWith("CAL_"))) {
                attributeFilter.add(ea.getAttributeCode().substring("COL_".length()));
            } else if (ea.getAttributeCode().startsWith("SCH_WILDCARD")) {
                if (ea.getValueString() != null) {
                    if (!StringUtils.isBlank(ea.getValueString())) {
                        wildcardValue = ea.getValueString();
                        wildcardValue = wildcardValue.replaceAll(("[^A-Za-z0-9 ]"), "");
                    }
                }

            } else if ((ea.getAttributeCode().startsWith("PRI_") || ea.getAttributeCode().startsWith("LNK_"))
                    && (!ea.getAttributeCode().equals("PRI_CODE")) && (!ea.getAttributeCode().equals("PRI_TOTAL_RESULTS"))
                    && (!ea.getAttributeCode().equals("PRI_INDEX"))) {
                String condition = SearchEntity.convertFromSaveable(ea.getAttributeName());
                if (condition == null) {
                    log.error("SQL condition is NULL, " + "EntityAttribute baseEntityCode is:" + ea.getBaseEntityCode()
                            + ", attributeCode is:" + ea.getAttributeCode());
                }

                if (attributeFilterCode1 == null) {
                    attributeFilterValue1 = " eb" + getAttributeValue(ea, condition);
                    attributeFilterCode1 = ea.getAttributeCode();
                } else {
                    if (attributeFilterCode2 == null) {
                        attributeFilterValue2 = " ec" + getAttributeValue(ea, condition);
                        attributeFilterCode2 = ea.getAttributeCode();
                    } else {
                        if (attributeFilterCode3 == null) {
                            attributeFilterValue3 = " ed" + getAttributeValue(ea, condition);
                            attributeFilterCode3 = ea.getAttributeCode();
                        } else {
                            if (attributeFilterCode4 == null) {
                                attributeFilterValue4 = " ee" + getAttributeValue(ea, condition);
                                attributeFilterCode4 = ea.getAttributeCode();
                            }
                        }
                    }
                }
            }
        }
        String sortBit = "";
        // if (sortCode != null) {
        // sortBit = ","+sortType +" ";
        // }
        String hql = "select distinct ea.baseEntityCode " + sortBit + " from EntityAttribute ea ";

        if (attributeFilterCode1 != null) {
            hql += ", EntityAttribute eb ";
        }
        if (attributeFilterCode2 != null) {
            hql += ", EntityAttribute ec ";
        }
        if (attributeFilterCode3 != null) {
            hql += ", EntityAttribute ed ";
        }
        if (attributeFilterCode4 != null) {
            hql += ", EntityAttribute ee ";
        }

        if (wildcardValue != null) {
            hql += ", EntityAttribute ew ";
        }

        if (sortCode != null) {
            hql += ", EntityAttribute ez ";
        }
        hql += " where ";

        if (attributeFilterCode1 != null) {
            hql += " ea.baseEntityCode=eb.baseEntityCode ";
            if (beFilter1 != null) {
                hql += " and (ea.baseEntityCode like '" + beFilter1 + "'  ";
            }
        } else {
            if (beFilter1 != null) {
                hql += " (ea.baseEntityCode like '" + beFilter1 + "'  ";
            }
        }

        if (searchBE.getCode().startsWith("SBE_SEARCHBAR")) {
            // search across people and companies
            hql += " and (ea.baseEntityCode like 'PER_%' or ea.baseEntityCode like 'CPY_%') ";
        }

        if (beFilter2 != null) {
            hql += " or ea.baseEntityCode like '" + beFilter2 + "'";
        }
        if (beFilter3 != null) {
            hql += " or ea.baseEntityCode like '" + beFilter3 + "'";
        }
        if (beFilter4 != null) {
            hql += " or ea.baseEntityCode like '" + beFilter4 + "'";
        }

        if (beFilter1 != null) {
            hql += ")  ";
        }

        if (attributeFilterCode1 != null) {
            hql += " and eb.attributeCode = '" + attributeFilterCode1 + "'"
                    + ((!StringUtils.isBlank(attributeFilterValue1)) ? (" and " + attributeFilterValue1) : "");
        }
        if (attributeFilterCode2 != null) {
            hql += " and ea.baseEntityCode=ec.baseEntityCode ";
            hql += " and ec.attributeCode = '" + attributeFilterCode2 + "'"
                    + ((!StringUtils.isBlank(attributeFilterValue2)) ? (" and " + attributeFilterValue2) : "");
        }
        if (attributeFilterCode3 != null) {
            hql += " and ea.baseEntityCode=ed.baseEntityCode ";
            hql += " and ed.attributeCode = '" + attributeFilterCode3 + "'"
                    + ((!StringUtils.isBlank(attributeFilterValue3)) ? (" and " + attributeFilterValue3) : "");
        }
        if (attributeFilterCode4 != null) {
            hql += " and ea.baseEntityCode=ee.baseEntityCode ";
            hql += " and ee.attributeCode = '" + attributeFilterCode4 + "'"
                    + ((!StringUtils.isBlank(attributeFilterValue4)) ? (" and " + attributeFilterValue4) : "");
        }

        if (wildcardValue != null) {
            hql += " and ea.baseEntityCode=ew.baseEntityCode and ew.valueString like '%" + wildcardValue + "%' ";
        }

        if (beSorted != null) {
            hql += " " + beSorted + " " + sortValue;

        } else if (sortCode != null) {
            hql += " and ea.baseEntityCode=ez.baseEntityCode and ez.attributeCode='" + sortCode + "' ";
            hql += " order by " + sortType + " " + sortValue;
        }
        return Tuple.of(hql, attributeFilter);
    }

    public String getAttributeValue(EntityAttribute ea, String condition) {
        if (ea.getValueString() != null) {
            return ".valueString " + condition + " '" + ea.getValueString() + "'";
        } else if (ea.getValueBoolean() != null) {
            return ".valueBoolean = " + (ea.getValueBoolean() ? "true" : "false");
        } else if (ea.getValueDouble() != null) {
            return ".valueDouble =ls" + ":q" + " " + ea.getValueDouble() + "";
        } else if (ea.getValueInteger() != null) {
            return ".valueInteger " + condition + " " + ea.getValueInteger() + "";
        } else if (ea.getValueDate() != null) {
            return ".valueDate = " + ea.getValueDate() + "";
        } else if (ea.getValueDateTime() != null) {
            return ".valueDateTime = " + ea.getValueDateTime() + "";
        }
        return null;
    }

    public List<BaseEntity> getRoles() {
        List<BaseEntity> roles = new ArrayList<BaseEntity>();
        BaseEntity be = this.getBaseEntityByCode(this.getGennyToken().getUserCode());
        if (be.getCode().startsWith("PER_")) {
            for (EntityAttribute ea : be.getBaseEntityAttributes()) {
                if (ea.getAttributeCode().startsWith("PRI_IS_")) {
                    String roleCode = "ROL_" + ea.getAttributeCode().substring("PRI_IS_".length());
                    BaseEntity role = this.getBaseEntityByCode(roleCode);
                    if (role != null) {
                        roles.add(role);
                    }
                }
            }
        }

        return roles;
    }

    public String getEmailFromOldCode(String oldCode) {
        String ret = null;
        if (oldCode.contains("_AT_")) {

            if ("PER_JIUNWEI_DOT_LU_AT_CQUMAIL_DOT_COM".equals(oldCode)) {
                oldCode = "PER_JIUN_DASH_WEI_DOT_LU_AT_CQUMAIL_DOT_COM"; // addd dash
            }

            oldCode = oldCode.substring(4);
            // convert to email
            oldCode = oldCode.replaceAll("_PLUS_", "+");
            oldCode = oldCode.replaceAll("_DOT_", ".");
            oldCode = oldCode.replaceAll("_AT_", "@");
            oldCode = oldCode.replaceAll("_DASH_", "-");
            ret = oldCode.toLowerCase();
        }
        return ret;
    }

    public BaseEntity getPersonFromEmail(String email) {
        BaseEntity person = null;

        SearchEntity searchBE = new SearchEntity("SBE_TEST", "email").addSort("PRI_NAME", "Created", SearchEntity.Sort.ASC)
                .addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PER_%")
                .addFilter("PRI_EMAIL", SearchEntity.StringFilter.LIKE, email).addColumn("PRI_CODE", "Name")
                .addColumn("PRI_EMAIL", "Email");

        searchBE.setRealm(realm);
        searchBE.setPageStart(0);
        searchBE.setPageSize(100000);
        Tuple2<String, List<String>> emailhqlTuple = getHql(searchBE);
        String emailhql = emailhqlTuple._1;

        emailhql = Base64.getUrlEncoder().encodeToString(emailhql.getBytes());
        try {
            String resultJsonStr = QwandaUtils
                    .apiGet(
                            GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search24/" + emailhql + "/"
                                    + searchBE.getPageStart(0) + "/" + searchBE.getPageSize(GennySettings.defaultPageSize),
                            serviceToken.getToken(), 120);

            JsonObject resultJson = null;
            resultJson = new JsonObject(resultJsonStr);
            io.vertx.core.json.JsonArray result2 = resultJson.getJsonArray("codes");
            String internCode = result2.getString(0);
            if (internCode.contains("_AT_")) {
                person = getBaseEntityByCode(internCode);
            }

        } catch (Exception e) {

        }
        return person;
    }

    public JsonObject writeMsg(BaseEntity be) {
        return writeMsg(be, new String[0]);
    }

    public JsonObject writeMsg(BaseEntity be, String... rxList) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
        msg.setToken(this.gennyToken.getToken());
        msg.setReplace(true);
        if ((rxList != null) && (rxList.length > 0)) {
            msg.setRecipientCodeArray(rxList);
            return VertxUtils.writeMsg("project", msg);
        } else {
            return VertxUtils.writeMsg("webdata", msg);
        }
    }

    public void processVideoAttribute(EntityAttribute ea) {
        String value = ea.getValueString();
        if (value != null && !value.startsWith("http")) {

            log.info("My Interview");
            BaseEntity project = this.getBaseEntityByCode("PRJ_" + this.getGennyToken().getRealm().toUpperCase());
            String apiKey = project.getValueAsString("ENV_API_KEY_MY_INTERVIEW");
            String secretToken = project.getValueAsString("ENV_SECRET_MY_INTERVIEW");
            long unixTimestamp = Instant.now().getEpochSecond();
            String apiSecret = apiKey + secretToken + unixTimestamp;
            String hashed = BCrypt.hashpw(apiSecret, BCrypt.gensalt(10));
            String videoId = ea.getValueString();
            String url = "https://api.myinterview.com/2.21.2/getVideo?apiKey=" + apiKey + "&hashTimestamp=" + unixTimestamp
                    + "&hash=" + hashed + "&video=" + videoId;
            String url2 = "https://embed.myinterview.com/player.v3.html?apiKey=" + apiKey + "&hashTimestamp=" + unixTimestamp
                    + "&hash=" + hashed + "&video=" + videoId + "&autoplay=1&fs=0";

            log.info("MyInterview Hash is " + url);
            log.info("MyInterview Hash2 is " + url2);
            ea.setValue(url2);

        }
    }
}


