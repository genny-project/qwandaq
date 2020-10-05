package life.genny.qwanda.message;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class QEventMessage extends QMessage implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE_TYPE = "EVT_MSG";
	@Expose
	private String event_type;
	@Expose
	public MessageData data;


	@Override
	public String toString() {
		return "QEventMessage [event_type=" + event_type + ", data=" + data + "]";
	}
	public String getEvent_type() {
		return event_type;
	}
	public void setEvent_type(String event_type) {
		this.event_type = event_type;
	}
	public MessageData getData() {
		return data;
	}
	public void setData(MessageData data) {
		this.data = data;
	}
	

	
	public QEventMessage(String eventType, String code) {
		super(MESSAGE_TYPE);
		this.event_type = eventType;
		this.data = new MessageData(code);
	}
	
}
