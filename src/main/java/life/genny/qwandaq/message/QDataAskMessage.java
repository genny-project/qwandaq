package life.genny.qwandaq.message;

import java.io.Serializable;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.entity.BaseEntity;

public class QDataAskMessage extends QDataMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Ask[] items;
	private static final String DATATYPE_ASK = Ask.class.getSimpleName();

	public QDataAskMessage() {
		super();
	}

	public QDataAskMessage(Ask[] items) {
		super(DATATYPE_ASK);
		// if ((items == null)||(items.length == 0)) {
		// setItems(new Ask[0]);
		// } else {
		setItems(items);
		// }

	}

	public QDataAskMessage(Ask ask) {
		super(DATATYPE_ASK);
		Ask[] asks = new Ask[1];
		asks[0] = ask;
		setItems(asks);
	}

	
	/** 
	 * @return Ask[]
	 */
	public Ask[] getItems() {
		return this.items;
	}

	
	/** 
	 * @param asks
	 */
	public void setItems(Ask[] asks) {
		// if ((items == null)||(items.length == 0)) {
		// this.items = new Ask[0];
		// } else {
		this.items = asks;
		// }

	}

}
