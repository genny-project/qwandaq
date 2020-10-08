package life.genny.qwanda.utils;

import com.google.gson.reflect.TypeToken;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.Answer;
import life.genny.qwanda.GennyToken;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.qwandautils.GennySettings;
import life.genny.qwanda.qwandautils.JsonUtils;
import life.genny.qwanda.qwandautils.QwandaUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.drools.compiler.compiler.DrlParser;
import org.drools.compiler.compiler.DroolsError;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.lang.descr.AttributeDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kie.internal.builder.conf.LanguageLevelOption;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class RulesUtils {
    public static final String ANSI_YELLOW = "\u001B[33m";

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    public static final String ANSI_RESET = "\u001B[0m";
    static public QDataAttributeMessage attributesMsg = null;


    static public Map<String, Attribute> attributeMap = new ConcurrentHashMap<>();

    public static void println(final Object obj, final String colour) {
        Date date = new Date();
        if (GennySettings.devMode) {
            log.info(date + ": " + obj);
        } else {
            log.info((GennySettings.devMode ? "" : colour) + ": " + obj
                    + (GennySettings.devMode ? "" : ANSI_RESET));
        }

    }

    public static void println(final Object obj) {
        println(obj, ANSI_RESET);
    }


    public static QDataAttributeMessage loadAllAttributesIntoCache(final String token) {
        return loadAllAttributesIntoCache(new GennyToken(token));
    }

    public static QDataAttributeMessage loadAllAttributesIntoCache(final GennyToken token) {
        try {
            boolean cacheWorked = false;
            println("All the attributes about to become loaded ...");
            JsonObject json = VertxUtils.readCachedJson(token.getRealm(), "attributes", token.getToken());
            if ("ok".equals(json.getString("status"))) {
                //	println("LOADING ATTRIBUTES FROM CACHE!");
                attributesMsg = JsonUtils.fromJson(json.getString("value"), QDataAttributeMessage.class);
                Attribute[] attributeArray = attributesMsg.getItems();

                for (Attribute attribute : attributeArray) {
                    attributeMap.put(attribute.getCode(), attribute);
                }
                println("All the attributes have been loaded in " + attributeMap.size() + " attributes");
            } else {
                println("LOADING ATTRIBUTES FROM API");
                String jsonString = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/attributes", token.getToken());
                if (!StringUtils.isBlank(jsonString)) {
                    VertxUtils.writeCachedJson(token.getRealm(), "attributes", jsonString, token.getToken());

                    attributesMsg = JsonUtils.fromJson(jsonString, QDataAttributeMessage.class);
                    Attribute[] attributeArray = attributesMsg.getItems();

                    for (Attribute attribute : attributeArray) {
                        attributeMap.put(attribute.getCode(), attribute);
                    }
                    println("All the attributes have been loaded from api in" + attributeMap.size() + " attributes");
                } else {
                    log.error("NO ATTRIBUTES LOADED FROM API");
                }
            }

            return attributesMsg;
        } catch (Exception e) {
            log.error("Attributes API not available");
        }
        return null;
    }

    public static Attribute getAttribute(final String attributeCode, final GennyToken token) {
        return getAttribute(attributeCode, token.getToken());
    }

    public static Attribute getAttribute(final String attributeCode, final String token) {
        Attribute ret = attributeMap.get(attributeCode);
        if (ret == null) {
            if (attributeCode.startsWith("SRT_") || attributeCode.startsWith("RAW_")) {
                ret = new AttributeText(attributeCode, attributeCode);
            } else {
                loadAllAttributesIntoCache(token);
                ret = attributeMap.get(attributeCode);
                if (ret == null) {
                    log.error("Attribute NOT FOUND :" + attributeCode);
                }
            }
        }
        return ret;
    }

    public static Map<String, BaseEntity> getRulesFromGit(final String remoteUrl, final String branch, final String realm, final String gitUsername, final String gitPassword,
                                                          boolean recursive, GennyToken userToken)
            throws BadDataException, GitAPIException,
            RevisionSyntaxException, IOException {

        Map<String, BaseEntity> ruleBes = new HashMap<String, BaseEntity>();

        log.info("remoteUrl=" + remoteUrl);
        log.info("branch=" + branch);
        log.info("realm=" + realm);

        String tmpDir = "/tmp/git";
        try {
            File directory = new File(tmpDir);

            // Deletes a directory recursively. When deletion process is fail an
            // IOException is thrown and that's why we catch the exception.
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Git git = Git.cloneRepository()

                .setURI(remoteUrl).setDirectory(new File(tmpDir)).setBranch(branch).setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitPassword)).call();

        log.info("Set up Git");

        git.fetch().setRemote(remoteUrl).setRefSpecs(new RefSpec("+refs/heads/*:refs/heads/*")).setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitPassword)).call();

        Repository repo = git.getRepository();

        /*
         * DfsRepositoryDescription repoDesc = new DfsRepositoryDescription();
         * InMemoryRepository repo = new InMemoryRepository(repoDesc); Git git = new
         * Git(repo); git.fetch() .setRemote(remoteUrl) .setRefSpecs(new
         * RefSpec("+refs/heads/*:refs/heads/*")) .call(); repo.getObjectDatabase();
         */
        // Ref head = repo.getRef("HEAD");
        ObjectId lastCommitId = repo.resolve("refs/heads/" + branch);

        // a RevWalk allows to walk over commits based on some filtering that is defined
        RevWalk walk = new RevWalk(repo);

        RevCommit commit = walk.parseCommit(lastCommitId);
        RevTree tree = commit.getTree();
        log.info("Having tree: " + tree);

        // now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        // treeWalk.setFilter(AndTreeFilter.create(TreeFilter.ANY_DIFF,
        // PathFilter.ANY_DIFF));

