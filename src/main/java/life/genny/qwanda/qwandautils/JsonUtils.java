package life.genny.qwanda.qwandautils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.datatype.LocalDateConverter;
import org.jboss.logging.Logger;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class JsonUtils {
	
	 private static final Logger log = Logger.getLogger(JsonUtils.class);	

	static GsonBuilder gsonBuilder = new GsonBuilder();       

	static public Gson gson = gsonBuilder
			.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer())
			.registerTypeAdapter(LocalDate.class, new LocalDateConverter())
		//	.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
		//	.excludeFieldsWithoutExposeAnnotation()
		//    .disableHtmlEscaping()
		    .setPrettyPrinting()
			.create();

	static public Gson gsonFull = gsonBuilder
			.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer())
			.registerTypeAdapter(LocalDate.class, new LocalDateConverter())
		//	.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
			.excludeFieldsWithoutExposeAnnotation()
		//    .disableHtmlEscaping()
			.serializeNulls()
		    .setPrettyPrinting()
			.create();

	
	public static <T> T fromJson(final String json, Class clazz)
	{
	        T item = null;
	        if (json != null) {
	                try {

	                      item = (T)gson.fromJson(json, clazz);

	                } catch (Exception e) {
//	                	     log.error("The JSON file received is  :::  "+json);;
	                     log.error("Bad Deserialisation for "+clazz.getSimpleName()+":"+e.getLocalizedMessage());
	                }
	        }
	        return item;
	}
	
	public static <T> T fromJson(final String json, Type clazz)
	{
	        T item = null;
	        if (json != null) {
	                try {
	                      item = (T)gson.fromJson(json, clazz);

	                } catch (Exception e) {
//	                	log.error("The JSON file received is  :::  "+json);;
	                	log.error("Bad Deserialisation for "+clazz.getTypeName()+":"+e.getLocalizedMessage());
	                }
	        }
	        return item;
	}
	
	public static String toJson(Object obj)
	{
	
		String ret =  gson.toJson(obj);
		return ret;
	}

	public static String toJsonWithNulls(Object obj)
	{
	
		String ret =  gsonFull.toJson(obj);
		return ret;
	}
	
	
}

