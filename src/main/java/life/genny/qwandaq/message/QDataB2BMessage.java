package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.models.GennyItem;

@RegisterForReflection
public class QDataB2BMessage extends QDataMessage {
	
	
	private static final long serialVersionUID = 1L;

	  @JsonProperty
	  private List<GennyItem> items;
	  private static final String DATATYPE_ITEM = GennyItem.class.getSimpleName();

	  // For json parameters
	  public QDataB2BMessage() {
		  super(DATATYPE_ITEM);
	  }

	  public QDataB2BMessage(final GennyItem[] items) {
	    super(DATATYPE_ITEM);
	    setItems(items);
	  }

	  public GennyItem[] getItems() {
		if (items != null) {
			return items.toArray(new GennyItem[0]);
		} else {
			return null;
		}
	  }

	  public void setItems(final GennyItem[] items) {
	    this.items = Arrays.asList(items);
	  }
	
}
