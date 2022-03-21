package life.genny.qwandaq.utils;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.intf.KafkaInterface;
import life.genny.qwandaq.utils.HttpUtils;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
 * A static utility class used for standard 
 * Kogito interactions
 * 
 * @author Adam Crow
 */
@ApplicationScoped
public class KogitoUtils implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(KogitoUtils.class);
	private static Jsonb jsonb = JsonbBuilder.create();

	public String fetchGraphQL(final String graphTable, final String likeField, final String likeValue,
			final String token, String... fields) {
		String data = " query {"
				+ "  " + graphTable + " (where: {"
				+ "      " + likeField + ": {"
				+ " like: \"" + likeValue + "\" }}) {";
		for (String field : fields) {
			data += "   " + field;
		}
		data += "  }"
				+ "}";
		String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
		log.info("graphQL url=" + graphQlUrl);
		HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL", token);
		return response.body();
	}

	public String fetchProcessId(final String graphTable, final String likeField, final String likeValue,
			final String token) throws Exception {
		String idStr = null;
		String data = " query {"
				+ "  " + graphTable + " (where: {"
				+ "      " + likeField + ": {"
				+ " like: \"" + likeValue + "\" }}) {";
		data += "   id";
		data += "  }"
				+ "}";
		String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
		log.info("graphQL url=" + graphQlUrl);
		HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL", token);
		String responseBody = response.body();
		// isolate the id
		JsonObject responseJson = jsonb.fromJson(responseBody, JsonObject.class);
		log.info(responseJson);
		JsonObject json = responseJson.getJsonObject("data");
		JsonArray jsonArray = json.getJsonArray(graphTable);
		if (!jsonArray.isEmpty()) {
			JsonObject firstItem = jsonArray.getJsonObject(0);
			idStr = firstItem.getString("id");

		} else {
			throw new Exception("No processId found");
		}
		return idStr;

	}

	public String sendSignal(final String graphTable, final String processId, final String signalCode, String token) {
		// http://alyson2.genny.life:${port}/travels/${id}/${abortCode}
		String kogitoUrl = System.getenv("GENNY_KOGITO_SERVICE_URL") + "/" + graphTable.toLowerCase() + "/" + processId
				+ "/" + signalCode;
		log.info("signal endpoint url=" + kogitoUrl);
		HttpResponse<String> response = HttpUtils.post(kogitoUrl, "", "application/json", token);
		String responseBody = response.body();
		return responseBody;
	}

}
