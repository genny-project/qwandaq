package life.genny.qwanda;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeDeserializer implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
	 private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

//    @Override
//    public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext jsonSerializationContext) {
//        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
//        Date date = Date.from(instant);
//        return new JsonPrimitive(date.getTime());
//    }
    
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context)
    {
      return new JsonPrimitive(FORMATTER.format(src));
    }
    
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException
    {
      return FORMATTER.parse(json.getAsString(), LocalDateTime::from);
    }
}