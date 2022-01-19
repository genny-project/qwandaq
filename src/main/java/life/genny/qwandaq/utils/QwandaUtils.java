package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.GennySettings;

@RegisterForReflection
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
	* @return	All {@link Attribute} objects found in the DB
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

	/**
	* Remove an atttribute from the in memory set using the code.
	*
	* @param code	Code of the attribute to remove.
	 */
	public static void removeAttributeFromMemory(String code) {

    	String realm = gennyToken.getRealm();
        attributes.get(realm).remove(code);
	}

	/**
	* Delete an atttribute from the database.
	*
	* @param code	Code of the attribute to delete.
	 */
	public static void deleteAttribute(String code) {

		String uri = GennySettings.fyodorServiceUrl + "/api/attribute/" + code;

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.DELETE().build();

		HttpResponse<String> response = null;

		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		if (response != null) {
			if (response.statusCode() != 201) {
				log.error("Could not delete attribute " + code);
			}
		}
	}

	/**
	* Save an attribute to the database.
	*
	* @param attribute	An {@link Attribute} object to save
	 */
	public static void saveAttribute(Attribute attribute) {

		String uri = GennySettings.fyodorServiceUrl + "/api/attributes";
		String json = jsonb.toJson(attribute);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + gennyToken.getToken())
			.POST(HttpRequest.BodyPublishers.ofString(json))
			.build();

		HttpResponse<String> response = null;

		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}

		if (response != null) {
			if (response.statusCode() != 201) {
				log.error("Could not save attribute " + attribute.getCode());
			}
		}
	}
}
