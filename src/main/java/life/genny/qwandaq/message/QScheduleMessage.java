package life.genny.qwandaq.message;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;




public class QScheduleMessage implements Serializable {

	
		/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
		public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));
		public LocalDateTime updated;

		public String cron;
		
		public LocalDateTime triggertime;
		
		public String realm;

		public String jsonMessage;
		
		public String sourceCode;

		public String channel;
		
		public String code;


	
	public QScheduleMessage()
	{}
	
	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final String cron, final String realm)
	{
		this.code = code;
		this.cron = cron;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}
	
	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final LocalDateTime triggertime, final String realm)
	{
		this.code = code;
		this.triggertime = triggertime;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}
	
}
