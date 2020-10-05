package life.genny.qwanda;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public class MoneyDeserializer implements JsonSerializer<Money>, JsonDeserializer<Money> {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


    @Override
    public JsonElement serialize(Money src, Type typeOfSrc, JsonSerializationContext context) {
        DecimalFormat decimalFormat = new DecimalFormat("###############0.00");

        String amount = decimalFormat.format(src.getNumber().doubleValue());
        if (amount.contains("+")) {
            log.debug("debug");
        }
        return JsonParser
                .parseString("{\"amount\":" + amount + ",\"currency\":\"" + src.getCurrency().getCurrencyCode() + "\"}");
    }

    @Override
    public Money deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final CurrencyUnit currency = Monetary.getCurrency(json.getAsJsonObject().get("currency").getAsString());

        if (StringUtils.isBlank(json.getAsJsonObject().get("amount").getAsString()))
            return null; // TODO, can we use Optional<Money> ?
        else {
            String amountStr = json.getAsJsonObject().get("amount").getAsString();
            if (amountStr.contains("+")) {
                log.debug("debug");
            }
            BigDecimal bDamount = new BigDecimal(amountStr);

            return Money.of(bDamount, currency);
        }
    }
}