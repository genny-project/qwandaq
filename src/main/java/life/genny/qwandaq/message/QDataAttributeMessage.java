package life.genny.qwandaq.message;


import java.util.Arrays;
import java.util.List;

import life.genny.qwandaq.annotation.ProtoMessage;
import life.genny.qwandaq.attribute.Attribute;

@ProtoMessage
public class QDataAttributeMessage extends QDataMessage{

	private static final long serialVersionUID = 1L;
	private List<Attribute> items;
	private static final String DATATYPE_ATTRIBUTE = Attribute.class.getSimpleName();

	public QDataAttributeMessage(Attribute[] items) {

		super(DATATYPE_ATTRIBUTE);
		setItems(items);
	}

	/** 
	 * @return Attribute[]
	 */
	public Attribute[] getItems() {
		return items.toArray(new Attribute[0]);
	}

	/** 
	 * @param items the array of attributes to set
	 */
	public void setItems(Attribute[] items) {
		this.items = Arrays.asList(items);
	}

}
