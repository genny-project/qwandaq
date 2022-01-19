package life.genny.qwandaq.utils;

import io.quarkus.runtime.annotations.RegisterForReflection;

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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QSearchBeResult;

@RegisterForReflection
public class BaseEntityUtils implements Serializable {

	static final Logger log = Logger.getLogger(BaseEntityUtils.class);
	Jsonb jsonb = JsonbBuilder.create();
	String token;
	String realm;
	GennyToken gennyToken;
	GennyToken serviceToken;

	public BaseEntityUtils() {}

	public BaseEntityUtils(String token, String realm) {
		this(new GennyToken(token));
		this.realm = realm;
	}

	public BaseEntityUtils(GennyToken serviceToken, GennyToken userToken) {
		this(userToken);
		this.serviceToken = serviceToken;
	}

	public BaseEntityUtils(GennyToken gennyToken) {
		this.token = gennyToken.getToken();
		this.realm = gennyToken.getRealm();
		this.gennyToken = gennyToken;
	}

	/**
	* Get the Token
	*
	* @return	The Token
	 */
	public String getToken() {
		return token;
	}

	/**
	* Set the Token
	*
	* @param token
	 */
	public void setToken(String token) {
		this.token = token;
	}


	/**
	* Get the current realm
	*
	* @return 	The realm
	 */
	private String getRealm() {
		return gennyToken.getRealm();
	}

	/**
	 * Get the GennyToken
	 *
	 * @return 	The gennyToken
	 */
	public GennyToken getGennyToken() {
		return gennyToken;
	}

	/**
	 * Set the GennyToken
	 *
	 * @param gennyToken
	 */
	public void setGennyToken(GennyToken gennyToken) {
		this.gennyToken = gennyToken;
	}

	/**
	 * Get the ServiceToken
	 *
	 * @return 	The serviceToken
	 */
	public GennyToken getServiceToken() {
		return serviceToken;
	}

	/**
	 * Set the ServiceToken
	 *
	 * @param serviceToken
	 */
	public void setServiceToken(GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}

	/**
	 * Get a string representation of the instance
	 *
	 * @return Instance as string
	 */
	@Override
	public String toString() {
		return "BaseEntityUtils [" + (realm != null ? "realm=" + realm : "") + ": "
				+ StringUtils.abbreviateMiddle(token, "...", 30) + "]";
	}

	/**
	 * Update the {@link GennyToken} of this utils instance. Unlike the standard 
	 * setter method, this will also update the token and the realm.
	 */
	public void updateGennyToken(GennyToken gennyToken) {
		this.token = gennyToken.getToken();
		this.realm = gennyToken.getRealm();
		this.gennyToken = gennyToken;
	}

	/**
	* Fetch A {@link BaseEntity} from cache the using a code.
	*
	* @param code	The code of the {@link BaseEntity} to fetch
	* @return		The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity getBaseEntityByCode(String code) {

		return CacheUtils.getObject(this.realm, code, BaseEntity.class);
	}

	/**
	* Fetch A {@link BaseEntity} from the database using the entity code.
	*
	* @param code	The code of the {@link BaseEntity} to fetch
	* @return		The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity fetchBaseEntityFromDatabase(String code) {

		String uri = GennySettings.fyodorServiceUrl + "/api/entity/" + code;

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
			e.printStackTrace();
		}

		if (body != null) {
			try {
				BaseEntity be = jsonb.fromJson(body, BaseEntity.class);
				return be;
			} catch (Exception e) {
				log.error(e.getStackTrace());
			}
		}

		return null;
	}

	/**
	* Fetch a list of {@link BaseEntity} objects using a {@link SearchEntity} object.
	*
	* @param searchBE	A {@link SearchEntity} object used to determine the results
	* @return			A list of {@link BaseEntity} objects
	 */
	public List<BaseEntity> getBaseEntitys(SearchEntity searchBE) {

		String uri = GennySettings.fyodorServiceUrl + "/api/search/fetch";
		String json = jsonb.toJson(searchBE);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + this.token)
			.POST(HttpRequest.BodyPublishers.ofString(json))
			.build();

