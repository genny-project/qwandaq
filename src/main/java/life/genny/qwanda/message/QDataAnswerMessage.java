package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;
import life.genny.qwanda.Answer;

import java.util.Arrays;
import java.util.List;

public class QDataAnswerMessage extends QDataMessage {

    private static final long serialVersionUID = 1L;
    @Expose
    private Answer[] items;
    private static final String DATATYPE_ANSWER = Answer.class.getSimpleName();

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
