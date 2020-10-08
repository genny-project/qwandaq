package life.genny.qwanda.qwandautils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.MoneyDeserializer;
import life.genny.qwanda.datatype.LocalDateConverter;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class QwandaJsonUtils {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    static GsonBuilder gsonBuilder = new GsonBuilder();

    static public Gson gson = gsonBuilder.registerTypeAdapter(Money.class, new MoneyDeserializer())
            .registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer()).setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateConverter())

            .excludeFieldsWithoutExposeAnnotation().create();


    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T fromJson(final String json, Class clazz) {
        T item = null;
        if (json != null) {
            try {
                item = (T) gson.fromJson(json, clazz);
            } catch (Exception e) {
                log.error("Bad Deserialisation for " + clazz.getSimpleName());
            }
        }
        return item;
    }

    public static <T> T fromJson(final String json, Type clazz) {
        T item = null;
        if (json != null) {
            try {
                item = gson.fromJson(json, clazz);
            } catch (Exception e) {
                log.error("Bad Deserialisation for " + clazz.getTypeName());
            }
        }
        return item;
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
}



