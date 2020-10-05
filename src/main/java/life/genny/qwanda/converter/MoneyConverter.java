package life.genny.qwanda.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import life.genny.qwanda.MoneyDeserializer;
import org.javamoney.moneta.Money;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class MoneyConverter implements AttributeConverter<Money, String> {
    @Override
    public String convertToDatabaseColumn(final Money money) {
        String ret = "";
        GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(Money.class, new MoneyDeserializer());
        Gson gson = gsonBuilder.create();

        ret = gson.toJson(money);
        return ret;
    }

    @Override
    public Money convertToEntityAttribute(String moneyStr) {
        GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(Money.class, new MoneyDeserializer());
        Gson gson = gsonBuilder.create();
        return gson.fromJson(moneyStr, Money.class);
    }
}
