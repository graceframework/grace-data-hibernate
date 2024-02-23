package org.grails.orm.hibernate.support;

import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.springframework.context.ApplicationContextAware;

/**
 * Abstract class for defining the event triggering interceptor
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public abstract class AbstractClosureEventTriggeringInterceptor
        extends DefaultSaveOrUpdateEventListener
        implements ApplicationContextAware,
        PreLoadEventListener,
        PostLoadEventListener,
        PostInsertEventListener,
        PostUpdateEventListener,
        PostDeleteEventListener,
        PreDeleteEventListener,
        PreUpdateEventListener,
        PreInsertEventListener {

}
