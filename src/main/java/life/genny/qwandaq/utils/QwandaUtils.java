package life.genny.qwandaq.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;

@RegisterForReflection
public class QwandaUtils {

	static final Logger log = Logger.getLogger(QwandaUtils.class);
	private static final ExecutorService executorService = Executors.newFixedThreadPool(200);

	// private static HttpClient httpClient =
	// HttpClient.newBuilder().executor(executorService)
	// .version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(20)).build();

	static Map<String, Map<String, Attribute>> attributes = new ConcurrentHashMap<>();

	static Jsonb jsonb = JsonbBuilder.create();

	static GennyToken gennyToken;

	public static void init(GennyToken token) {
		gennyToken = token;
		loadAllAttributes();
	}

	public static void init(GennyToken token, List<Attribute> attributeList) {
		gennyToken = token;

		attributes.put(token.getRealm(), new ConcurrentHashMap<String, Attribute>());
		Map<String, Attribute> attributeMap = attributes.get(token.getRealm());

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
			log.error("Bad Attribute in Map for realm " + realm + " and code " + attributeCode);
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

		List<Attribute> attributeList = null;

		try {
			attributeList = DatabaseUtils.fetchAttributes(realm);
			log.info("Loaded all attributes for realm " + realm);
			if (attributeList == null) {
				log.error("Null attributeList, not putting in map!!!");
				return;
			}

			// Check for existing map
			if (!attributes.containsKey(realm)) {
				attributes.put(realm, new ConcurrentHashMap<String, Attribute>());
			}
			Map<String, Attribute> attributeMap = attributes.get(realm);

			// Insert attributes into map
			for (Attribute attribute : attributeList) {
				attributeMap.put(attribute.getCode(), attribute);
			}

			log.info("All attributes have been loaded: " + attributeMap.size() + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for realm " + realm);
		}

	}

	/**
	 * Remove an atttribute from the in memory set using the code.
	 *
	 * @param code Code of the attribute to remove.
	 */
	public static void removeAttributeFromMemory(String code) {

		String realm = gennyToken.getRealm();
		attributes.get(realm).remove(code);
	}

	/**
	 * Send a {@link QEventMessage} to shleemy for scheduling.
	 *
	 * @param userToken
	 * @param eventMsgCode
	 * @param scheduleMsgCode
	 * @param triggertime
	 * @param targetCode
	 */
	public static void scheduleEvent(GennyToken userToken, String eventMsgCode, String scheduleMsgCode,
			LocalDateTime triggertime, String targetCode) {

		// create the event message
		QEventMessage evtMsg = new QEventMessage("SCHEDULE_EVT", eventMsgCode);
		evtMsg.setToken(userToken.getToken());
		evtMsg.getData().setTargetCode(targetCode);

		// create a recipient list
		String[] rxList = new String[2];
		rxList[0] = "SUPERUSER";
		rxList[1] = userToken.getUserCode();
		evtMsg.setRecipientCodeArray(rxList);

		log.info("Scheduling event: " + eventMsgCode + ", Trigger time: " + triggertime.toString());

		// create schedule message
		QScheduleMessage scheduleMessage = new QScheduleMessage(scheduleMsgCode, jsonb.toJson(evtMsg),
				userToken.getUserCode(), "project", triggertime, userToken.getRealm());
		log.info("Sending message " + scheduleMessage.getCode() + " to shleemy for scheduling");

		// send msg to shleemy
		String json = jsonb.toJson(scheduleMessage);
		KafkaUtils.writeMsg("schedule", json);
	}

	/**
	 * Delete a currently scheduled message via shleemy.
	 *
	 * @param userToken
	 * @param code
	 */
	public static void deleteSchedule(GennyToken userToken, String code) {

		// String uri = GennySettings.shleemyServiceUrl + "/api/schedule/code/" + code;

		// HttpClient client = HttpClient.newHttpClient();
		// HttpRequest request = HttpRequest.newBuilder()
		// .uri(URI.create(uri))
		// .setHeader("Content-Type", "application/json")
		// .setHeader("Authorization", "Bearer " + userToken.getToken())
		// .DELETE().build();

		// try {

		// HttpResponse<String> response = client.send(request,
		// HttpResponse.BodyHandlers.ofString());

		// if (response.statusCode() != 200) {
		// log.error("Unable to delete scheduled message " + code);
		// }

		// } catch (IOException | InterruptedException e) {
		// log.error(e);
		// }
	}

	static public String apiGet(String url, String authToken) {

		// HttpRequest.Builder requestBuilder = Optional.ofNullable(authToken)
		// .map(token -> HttpRequest.newBuilder()
		// .GET()
		// .uri(URI.create(url))
		// .setHeader("Content-Type", "application/json")
		// .setHeader("Authorization", "Bearer " + token))
		// .orElse(
		// HttpRequest.newBuilder()
		// .GET()
		// .uri(URI.create(url)));

		// if (url.contains("genny.life")) { // Hack for local server not having http2
		// requestBuilder = requestBuilder.version(HttpClient.Version.HTTP_1_1);
		// }

		// HttpRequest request = requestBuilder.build();

		// String result = null;
		// Boolean done = false;
		// int count = 5;
		// while ((!done) && (count > 0)) {

		// CompletableFuture<java.net.http.HttpResponse<String>> response =
		// httpClient.sendAsync(request,
		// java.net.http.HttpResponse.BodyHandlers.ofString());

		// try {
		// result = response.thenApply(java.net.http.HttpResponse::body).get(20,
		// TimeUnit.SECONDS);
		// done = true;
		// } catch (InterruptedException | ExecutionException | TimeoutException e) {
		// // TODO Auto-generated catch block
		// log.error("Count:" + count + ", Exception occurred when post to URL: " + url
		// + ",Body is authToken:"
		// + authToken + ", Exception details:" + e.getCause());
		// httpClient =
		// HttpClient.newBuilder().executor(executorService).version(HttpClient.Version.HTTP_2)
		// .connectTimeout(Duration.ofSeconds(20)).build();
		// if (count <= 0) {
		// done = true;
		// }

		// }
		// count--;
		// }
		// // System.out.println(result);
		// // can't find
		// if (result.equals("<html><head><title>Error</title></head><body>Not
		// Found</body></html>")) {
		// log.error("Can't find result for request:" + url + ", set returned result to
		// NULL");
		// result = null;
		// }

		// return result;
		return null;
	}
}
