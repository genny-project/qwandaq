package life.genny.qwandaq.converter;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

  
  /** 
   * @param list
   * @return String
   */
  @Override
  public String convertToDatabaseColumn(final List<String> list) {
    String ret = "";
    if (list!=null) {
    for (final String str : list) {
      ret += str + ",";
    }
    }
    return ret;

  }

  
  /** 
   * @param joined
   * @return List<String>
   */
  @Override
  public List<String> convertToEntityAttribute(final String joined) {
    List<String> strings = new CopyOnWriteArrayList<String>();
    if (joined != null) {
      strings = new CopyOnWriteArrayList<>(Arrays.asList(joined.split(",")));
    }
    return strings;
  }

}
