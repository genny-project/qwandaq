package life.genny.qwandaq.message;


import java.util.Arrays;
import java.util.List;

import life.genny.qwandaq.attribute.Attribute;

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
		if (items != null) {
			return items.toArray(new Attribute[0]);
		} else {
			return null;
		}
		
	}

	/** 
	 * @param items the array of attributes to set
	 */
	public void setItems(Attribute[] items) {
		this.items = Arrays.asList(items);
	}

}
