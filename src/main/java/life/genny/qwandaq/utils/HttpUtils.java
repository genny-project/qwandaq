package life.genny.qwandaq.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class HttpUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	/**
	* Create and send a PUT request.
	*
	* @param uri
	* @param body
	* @param token
	* @return
	 */
	public static HttpResponse<String> put(String uri, String body, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + token)
			.PUT(HttpRequest.BodyPublishers.ofString(body))
			.build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}

	/**
	* Create and send a POST request.
	*
	* @param uri
	* @param body
	* @param token
	* @return
	 */
	public static HttpResponse<String> post(String uri, String body, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + token)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}

	/**
	* Create and send a GET request.
	*
	* @param uri
	* @param token
	* @return
	 */
	public static HttpResponse<String> get(String uri, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder() .uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + token)
			.GET().build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}

	/**
	* Create and send a DELETE request.
	*
	* @param uri
	* @param token
	* @return
	 */
	public static HttpResponse<String> delete(String uri, String token) {

		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder() .uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + token)
			.DELETE().build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response;
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}

		return null;
	}



	/**
	* Build an error message json string from a msg string.
	*
	* @param msg
	* @return
	 */
	public static String errorBody(String msg) {

		JsonObject json = Json.createObjectBuilder()
			.add("error", msg)
			.build();

		return jsonb.toJson(json);
	}
}
