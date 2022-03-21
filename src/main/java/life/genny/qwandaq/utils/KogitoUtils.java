package life.genny.qwandaq.utils;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
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

	public String fetchGraphQL(final String token) {
		String data = " query {"
				+ "  Application (where: {"
				+ "      internCode: {"
				+ " like: \"PER_A%\" }}) {"
				+ "   id"
				+ "   internCode"
				+ "   agentCode"
				+ "  }"
				+ "}";
		String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
		log.info("graphQL url=" + graphQlUrl);
		HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL", token);
		return response.body();
	}

}
