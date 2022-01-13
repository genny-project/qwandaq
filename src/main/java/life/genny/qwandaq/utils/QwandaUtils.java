package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.io.IOException;
import java.io.Serializable;
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
public class QwandaUtils implements Serializable {

	private static final Logger log = Logger.getLogger(QwandaUtils.class);

    private Map<String, Map<String, Attribute>> attributes = new ConcurrentHashMap<>();

	private static Jsonb jsonb = JsonbBuilder.create();
	private GennyToken gennyToken;

	public QwandaUtils() {}

	public QwandaUtils(GennyToken gennyToken) {
		this.gennyToken = gennyToken;
		loadAllAttributes();
	}

	/**
	* Get an attribute from the in memory attribute map. If realm not found, it 
	* will try to fetch attributes from the DB.
	*
	* @param attributeCode
	* @param gennyToken
	* @return
	 */
    public Attribute getAttribute(final String attributeCode) {

    	String realm = this.gennyToken.getRealm();

    	if (this.attributes.get(gennyToken.getRealm()) == null) {
    		loadAllAttributes();
    	}

        Attribute attribute = this.attributes.get(realm).get(attributeCode);

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
	public void loadAllAttributes() {

		String realm = this.gennyToken.getRealm();

		log.info("About to load all attributes for realm " + realm);

		List<Attribute> attributeList = fetchAttributesFromDB();

		if (attributeList == null) {
			log.error("Null attributeList, not putting in map!!!");
			return;
		}

		// Check for existing map
		if (!this.attributes.containsKey(realm)) {
			this.attributes.put(realm, new ConcurrentHashMap<String,Attribute>());
		}
		Map<String,Attribute> attributeMap = this.attributes.get(realm);

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



}
