/*
 * Copyright 2016-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.cfg;

import java.util.List;
import java.util.Map;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.Embedded;
import org.grails.datastore.mapping.reflect.ClassUtils;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.datasource.MultipleDataSourceSupport;
import org.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.grails.orm.hibernate.support.HibernateRuntimeUtils;

/**
 * Utility methods for configuring Hibernate inside Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsHibernateUtil extends HibernateRuntimeUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(GrailsHibernateUtil.class);

    public static final String ARGUMENT_FETCH_SIZE = "fetchSize";

    public static final String ARGUMENT_TIMEOUT = "timeout";

    public static final String ARGUMENT_READ_ONLY = "readOnly";

    public static final String ARGUMENT_FLUSH_MODE = "flushMode";

    public static final String ARGUMENT_MAX = "max";

    public static final String ARGUMENT_OFFSET = "offset";

    public static final String ARGUMENT_ORDER = "order";

    public static final String ARGUMENT_SORT = "sort";

    public static final String ORDER_DESC = "desc";

    public static final String ORDER_ASC = "asc";

    public static final String ARGUMENT_FETCH = "fetch";

    public static final String ARGUMENT_IGNORE_CASE = "ignoreCase";

    public static final String ARGUMENT_CACHE = "cache";

    public static final String ARGUMENT_LOCK = "lock";

    public static final Class<?>[] EMPTY_CLASS_ARRAY = {};


    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    private static FlushMode convertFlushMode(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof FlushMode) {
            return (FlushMode) object;
        }
        return FlushMode.valueOf(String.valueOf(object));
    }

    /**
     * Retrieves the fetch mode for the specified instance; otherwise returns the default FetchMode.
     *
     * @param object The object, converted to a string
     * @return The FetchMode
     */
    public static FetchMode getFetchMode(Object object) {
        String name = object != null ? object.toString() : "default";
        if (name.equalsIgnoreCase(FetchMode.JOIN.toString()) || name.equalsIgnoreCase("eager")) {
            return FetchMode.JOIN;
        }
        if (name.equalsIgnoreCase(FetchMode.SELECT.toString()) || name.equalsIgnoreCase("lazy")) {
            return FetchMode.SELECT;
        }
        return FetchMode.DEFAULT;
    }

    /**
     * Sets the target object to read-only using the given SessionFactory instance. This
     * avoids Hibernate performing any dirty checking on the object
     *
     * @see #setObjectToReadWrite(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadyOnly(Object target, SessionFactory sessionFactory) {
        Object resource = TransactionSynchronizationManager.getResource(sessionFactory);
        if (resource != null) {
            Session session = sessionFactory.getCurrentSession();
            if (canModifyReadWriteState(session, target)) {
                if (target instanceof HibernateProxy) {
                    target = ((HibernateProxy) target).getHibernateLazyInitializer().getImplementation();
                }
                session.setReadOnly(target, true);
                session.setHibernateFlushMode(FlushMode.MANUAL);
            }
        }
    }

    private static boolean canModifyReadWriteState(Session session, Object target) {
        return session.contains(target) && Hibernate.isInitialized(target);
    }

    /**
     * Sets the target object to read-write, allowing Hibernate to dirty check it and auto-flush changes.
     *
     * @see #setObjectToReadyOnly(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadWrite(final Object target, SessionFactory sessionFactory) {
        Session session = sessionFactory.getCurrentSession();
        if (!canModifyReadWriteState(session, target)) {
            return;
        }

        SessionImplementor sessionImpl = (SessionImplementor) session;
        EntityEntry ee = sessionImpl.getPersistenceContext().getEntry(target);

        if (ee == null || ee.getStatus() != Status.READ_ONLY) {
            return;
        }

        Object actualTarget = target;
        if (target instanceof HibernateProxy) {
            actualTarget = ((HibernateProxy) target).getHibernateLazyInitializer().getImplementation();
        }

        session.setReadOnly(actualTarget, false);
        session.setHibernateFlushMode(FlushMode.AUTO);
        incrementVersion(target);
    }

    /**
     * Increments the entities version number in order to force an update
     * @param target The target entity
     */
    public static void incrementVersion(Object target) {
        MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
        if (metaClass.hasProperty(target, GormProperties.VERSION) != null) {
            Object version = metaClass.getProperty(target, GormProperties.VERSION);
            if (version instanceof Long) {
                Long newVersion = (Long) version + 1;
                metaClass.setProperty(target, GormProperties.VERSION, newVersion);
            }
        }
    }

    /**
     * Ensures the meta class is correct for a given class
     *
     * @param target The GroovyObject
     * @param persistentClass The persistent class
     */
    @Deprecated
    public static void ensureCorrectGroovyMetaClass(Object target, Class<?> persistentClass) {
        if (target instanceof GroovyObject) {
            GroovyObject go = ((GroovyObject) target);
            if (!go.getMetaClass().getTheClass().equals(persistentClass)) {
                go.setMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(persistentClass));
            }
        }
    }

    /**
     * Unwraps and initializes a HibernateProxy.
     * @param proxy The proxy
     * @return the unproxied instance
     */
    public static Object unwrapProxy(HibernateProxy proxy) {
        return proxyHandler.unwrapProxy(proxy);
    }

    /**
     * Returns the proxy for a given association or null if it is not proxied
     *
     * @param obj The object
     * @param associationName The named assoication
     * @return A proxy
     */
    public static HibernateProxy getAssociationProxy(Object obj, String associationName) {
        return proxyHandler.getAssociationProxy(obj, associationName);
    }

    /**
     * Checks whether an associated property is initialized and returns true if it is
     *
     * @param obj The name of the object
     * @param associationName The name of the association
     * @return true if is initialized
     */
    public static boolean isInitialized(Object obj, String associationName) {
        return proxyHandler.isInitialized(obj, associationName);
    }

    public static Object unwrapIfProxy(Object instance) {
        return proxyHandler.unwrapIfProxy(instance);
    }

    /**
     * @deprecated Use {@link  MultipleDataSourceSupport#getDefaultDataSource(PersistentEntity)} instead
     */
    @Deprecated
    public static String getDefaultDataSource(PersistentEntity domainClass) {
        return MultipleDataSourceSupport.getDefaultDataSource(domainClass);
    }

    /**
     * @deprecated Use {@link  MultipleDataSourceSupport#getDatasourceNames(PersistentEntity)} instead
     */
    @Deprecated
    public static List<String> getDatasourceNames(PersistentEntity domainClass) {
        return MultipleDataSourceSupport.getDatasourceNames(domainClass);
    }

    /**
     * @deprecated Use {@link  MultipleDataSourceSupport#getDefaultDataSource(PersistentEntity)} instead
     */
    @Deprecated
    public static boolean usesDatasource(PersistentEntity domainClass, String dataSourceName) {
        return MultipleDataSourceSupport.usesDatasource(domainClass, dataSourceName);
    }

    public static boolean isMappedWithHibernate(PersistentEntity domainClass) {
        return domainClass instanceof HibernatePersistentEntity;
    }

    public static String qualify(final String prefix, final String name) {
        return StringHelper.qualify(prefix, name);
    }

    public static boolean isNotEmpty(final String string) {
        return StringHelper.isNotEmpty(string);
    }

    public static String unqualify(final String qualifiedName) {
        return StringHelper.unqualify(qualifiedName);
    }

}