//		treeWalk.setFilter(AndTreeFilter.create(PathSuffixFilter.create(".bpmn"),PathSuffixFilter.create(".drl")));
//		treeWalk.setFilter(AndTreeFilter.create(PathFilter.create(realmFilter), PathSuffixFilter.create(".drl")));
        while (treeWalk.next()) {

            final ObjectId objectId = treeWalk.getObjectId(0);
            final ObjectLoader loader = repo.open(objectId);
            FileMode fileMode = treeWalk.getFileMode(0);
            // and then one can the loader to read the file

            String ruleCode = "";
            String fullpath = "";

            fullpath = treeWalk.getPathString(); // .substring(realmFilter.length()+1); // get rid of
            // realm+"-new/sublayouts/"
            Path p = Paths.get(fullpath);
            if ((fullpath.endsWith(".xls") || fullpath.endsWith(".bpmn") || (fullpath.endsWith(".drl"))) && (!p.toString().contains("XXX"))) {

                if (fullpath.equals("rules/rulesCurrent/shared/RULEGROUPS/EventProcessing/LOGOUT_EVENT.drl")) {
                    log.info("logout rule detected!");
                    // continue;
                }

                String filename = p.getFileName().toString();
                String content = new String(loader.getBytes());
                Boolean goodRule = false;
                List<DroolsError> droolsErrors = null;
                DrlParser parser = new DrlParser(LanguageLevelOption.DRL6);
                PackageDescr descr = null;
                try {
                    descr = parser.parse(true, content);

                    if (!parser.hasErrors()) {
                        goodRule = true;
                    } else {
                        droolsErrors = parser.getErrors();
                    }
                } catch (DroolsParserException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if ((goodRule) || (!fullpath.endsWith(".drl"))) {
                    System.out.println(realm + ":" + p.toString());
                    if ((descr.getRules().size() == 1) || ((!fullpath.endsWith(".drl")))) {
                        String beName = filename;
                        BaseEntity ruleBe = null;


                        if (fullpath.endsWith(".drl")) {
                            RuleDescr rule = descr.getRules().get(0);
                            beName = rule.getName();
                            ruleBe = new BaseEntity("RUL_" + beName.toUpperCase(), beName);

                            if (rule.getAttributes().containsKey("ruleflow-group")) {
                                AttributeDescr attD = rule.getAttributes().get("ruleflow-group");
                                String ruleflowgroup = attD.getValue();
                                ruleBe.setValue(RulesUtils.getAttribute("PRI_KIE_RULE_GROUP", userToken.getToken()), ruleflowgroup);
                            }
                            if (rule.getAttributes().containsKey("no-loop")) {
                                AttributeDescr attD = rule.getAttributes().get("no-loop");
                                String noloop = attD.getValue();
                                ruleBe.setValue(RulesUtils.getAttribute("PRI_KIE_RULE_NOLOOP", userToken.getToken()), noloop);
                            }
                            if (rule.getAttributes().containsKey("salience")) {
                                AttributeDescr attD = rule.getAttributes().get("salience");
                                String salience = attD.getValue();
                                ruleBe.setValue(RulesUtils.getAttribute("PRI_KIE_RULE_SALIENCE", userToken.getToken()), salience);
                            }

                        } else {
                            ruleBe = new BaseEntity("RUL_" + beName.toUpperCase(), beName);
                        }
                        Integer hashcode = content.hashCode();

                        long secs = commit.getCommitTime();
                        LocalDateTime commitDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(secs * 1000),
                                TimeZone.getDefault().toZoneId());
                        String lastCommitDateTimeString = commitDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        ruleBe.addAnswer(new Answer(ruleBe, ruleBe,
                                new AttributeText("PRI_COMMIT_DATETIME", "Commited"), lastCommitDateTimeString)); // if
                        // new
                        ruleBe.setRealm(realm);
                        ruleBe.setUpdated(commitDateTime);

                        ruleBe.setValue(RulesUtils.getAttribute("PRI_HASHCODE", userToken.getToken()), hashcode);
                        ruleBe.setValue(RulesUtils.getAttribute("PRI_FILENAME", userToken.getToken()), filename);

                        String ext = filename.substring(filename.lastIndexOf(".") + 1);
                        String kieType = ext.toUpperCase();
                        ruleBe.setValue(RulesUtils.getAttribute("PRI_KIE_TYPE", userToken.getToken()), kieType);

                        ruleBe.setValue(RulesUtils.getAttribute("PRI_KIE_TEXT", userToken.getToken()), content);
                        ruleBe.setValue(RulesUtils.getAttribute("PRI_KIE_NAME", userToken.getToken()), beName);

                        ruleBe.setValue(RulesUtils.getAttribute("PRI_BRANCH", userToken.getToken()), branch);

                        if (ruleBe.getCode().startsWith("RUL_FRM_")) {
                            ruleBe.setValue(RulesUtils.getAttribute("PRI_ASKS", userToken.getToken()), "EMPTY");  // initialise empty pojo
                            ruleBe.setValue(RulesUtils.getAttribute("PRI_MSG", userToken.getToken()), "EMPTY");  // initialise empty msg
                        }
                        if ((ruleBe.getCode().startsWith("RUL_FRM_")) || (ruleBe.getCode().startsWith("RUL_THM_"))) {
                            ruleBe.setValue(RulesUtils.getAttribute("PRI_POJO", userToken.getToken()), "EMPTY");  // initialise empty pojo
                        }
                        ruleBes.put("RUL_" + beName.toUpperCase(), ruleBe);
                    } else {
                        System.out.println("!!!!!!!!!!!!!!!!!!! ERROR: MUST HAVE ONLY 1 RULE PER FILE " + p.toString());
                    }
                } else {
                    System.out.println("!!!!!!!!!!!!!!!!!!! ERROOR: BAD RULE: " + p.toString());
                    System.out.println(droolsErrors);
                }

            }

        }

        return ruleBes;

    }

    public static String generateServiceToken(String realm, String token) {

        /* we get the service token currently stored in the cache */
        String serviceToken = VertxUtils.getObject(realm, "CACHE", "SERVICE_TOKEN", String.class, token);

        return serviceToken;

    }

    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @return baseEntity user for the decodedToken passed
     */
    public static String getBaseEntityJsonByAttributeAndValue(final String qwandaServiceUrl,
                                                              Map<String, Object> decodedToken, final String token, final String attributeCode, final String value) {

        return getBaseEntityJsonByAttributeAndValue(qwandaServiceUrl, decodedToken, token, attributeCode, value, 1);

    }

    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @return baseEntity user for the decodedToken passed
     */
    public static String getBaseEntityJsonByAttributeAndValue(final String qwandaServiceUrl,
                                                              Map<String, Object> decodedToken, final String token, final String attributeCode, final String value,
                                                              final Integer pageSize) {

        try {
            String beJson = null;
            beJson = QwandaUtils.apiGet(qwandaServiceUrl + "/qwanda/baseentitys/test2?pageSize=" + pageSize + "&"
                    + attributeCode + "=" + value, token);
            // println("BE"+beJson);
            return beJson;

        } catch (IOException e) {
            log.error("Error in fetching Base Entity from Qwanda Service");
        }
        return null;

    }

    public static <T> T fromJson(final String json, Class clazz) {
        return JsonUtils.fromJson(json, clazz);
    }


    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @return baseEntity user for the decodedToken passed
     */
    public static List<BaseEntity> getBaseEntitysByAttributeAndValue(final String qwandaServiceUrl,
                                                                     Map<String, Object> decodedToken, final String token, final String attributeCode, final String value) {

        String beJson = getBaseEntityJsonByAttributeAndValue(qwandaServiceUrl, decodedToken, token, attributeCode,
                value, 1000);
        if (StringUtils.isBlank(beJson)) {
            return new ArrayList<BaseEntity>();
        }

        QDataBaseEntityMessage be = fromJson(beJson, QDataBaseEntityMessage.class);

        List<BaseEntity> items = null;

        try {
            items = new ArrayList<BaseEntity>(Arrays.asList(be.getItems()));
        } catch (Exception e) {
            println("Warning: items is null: (json=" + beJson + ")");
        }

        return items;

    }


    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @return baseEntity user for the decodedToken passed
     */
    public static BaseEntity getBaseEntityByAttributeAndValue(final String qwandaServiceUrl,
                                                              Map<String, Object> decodedToken, final String token, final String attributeCode, final String value) {

        List<BaseEntity> items = getBaseEntitysByAttributeAndValue(qwandaServiceUrl, decodedToken, token, attributeCode,
                value);

        if ((items != null) && (items.size() > 0)) {
            if (!items.isEmpty())
                return items.get(0);
        }

        return null;

    }

    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @param parentCode
     * @param linkCode
     * @return baseEntitys
     */
    public static List<BaseEntity> getBaseEntitysByParentAndLinkCodeWithAttributes(final String qwandaServiceUrl,
                                                                                   Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode) {

        BaseEntity[] beArray = getBaseEntitysArrayByParentAndLinkCodeWithAttributes(qwandaServiceUrl, decodedToken,
                token, parentCode, linkCode);
        ArrayList<BaseEntity> arrayList = new ArrayList<BaseEntity>(Arrays.asList(beArray));
        return arrayList;

    }

    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @param parentCode
     * @param linkCode
     * @param pageStart
     * @param pageSize
     * @return baseEntitys
     */
    public static List<BaseEntity> getBaseEntitysByParentAndLinkCodeWithAttributes(final String qwandaServiceUrl,
                                                                                   Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                                                   final Integer pageStart, final Integer pageSize) {

        BaseEntity[] beArray = getBaseEntitysArrayByParentAndLinkCodeWithAttributes(qwandaServiceUrl, decodedToken,
                token, parentCode, linkCode, pageStart, pageSize);
        ArrayList<BaseEntity> arrayList = new ArrayList<BaseEntity>(Arrays.asList(beArray));
        return arrayList;

    }

    public static BaseEntity[] getBaseEntitysArrayByParentAndLinkCodeWithAttributes(final String qwandaServiceUrl,
                                                                                    Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                                                    final Integer pageStart, final Integer pageSize) {

        String beJson = getBaseEntitysJsonByParentAndLinkCode(qwandaServiceUrl, decodedToken, token, parentCode,
                linkCode, pageStart, pageSize);

        QDataBaseEntityMessage msg = null;
        if (StringUtils.isBlank(beJson)) {
            BaseEntity[] beArray = new BaseEntity[0];
            msg = new QDataBaseEntityMessage(beArray);

        } else {
            msg = JsonUtils.fromJson(beJson, QDataBaseEntityMessage.class);
        }
        if (msg == null) {
            log.error("No BaseEntitys found for parentCode:" + parentCode + " and linkCode:" + linkCode);
            return new BaseEntity[0];
        }
        return msg.getItems();

    }

    public static String getBaseEntitysJsonByParentAndLinkCode(final String qwandaServiceUrl,
                                                               Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                               final Integer pageStart, final Integer pageSize) {

        try {
            String beJson = null;
            beJson = QwandaUtils.apiGet(qwandaServiceUrl + "/qwanda/baseentitys/" + parentCode + "/linkcodes/"
                    + linkCode + "/attributes?pageStart=" + pageStart + "&pageSize=" + pageSize, token);
            return beJson;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @param parentCode
     * @param linkCode
     * @return baseEntitys
     */
    public static BaseEntity[] getBaseEntitysArrayByParentAndLinkCodeWithAttributes(final String qwandaServiceUrl,
                                                                                    Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode) {

        String beJson = getBaseEntitysJsonByParentAndLinkCode(qwandaServiceUrl, decodedToken, token, parentCode,
                linkCode);
        QDataBaseEntityMessage msg = JsonUtils.fromJson(beJson, QDataBaseEntityMessage.class);
        return msg.getItems();
    }

    public static String getBaseEntitysJsonByParentAndLinkCode(final String qwandaServiceUrl,
                                                               Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode) {

        try {
            String beJson = null;
            beJson = QwandaUtils.apiGet(
                    qwandaServiceUrl + "/qwanda/baseentitys/" + parentCode + "/linkcodes/" + linkCode + "/attributes",
                    token);
            return beJson;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @param parentCode
     * @param linkCode
     * @param pageStart
     * @param pageSize
     * @return baseEntitys
     */

    /* added because of bug */
    public static List<BaseEntity> getBaseEntitysByParentAndLinkCodeWithAttributes2(final String qwandaServiceUrl,
                                                                                    Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                                                    final Integer pageStart, final Integer pageSize) {

        BaseEntity[] beArray = getBaseEntitysArrayByParentAndLinkCodeWithAttributes2(qwandaServiceUrl, decodedToken,
                token, parentCode, linkCode, pageStart, pageSize);
        ArrayList<BaseEntity> arrayList = new ArrayList<BaseEntity>(Arrays.asList(beArray));
        return arrayList;

    }

    /**
     * added because of bug / /**
     *
     * @param qwandaServiceUrl
     * @param decodedToken
     * @param token
     * @param parentCode
     * @param linkCode
     * @param pageStart
     * @param pageSize
     * @return baseEntitys
     */
    public static BaseEntity[] getBaseEntitysArrayByParentAndLinkCodeWithAttributes2(final String qwandaServiceUrl,
                                                                                     Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                                                     final Integer pageStart, final Integer pageSize) {

        String beJson = getBaseEntitysJsonByParentAndLinkCode2(qwandaServiceUrl, decodedToken, token, parentCode,
                linkCode, pageStart, pageSize);
        QDataBaseEntityMessage msg = JsonUtils.fromJson(beJson, QDataBaseEntityMessage.class);
        return msg.getItems();

    }

    /* added because of the bug */
    public static String getBaseEntitysJsonByParentAndLinkCode2(final String qwandaServiceUrl,
                                                                Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                                final Integer pageStart, final Integer pageSize) {

        try {
            String beJson = null;
            beJson = QwandaUtils.apiGet(qwandaServiceUrl + "/qwanda/baseentitys/" + parentCode + "/linkcodes/"
                    + linkCode + "?pageStart=" + pageStart + "&pageSize=" + pageSize, token);
            return beJson;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static List<BaseEntity> getBaseEntitysByParentAndLinkCodeAndLinkValueWithAttributes(
            final String qwandaServiceUrl, Map<String, Object> decodedToken, final String token,
            final String parentCode, final String linkCode, final String linkValue, final Integer pageStart,
            final Integer pageSize) {

        BaseEntity[] beArray = getBaseEntitysArrayByParentAndLinkCodeAndLinkValueWithAttributes(qwandaServiceUrl,
                decodedToken, token, parentCode, linkCode, linkValue, pageStart, pageSize);
        ArrayList<BaseEntity> arrayList = new ArrayList<BaseEntity>(Arrays.asList(beArray));
        return arrayList;
    }

    public static BaseEntity[] getBaseEntitysArrayByParentAndLinkCodeAndLinkValueWithAttributes(
            final String qwandaServiceUrl, Map<String, Object> decodedToken, final String token,
            final String parentCode, final String linkCode, final String linkValue, final Integer pageStart,
            final Integer pageSize) {

        String beJson = getBaseEntitysJsonByParentAndLinkCodeAndLinkValue(qwandaServiceUrl, decodedToken, token,
                parentCode, linkCode, linkValue, pageStart, pageSize);
        QDataBaseEntityMessage msg = JsonUtils.fromJson(beJson, QDataBaseEntityMessage.class);
        return msg.getItems();

    }

    public static String getBaseEntitysJsonByParentAndLinkCodeAndLinkValue(final String qwandaServiceUrl,
                                                                           Map<String, Object> decodedToken, final String token, final String parentCode, final String linkCode,
                                                                           final String linkValue, final Integer pageStart, final Integer pageSize) {

        try {
            String beJson = null;
            beJson = QwandaUtils.apiGet(
                    qwandaServiceUrl + "/qwanda/baseentitys2/" + parentCode + "/linkcodes/" + linkCode + "/linkValue/"
                            + linkValue + "/attributes?pageStart=" + pageStart + "&pageSize=" + pageSize,
                    token);
            return beJson;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static List<BaseEntity> getBaseEntitysByParentAndLinkCodeWithAttributesAndStakeholderCode(
            final String qwandaServiceUrl, Map<String, Object> decodedToken, final String token,
            final String parentCode, final String linkCode, final String stakeholderCode) {
        if (parentCode.equalsIgnoreCase("GRP_NEW_ITEMS")) {
            println("Group New Items Debug");
        }
        println("stakeholderCode is :: " + stakeholderCode);
        String beJson = getBaseEntitysJsonByParentAndLinkCodeWithAttributesAndStakeholderCode(qwandaServiceUrl,
                decodedToken, token, parentCode, linkCode, stakeholderCode);
        QDataBaseEntityMessage msg = fromJson(beJson, QDataBaseEntityMessage.class);
        if (msg == null) {
            log.error("Error in fetching BE from Qwanda Service");
        } else {
            BaseEntity[] beArray = msg.getItems();
            ArrayList<BaseEntity> arrayList = new ArrayList<BaseEntity>(Arrays.asList(beArray));
            return arrayList;
        }
        return null; // TODO get exception =s in place

    }

    public static String getBaseEntitysJsonByParentAndLinkCodeWithAttributesAndStakeholderCode(
            final String qwandaServiceUrl, Map<String, Object> decodedToken, final String token,
            final String parentCode, final String linkCode, final String stakeholderCode) {

        try {
            String beJson = null;
            log.info("stakeholderCode is :: " + stakeholderCode);
            beJson = QwandaUtils.apiGet(qwandaServiceUrl + "/qwanda/baseentitys/" + parentCode + "/linkcodes/"
                    + linkCode + "/attributes/" + stakeholderCode, token);
            return beJson;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    public static List<Link> getLinks(final String qwandaServiceUrl, Map<String, Object> decodedToken,
                                      final String token, final String parentCode, final String linkCode) {

        String linkJson = null;
        List<Link> linkList = null;

        try {

            linkJson = QwandaUtils.apiGet(
                    qwandaServiceUrl + "/qwanda/entityentitys/" + parentCode + "/linkcodes/" + linkCode + "/children",
                    token);
            return JsonUtils.fromJson(linkJson, new TypeToken<List<Link>>() {
            }.getType());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // BaseEntity[] beArray = msg.getItems();
        // ArrayList<BaseEntity> arrayList = new
        // ArrayList<BaseEntity>(Arrays.asList(beArray));
        // return arrayList;
        return linkList;
    }

    public static String jsonLogger(String module, Object data) {
        String initialLogger = "------------------------------------------------------------------------\n";
        String moduleLogger = ANSI_YELLOW + module + "   ::   " + ANSI_RESET + data + "\n";
        String finalLogger = "------------------------------------------------------------------------\n";
        return initialLogger + moduleLogger + finalLogger;
    }

    public static void ruleLogger(String module, Object data) {
        println(jsonLogger(module, data));
    }


    public static String getLayout(String realm, String path) {

        String jsonStr = "";
        String finalPath = "";
        try {

            if (path.startsWith("/") == false && realm.endsWith("/") == false) {
                finalPath = realm + "/" + path;
            } else {
                finalPath = realm + path;
            }

            String url = getLayoutCacheURL(realm, finalPath);
            println("Trying to load url.....");
            println(url);

            /* we make a GET request */
            jsonStr = QwandaUtils.apiGet(url, null);

            if (jsonStr != null) {

                /* we serialise the layout into a JsonObject */
                JsonObject layoutObject = new JsonObject(jsonStr);
                if (layoutObject != null) {

                    /* we check if an error happened when grabbing the layout */
                    if ((layoutObject.containsKey("Error") || layoutObject.containsKey("error"))
                            && realm.equals("genny") == false) {

                        /* we try to grab the layout using the genny realm */
                        return RulesUtils.getLayout("genny", path);
                    } else {

                        /* otherwise we return the layout */
                        return jsonStr;
                    }
                }
            }
        } catch (Exception e) {
            log.info(jsonStr);
            return jsonStr;
        }

        return null;
    }

    public static String getLayoutCacheURL(String realm, final String path) {

        String host = GennySettings.layoutCacheUrl;

        return String.format("%s/%s", host, path);
    }
}
