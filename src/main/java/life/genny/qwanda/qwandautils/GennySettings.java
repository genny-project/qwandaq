package life.genny.qwanda.qwandautils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class GennySettings {
    //Constants
    public final static String LOCALHOST = "localhost";
    public final static String DEFAULT_CACHE_SERVER_NAME = "bridge-service";
    public final static String GENNY_REALM = "jenny"; //deliberatly not genny
    public static int ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS = 60;
    public static String hostIP = System.getenv("HOSTIP") != null ? System.getenv("HOSTIP") : System.getenv("MYIP");   // remember to set up this local IP on the host
    public static String cacheApiPort = System.getenv("CACHE_API_PORT") != null ? System.getenv("CACHE_API_PORT") : "8089";
    public static String apiPort = System.getenv("API_PORT") != null ? System.getenv("API_PORT") : "8088";
    public static String pontoonPort = System.getenv("PONTOON_PORT") != null ? System.getenv("PONTOON_PORT") : "8086";

    public static final String projectUrl = System.getenv("PROJECT_URL") != null ? System.getenv("PROJECT_URL") : "http://alyson7.genny.life";
    public static final String qwandaServiceUrl = System.getenv("REACT_APP_QWANDA_API_URL") != null ? System.getenv("REACT_APP_QWANDA_API_URL") : (projectUrl + ":8280");
    public static final String bridgeServiceUrl = System.getenv("BRIDGE_SERVICE_API") != null ? System.getenv("BRIDGE_SERVICE_API") : projectUrl + "/api/service";
    public static final String pontoonUrl = System.getenv("PONTOON_URL") != null ? System.getenv("PONTOON_URL") : "http://" + hostIP + ":" + pontoonPort;
    public static final Boolean devMode = "TRUE".equalsIgnoreCase(System.getenv("DEV_MODE")) || "TRUE".equalsIgnoreCase(System.getenv("GENNYDEV"));
    public static final Boolean gzipMode = "TRUE".equalsIgnoreCase(System.getenv("MODE_GZIP"));
    public static final Boolean gzip64Mode = "TRUE".equalsIgnoreCase(System.getenv("MODE_GZIP64"));
    public static final Boolean multiBridgeMode = "TRUE".equalsIgnoreCase(System.getenv("MULTI_BRIDGE_MODE"));
    public static final Boolean bulkPull = "TRUE".equalsIgnoreCase(System.getenv("BULKPULL"));
    public static final Long pontoonMinimumThresholdBytes = System.getenv("PONTOON_MIN_THRESHOLD_BYTES") == null ? 10000L : (Integer.parseInt(System.getenv("PONTOON_MIN_THRESHOLD_BYTES")));
    public static final Integer pontoonTimeout = System.getenv("PONTOON_TIMEOUT") == null ? 43200 : (Integer.parseInt(System.getenv("PONTOON_TIMEOUT")));  // 12 hours

    // 2^19-1 = 524287 2^23-1=8388607
    public final static Boolean isDdtHost = "TRUE".equalsIgnoreCase(System.getenv("DDTHOST"));
    public static Boolean forceEventBusApi = System.getenv("FORCE_EVENTBUS_USE_API") != null && "TRUE".equalsIgnoreCase(System.getenv("FORCE_EVENTBUS_USE_API"));
    public final static Boolean noCache = System.getenv("NO_CACHE") != null && "TRUE".equalsIgnoreCase(System.getenv("NO_CACHE"));
    public static Boolean forceCacheApi = System.getenv("FORCE_CACHE_USE_API") != null && "TRUE".equalsIgnoreCase(System.getenv("FORCE_CACHE_USE_API"));
    public final static Boolean disableLayoutLoading = "TRUE".equalsIgnoreCase(System.getenv("DISABLE_LAYOUT_LOADING"));
    public final static Boolean useJMS = System.getenv("USE_JMS") != null && "TRUE".equalsIgnoreCase(System.getenv("USE_JMS"));
    public static final Boolean searchAlt = !"FALSE".equalsIgnoreCase(System.getenv("SEARCH_ALT"));
    public static final String ddtUrl = System.getenv("DDT_URL") == null ? ("http://" + hostIP + ":" + cacheApiPort) : System.getenv("DDT_URL");

    public final static String gitProjectUrls = System.getenv("GIT_PROJECT_URLS") == null ? ("https://github.com/genny-project/prj_genny.git")
            : System.getenv("GIT_PROJECT_URLS"); // comma separated urls, in order of loading with subsequent overwriting

    public final static String gitPassword = System.getenv("GIT_PASSWORD") == null ? "" : System.getenv("GIT_PASSWORD");
    public final static String gitUsername = System.getenv("GIT_USERNAME") == null ? "git" : System.getenv("GIT_USERNAME");
//    public final static String gitRulesBranch = System.getenv("GIT_RULES_BRANCH") == null ? "v3.1.0" : System.getenv("GIT_RULES_BRANCH");

    // This is public
    public final static String gennyPublicUsername = System.getenv("USERNAME") == null ? "user1" : System.getenv("USERNAME");
    public static final String username = System.getenv("USER") == null ? "GENNY" : System.getenv("USER");

    public static final String defaultServiceKey = System.getenv("ENV_SECURITY_KEY") == null ? "WubbaLubbaDubDub" : System.getenv("ENV_SECURITY_KEY");
    public static final String defaultServiceEncryptedPassword = System.getenv("ENV_SERVICE_PASSWORD") == null ? "vRO+tCumKcZ9XbPWDcAXpU7tcSltpNpktHcgzRkxj8o=" : System.getenv("ENV_SERVICE_PASSWORD");
    public static final String defaultServicePassword = System.getenv("DENV_SERVICE_UNENCRYPTED_PASSWORD") == null ? "Wubba!Lubba!Dub!Dub!" : System.getenv("ENV_SERVICE_UNENCRYPTED_PASSWORD");

    public static final String realmDir = System.getenv("REALM_DIR") != null ? System.getenv("REALM_DIR") : "./realm";
    public static final String rulesDir = System.getenv("RULES_DIR") != null ? System.getenv("RULES_DIR") : "/rules";  // TODO, docker focused


    public static final String layoutCacheUrl = System.getenv("LAYOUT_CACHE_HOST") != null ? System.getenv("LAYOUT_CACHE_HOST") : "http://" + hostIP + ":2223";

    public static final String cacheServerName;
    public static final Boolean isCacheServer;
    public static final Integer defaultPageSize = System.getenv("DEFAULT_PAGE_SIZE") == null ? 20 : (Integer.parseInt(System.getenv("DEFAULT_PAGE_SIZE")));
    public static final Integer defaultDropDownPageSize = System.getenv("DEFAULT_DROPDOWN_PAGE_SIZE") == null ? 10000 : (Integer.parseInt(System.getenv("DEFAULT_DROPDOWN_PAGE_SIZE")));

    public static final String emailSmtpPassword = System.getenv("EMAIL_SMTP_PASS") != null ? System.getenv("EMAIL_SMTP_PASS") : "http://keycloak.genny.life";
    public static final String emailSmtpStartTls = System.getenv("EMAIL_SMTP_STARTTLS") != null ? System.getenv("EMAIL_SMTP_STARTTLS") : "http://keycloak.genny.life";

    public static final String twilioSenderMobile = System.getenv("TWILIO_SENDER_MOBILE") != null ? System.getenv("TWILIO_SENDER_MOBILE") : "TWILIO_SENDER_MOBILE";

    static {
        Optional<String> cacheServerNameOptional = Optional.ofNullable(System.getenv("CACHE_SERVER_NAME"));
        Optional<String> isCacheServerOptional = Optional.ofNullable(System.getenv("IS_CACHE_SERVER"));
        if (devMode) {
            cacheServerName = cacheServerNameOptional.orElse(LOCALHOST);
        } else {
            cacheServerName = cacheServerNameOptional.orElse(DEFAULT_CACHE_SERVER_NAME);
        }
        isCacheServer = isCacheServerOptional.map(Boolean::parseBoolean).orElse(false);
    }


    public static String dynamicEncryptedPassword(final String realm) {
        String envServiceEncryptedPassword = System.getenv("ENV_SERVICE_PASSWORD" + "_" + realm.toUpperCase());
        if (envServiceEncryptedPassword == null) {
            return defaultServiceEncryptedPassword;
        } else {
            return envServiceEncryptedPassword;
        }
    }

    public static String dynamicInitVector(final String realm) {
        String initVector = "PRJ_" + realm.toUpperCase();
        initVector = StringUtils.rightPad(initVector, 16, '*');
        return initVector;
    }
}
