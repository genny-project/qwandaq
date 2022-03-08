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

/**
 * A Static utility class for standard HTTP requests.
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
public class HttpUtils {

	static final Logger log = Logger.getLogger(CacheUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	/**
	* Create and send a PUT request.
	*
	* @param uri The target URI of the request.
	* @param body The json string to use as the body.
	* @param token The token to use in authorization.
	* @return HttpResponse&lt;String&gt; The returned response object.
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
	* @param uri The target URI of the request.
	* @param body The json string to use as the body.
	* @param token The token to use in authorization.
	* @return HttpResponse&lt;String&gt; The returned response object.
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
	* @param uri The target URI of the request.
	* @param token The token to use in authorization.
	* @return HttpResponse&lt;String&gt; The returned response object.
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
	* @param uri The target URI of the request.
	* @param token The token to use in authorization.
	* @return HttpResponse&lt;String&gt; The returned response object.
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
	* @param msg The error message used to construct the json.
	* @return String A stringified json object containing an error msg and status.
	 */
	public static String error(String msg) {

		JsonObject json = Json.createObjectBuilder()
			.add("status", "failed")
			.add("error", msg)
			.build();

		return jsonb.toJson(json);
	}

	/**
	* Build an ok status json string;
	*
	* @return String A stringified json object containing an ok status.
	 */
	public static String ok() {

		JsonObject json = Json.createObjectBuilder()
			.add("status", "ok")
			.build();

		return jsonb.toJson(json);
	}
}
