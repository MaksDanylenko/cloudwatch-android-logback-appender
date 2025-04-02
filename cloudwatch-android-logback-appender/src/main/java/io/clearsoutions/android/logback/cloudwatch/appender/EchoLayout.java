package io.clearsoutions.android.logback.cloudwatch.appender;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

public class EchoLayout<E> extends LayoutBase<E> {

    public String doLayout(E event) {
        return event + CoreConstants.LINE_SEPARATOR;
    }

}