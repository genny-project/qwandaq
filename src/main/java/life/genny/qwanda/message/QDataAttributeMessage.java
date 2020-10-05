package life.genny.qwanda.message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.attribute.Attribute;

@RegisterForReflection
public class QDataAttributeMessage extends QDataMessage{

	private static final long serialVersionUID = 1L;
	private Attribute[] items = new Attribute[0];
	private static final String DATATYPE_ATTRIBUTE = Attribute.class.getSimpleName();

	public QDataAttributeMessage()
	{
		super(DATATYPE_ATTRIBUTE);
	}
	
	public QDataAttributeMessage(Attribute[] items) {
		super(DATATYPE_ATTRIBUTE);
		setItems(items);
	}

	public Attribute[] getItems() {
		return items;
	}

	public void setItems(Attribute[] items) {
		this.items = items;
	}

}
