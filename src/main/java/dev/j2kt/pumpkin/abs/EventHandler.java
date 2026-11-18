package dev.j2kt.pumpkin.abs;

import pumpkin.EventPriority;
import pumpkin.EventType;

/** Declares a listener method for the build-time listener generator. */
public @interface EventHandler {
    EventType type();

    EventPriority priority() default EventPriority.NORMAL;

    boolean ignoreCancelled() default false;
}
