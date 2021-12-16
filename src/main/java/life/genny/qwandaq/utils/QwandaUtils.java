package life.genny.qwandaq.utils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.GennySettings;

public class QwandaUtils implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(QwandaUtils.class);

    static public Map<String,Map<String, Attribute>> attributes = new ConcurrentHashMap<>();
    static public Map<String,Map<String,BaseEntity>> defs = new ConcurrentHashMap<>();

	static Jsonb jsonb = JsonbBuilder.create();

    public static Attribute getAttribute(final String attributeCode, GennyToken gennyToken) {

    	String realm = gennyToken.getRealm();

    	if (attributes.get(gennyToken.getRealm()) == null) {
    		loadAllAttributes(gennyToken);
    	}

        Attribute attribute = attributes.get(realm).get(attributeCode);

		if (attribute == null) {
			log.error("Bad Attribute in Map for realm " +realm + " and code " + attributeCode);
		}

        return attribute;
    }

	public static void loadAllAttributes(GennyToken gennyToken) {

		String realm = gennyToken.getRealm();

		log.info("About to load all attributes for realm " + realm);

		List<Attribute> attributeList = fetchAttributesFromDB();

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
	* Fetch all attributes from the database
	*
	* @param code
	* @return	The corresponding BaseEntity, or null if not found.
	 */
	public static List<Attribute> fetchAttributesFromDB() {

		String uri = GennySettings.fyodorServiceUrl + "/api/attributes";

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.GET().build();

		String body = null;
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			body = response.body();
		} catch (IOException | InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}

		if (body != null) {

			try {

				List<Attribute> attributeList = jsonb.fromJson(body, new ArrayList<Attribute>(){}.getClass().getGenericSuperclass());

				return attributeList;

			} catch (Exception e) {
				log.error(e.getStackTrace());
			}
		}

		return null;
	}



}
