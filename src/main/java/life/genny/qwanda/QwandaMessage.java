package life.genny.qwanda;
import com.google.gson.annotations.Expose;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QDataAskMessage;

import java.io.Serializable;

public class QwandaMessage extends QCmdMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Expose
    public QDataAskMessage asks;
    @Expose
    public QBulkMessage askData;

    private static final String CMD_TYPE = "CMD_BULKASK";
    private static final String CODE = "QWANDAMESSAGE";


    public QwandaMessage() {
        super(CMD_TYPE, CODE);

    }


}

