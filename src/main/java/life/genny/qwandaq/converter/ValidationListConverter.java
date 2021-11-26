package life.genny.qwandaq.converter;


import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.StringReader;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonReader;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.validation.Validation;
import life.genny.qwandaq.validation.ValidationList;

@Converter
public class ValidationListConverter implements AttributeConverter<List<Validation>, String> {

	private static final Logger log = Logger.getLogger(ValidationListConverter.class);

	@Override
	public String convertToDatabaseColumn(final List<Validation> list) {
		String ret = "";
		for (final Validation validation : list) {
			String validationGroupStr = "";
			if (validation != null) {
			if (validation.getSelectionBaseEntityGroupList() != null) {
				validationGroupStr += "\"" + convertToString(validation.getSelectionBaseEntityGroupList()) + "\"";
				validationGroupStr += ",\"" + (validation.getMultiAllowed() ? "TRUE" : "FALSE") + "\"";
				validationGroupStr += ",\"" + (validation.getRecursiveGroup() ? "TRUE" : "FALSE") + "\",";
				ret += "\"" + validation.getCode() + "\",\"" + validation.getName() + "\",\"" + validation.getRegex()
				+ "\"," + validationGroupStr;
			} else {
				ret += "\"" + validation.getCode() + "\",\"" + validation.getName() + "\",\"" + validation.getRegex()+"\",";

			}
			}
		
		}
		ret = StringUtils.removeEnd(ret, ",");
		if (ret.length() >= 512) {
			log.error("Error -> field > 512 " + ret + ":" + ret.length());
		}

		return ret;

	}

	@SuppressWarnings("deprecation")
	@Override
	public List<Validation> convertToEntityAttribute(String joined) {
		final List<Validation> validations = new CopyOnWriteArrayList<Validation>();
		if (joined != null) {
		//	log.info("ValidationStr=" + joined);
			if (!StringUtils.isBlank(joined)) {
				joined = joined.substring(1); // remove leading quotes
				joined = StringUtils.chomp(joined, "\""); // remove last char
				final String[] validationListStr = joined.split("\",\"");

				if (validationListStr.length == 6) {
				//	log.info("ValidationListStr LENGTH=6");
					for (int i = 0; i < validationListStr.length; i = i + 6) {
						List<String> validationGroups = convertFromString(validationListStr[i + 3]);
						List<String> regexs = convertFromString(validationListStr[i + 2]);
						Validation v = new Validation(validationListStr[i], validationListStr[i + 1], validationGroups,
								validationListStr[i + 3].equalsIgnoreCase("TRUE"),
								validationListStr[i + 4].equalsIgnoreCase("TRUE"));
						if (!regexs.isEmpty()) {
							v.setRegex(regexs.get(0));
						}
						validations.add(v);
					}

				} else {
					for (int i = 0; i < validationListStr.length; i = i + 3) {
						Validation validation  = new Validation(validationListStr[i], validationListStr[i + 1],
								validationListStr[i + 2]);
					//	log.info("VALIDATION:"+validation);
						validations.add(validation);
					}
				}

			}
		}
		return validations;
	}

	public String convertToString(final List<String> list) {

		// log.info(list);

		// JsonArrayBuilder builder = Json.createArrayBuilder();
		// for(String item : list) {
		// 	builder.add(item);
		// }
		// JsonArray array = builder.build();
		// String json = array.toString();
		return list.toString();
	}

	public List<String> convertFromString(final String joined) {

		List<String> list = new CopyOnWriteArrayList<String>();
		if (joined.startsWith("[") || joined.startsWith("{")) {
			JsonReader reader = Json.createReader(new StringReader(joined));
			JsonArray array = reader.readArray();
			list = (List) array;
		} else {
			list.add(joined);
		}

		return list;
	}
}