		String body = null;
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			body = response.body();
		} catch (IOException | InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}

		if (body != null) {
			try {
				QSearchBeResult results = jsonb.fromJson(body, QSearchBeResult.class);
				return Arrays.asList(results.getEntities());
			} catch (Exception e) {
				log.error(e);
			}
		}

		return null;
	}

	/**
	* Update a {@link BaseEntity} in the database.
	*
	* @param baseEntity
	 */
	public void updateBaseEntity(BaseEntity baseEntity) {

		String uri = GennySettings.fyodorServiceUrl + "/api/baseentity";
		String json = jsonb.toJson(baseEntity);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uri))
			.setHeader("Content-Type", "application/json")
			.setHeader("Authorization", "Bearer " + this.token)
			.PUT(HttpRequest.BodyPublishers.ofString(json))
			.build();

		HttpResponse<String> response = null;

		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			log.error(e.getLocalizedMessage());
		}

		if (response != null) {
			if (response.statusCode() != 200) {
				log.error("Unable to update " + baseEntity.getCode());
			}
		}
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute. Generally this
	 * will be a LNK attribute, although it doesn't have to be.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return The baseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLNKAttr(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		return getBaseEntityFromLNKAttr(be, attributeCode);
	}

	/**
	 * Get the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The baseEntity with code stored in the attribute
	 */
	public BaseEntity getBaseEntityFromLNKAttr(BaseEntity baseEntity, String attributeCode) {

		String newBaseEntityCode = getBaseEntityCodeFromLNKAttr(baseEntity, attributeCode);
		// return null if attributeCode valueString is null or empty
		if (StringUtils.isEmpty(newBaseEntityCode)) {
			return null;
		}
		BaseEntity newBe = getBaseEntityByCode(newBaseEntityCode);
		return newBe;
	}

	/**
	 * Get the code of the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return The baseEntity code stored in the attribute
	 */
	public String getBaseEntityCodeFromLNKAttr(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		return getBaseEntityCodeFromLNKAttr(be, attributeCode);
	}

	/**
	 * Get the code of the BaseEntity that is linked with a specific attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return The baseEntity code stored in the attribute
	 */
	public String getBaseEntityCodeFromLNKAttr(BaseEntity baseEntity, String attributeCode) {

		String attributeValue = baseEntity.getValue(attributeCode, null);
		if (attributeValue == null) {
			return null;
		}
		String newBaseEntityCode = cleanUpAttributeValue(attributeValue);
		return newBaseEntityCode;
	}

	/**
	 * Get an ArrayList of BaseEntity codes that are linked with a specific
	 * attribute.
	 *
	 * @param baseEntityCode The targeted BaseEntity Code
	 * @param attributeCode  The attribute storing the data
	 * @return An ArrayList of codes stored in the attribute
	 */
	public List<String> getBaseEntityCodeArrayFromLNKAttr(String baseEntityCode, String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		return getBaseEntityCodeArrayFromLNKAttr(be, attributeCode);
	}

	/**
	 * Get an ArrayList of BaseEntity codes that are linked with a specific
	 * attribute.
	 *
	 * @param baseEntity    The targeted BaseEntity
	 * @param attributeCode The attribute storing the data
	 * @return An ArrayList of codes stored in the attribute
	 */
	public List<String> getBaseEntityCodeArrayFromLNKAttr(BaseEntity baseEntity, String attributeCode) {

		String attributeValue = getBaseEntityCodeFromLNKAttr(baseEntity, attributeCode);
		if (attributeValue == null) {
			return null;
		}

		String[] baseEntityCodeArray = attributeValue.split(",");
		List<String> beCodeList = Arrays.asList(baseEntityCodeArray);
		return beCodeList;
	}

	/**
	 * Classic Genny style string clean up. Hope this makes our code look a little
	 * nicer :)
	 *
	 * @param value The value to clean
	 * @return A clean string
	 */
	public String cleanUpAttributeValue(String value) {
		String cleanCode = value.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "");
		return cleanCode;
	}

	public Object getBaseEntityValue(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getObject();
		} else {
			return null;
		}
	}

	public static String getBaseEntityAttrValueAsString(BaseEntity be, String attributeCode) {

		String attributeVal = null;
		for (EntityAttribute ea : be.getBaseEntityAttributes()) {
			try {
				if (ea.getAttributeCode().equals(attributeCode)) {
					attributeVal = ea.getObjectAsString();
				}
			} catch (Exception e) {
			}
		}

		return attributeVal;
	}

	public String getBaseEntityValueAsString(final String baseEntityCode, final String attributeCode) {

		String attrValue = null;

		if (baseEntityCode != null) {

			BaseEntity be = getBaseEntityByCode(baseEntityCode);
			attrValue = getBaseEntityAttrValueAsString(be, attributeCode);
		}

		return attrValue;
	}

	public LocalDateTime getBaseEntityValueAsLocalDateTime(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDateTime();
		} else {
			return null;
		}
	}

	public LocalDate getBaseEntityValueAsLocalDate(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDate();
		} else {
			return null;
		}
	}

	public LocalTime getBaseEntityValueAsLocalTime(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueTime();
		} else {
			return null;
		}
	}

	public List<BaseEntity> convertCodesToBaseEntityArray(String strArr) {

		String[] arr = strArr.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "").split(",");
        List<BaseEntity> entityList = Arrays.stream(arr)
			.filter(item -> !item.isEmpty())
			.map(item -> (BaseEntity) getBaseEntityByCode(item))
			.collect(Collectors.toList());

		return entityList;
	}

	public BaseEntity saveAnswer(Answer answer) {

		// Check if target is valid
		BaseEntity target = getBaseEntityByCode(answer.getTargetCode());
		if (target == null) {
			return null;
		}

		// Filter non-valid answers using DEF
		if (DefUtils.answerValidForDEF(answer)) {

			// Add the answer to our target to return
			// TODO: Create this function
			// target = addAnswer(answer);

			QDataAnswerMessage msg = new QDataAnswerMessage(answer);
			msg.setToken(this.token);

			KafkaUtils.writeMsg("answers", msg);
		}

		return target;
	}

	public void saveAnswers(List<Answer> answers) {

		// sort answers into target BaseEntitys
		Map<String, List<Answer>> answersPerTargetCodeMap = answers.stream()
			.collect(Collectors.groupingBy(Answer::getTargetCode));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {

			// check if target is valid
			BaseEntity target = getBaseEntityByCode(targetCode);
			if (target == null) {
				log.error(targetCode +  " does not exist!");
				continue;
			}

			// fetch the DEF for this target
			BaseEntity defBE = DefUtils.getDEF(target);

			// filter Non-valid answers using def
			List<Answer> group = answersPerTargetCodeMap.get(targetCode);
			List<Answer> validAnswers = group.stream()
				.filter(item -> DefUtils.answerValidForDEF(defBE, item))
				.collect(Collectors.toList());

			// send valid answers for this target
			QDataAnswerMessage msg = new QDataAnswerMessage(validAnswers);
			msg.setToken(this.token);

			KafkaUtils.writeMsg("answers", msg);
		}
	}
}
