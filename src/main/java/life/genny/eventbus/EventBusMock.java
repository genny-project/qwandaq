package life.genny.eventbus;

import io.vertx.rxjava.core.eventbus.EventBus;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public class EventBusMock implements EventBusInterface {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


    public EventBusMock() {

    }

    public EventBusMock(EventBus eventBus) {

    }


}

