package life.genny.qwanda.message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;

@RegisterForReflection
public class QDataBaseEntityMessage extends QDataMessage{

	private static final long serialVersionUID = 1L;
	private BaseEntity[] items = new BaseEntity[0];
	private static final String DATATYPE_ATTRIBUTE = Attribute.class.getSimpleName();

	public QDataBaseEntityMessage()
	{
		super(DATATYPE_ATTRIBUTE);
	}
	
	public QDataBaseEntityMessage(BaseEntity[] items) {
		super(DATATYPE_ATTRIBUTE);
		setItems(items);
	}

	public BaseEntity[] getItems() {
		return items;
	}

	public void setItems(BaseEntity[] items) {
		this.items = items;
	}

}
