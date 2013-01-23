package au.org.massive.launcher;

import java.util.ArrayList;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class StringAppender extends AppenderSkeleton {
    StringBuilder log = new StringBuilder();

    @Override
    protected void append(LoggingEvent event) {
        log.append(getLayout().format(event));
    }

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

}

