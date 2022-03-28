package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.List;

import life.genny.qwandaq.Ask;

public class QDataAskMessage extends QDataMessage {

	private static final long serialVersionUID = 1L;
	private List<Ask> items;
	private static final String DATATYPE_ASK = Ask.class.getSimpleName();

	public QDataAskMessage() {
		super();
	}

	public QDataAskMessage(Ask[] items) {

		super(DATATYPE_ASK);
		setItems(items);
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
		return this.items.toArray(new Ask[0]);
	}

	/** 
	 * @param asks the array of asks to set
	 */
	public void setItems(Ask[] asks) {
		this.items = Arrays.asList(asks);
	}

}
