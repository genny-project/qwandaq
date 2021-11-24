package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.List;


import life.genny.qwandaq.Answer;
import life.genny.qwandaq.entity.BaseEntity;

public class QDataAnswerMessage extends QDataMessage {

	private static final long serialVersionUID = 1L;
	private Answer[] items;
	private static final String DATATYPE_ANSWER = Answer.class.getSimpleName();

	private QDataAnswerMessage() {
		super(DATATYPE_ANSWER);
	}
	
	public QDataAnswerMessage(Answer[] items) {
		super(DATATYPE_ANSWER);
		setItems(items);
	}
	
	
	public QDataAnswerMessage(List<Answer> items) {
		super(DATATYPE_ANSWER);
		setItems(items.toArray(new Answer[0]));
	}
	public QDataAnswerMessage(Answer item) {
		super(DATATYPE_ANSWER);
		items = new Answer[1];
		items[0] = item;
		setItems(items);
	}

	public Answer[] getItems() {
		return items;
	}

	public void setItems(Answer[] items) {
		this.items = items;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QDataAnswerMessage [" + (items != null ? "items=" + Arrays.toString(items) : "") + "]";
	}
	
	
}
