package life.genny.qwandaq.datatype;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
	
	/** 
	 * @param s
	 * @return LocalDateTime
	 * @throws Exception
	 */
	@Override
	public LocalDateTime unmarshal(String s) throws Exception {
		return LocalDateTime.parse(s);
	}

	
	/** 
	 * @param dateTime
	 * @return String
	 * @throws Exception
	 */
	@Override
	public String marshal(LocalDateTime dateTime) throws Exception {
		return dateTime.toString();
	}
}
