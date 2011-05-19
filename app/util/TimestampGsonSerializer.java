package util;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;

public class TimestampGsonSerializer implements JsonSerializer<Date>, JsonDeserializer<Date> {
    @Override
    public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(String.valueOf(date.getTime()));
    }

    @Override
    public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new Date(jsonElement.getAsLong());
    }
}
