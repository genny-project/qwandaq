package life.genny.qwanda.message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.attribute.EntityAttribute;

@RegisterForReflection
public class QDataEntityAttributeMessage extends QDataMessage{

	private static final long serialVersionUID = 1L;
	private EntityAttribute[] items = new EntityAttribute[0];
	private static final String DATATYPE_ATTRIBUTE = EntityAttribute.class.getSimpleName();

	public QDataEntityAttributeMessage()
	{
		super(DATATYPE_ATTRIBUTE);
	}
	
	public QDataEntityAttributeMessage(EntityAttribute[] items) {
		super(DATATYPE_ATTRIBUTE);
		setItems(items);
	}

	public EntityAttribute[] getItems() {
		return items;
	}

	public void setItems(EntityAttribute[] items) {
		this.items = items;
	}

}
