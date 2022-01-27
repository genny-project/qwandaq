package life.genny.qwandaq.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.models.GennyToken;

public class QwandaUtils {

	static final Logger log = Logger.getLogger(QwandaUtils.class);

    static Map<String, Map<String, Attribute>> attributes = new ConcurrentHashMap<>();

	static Jsonb jsonb = JsonbBuilder.create();

	static GennyToken gennyToken;

	public static void init(GennyToken token) {
		gennyToken = token;
		loadAllAttributes();
	}

	public static void init(GennyToken token, List<Attribute> attributeList) {
		gennyToken = token;

		attributes.put(token.getRealm(), new ConcurrentHashMap<String,Attribute>());
		Map<String,Attribute> attributeMap = attributes.get(token.getRealm());

		for (Attribute attribute : attributeList) {
			attributeMap.put(attribute.getCode(), attribute);
		}
	}

	/**
	* Get an attribute from the in memory attribute map. If realm not found, it 
	* will try to fetch attributes from the DB.
	*
	* @param attributeCode
	* @param gennyToken
	* @return
	 */
    public static Attribute getAttribute(final String attributeCode) {

    	String realm = gennyToken.getRealm();

    	if (attributes.get(gennyToken.getRealm()) == null) {
    		loadAllAttributes();
    	}

        Attribute attribute = attributes.get(realm).get(attributeCode);

		if (attribute == null) {
			log.error("Bad Attribute in Map for realm " +realm + " and code " + attributeCode);
		}

        return attribute;
    }

	/**
	* Load all attributes into the in memory map.
	*
	* @return
	 */
	public static void loadAllAttributes() {

		String realm = gennyToken.getRealm();

		log.info("About to load all attributes for realm " + realm);

		List<Attribute> attributeList = DatabaseUtils.fetchAttributesFromDB(realm);

		if (attributeList == null) {
			log.error("Null attributeList, not putting in map!!!");
			return;
		}

		// Check for existing map
		if (!attributes.containsKey(realm)) {
			attributes.put(realm, new ConcurrentHashMap<String,Attribute>());
		}
		Map<String,Attribute> attributeMap = attributes.get(realm);

		// Insert attributes into map
		for (Attribute attribute : attributeList) {
			attributeMap.put(attribute.getCode(), attribute);
		}

		log.info("All attributes have been loaded: " + attributeMap.size() + " attributes");
    }

	/**
	* Remove an atttribute from the in memory set using the code.
	*
	* @param code	Code of the attribute to remove.
	 */
	public static void removeAttributeFromMemory(String code) {

    	String realm = gennyToken.getRealm();
        attributes.get(realm).remove(code);
	}

}
