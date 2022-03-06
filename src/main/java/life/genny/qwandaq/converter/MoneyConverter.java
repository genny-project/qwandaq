package life.genny.qwandaq.converter;

import java.lang.invoke.MethodHandles;
import java.io.StringReader;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.jboss.logging.Logger;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;

@Converter
public class MoneyConverter implements AttributeConverter<Money, String> {

	private static final Logger log = Logger.getLogger(MoneyConverter.class);

	
	/** 
	 * @param money
	 * @return String
	 */
	@Override
	public String convertToDatabaseColumn(final Money money) {
		if (money == null) {
			return null;
		}
		return "{\"amount\":" + money.getNumber() + ",\"currency\":\"" + money.getCurrency().getCurrencyCode() + "\"}";
	}

	
	/** 
	 * @param moneyStr
	 * @return Money
	 */
	@Override
	public Money convertToEntityAttribute(String moneyStr) {

		if (moneyStr == null || moneyStr.equals("null")) {
			return null;
		}
		JsonReader reader = Json.createReader(new StringReader(moneyStr));
		JsonObject obj = reader.readObject();

		CurrencyUnit currency = Monetary.getCurrency(obj.getString("currency"));
		Double amount = Double.valueOf(obj.getString("amount"));

		Money money = Money.of(amount, currency);
		return money;
	}


}
