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
package org.grails.orm.hibernate.support;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.ExecutableList;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.AbstractPreDatabaseOperationEvent;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;

import org.grails.datastore.gorm.GormValidateable;
import org.grails.datastore.gorm.support.BeforeValidateHelper.BeforeValidateEventTriggerCaller;
import org.grails.datastore.gorm.support.EventTriggerCaller;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.grails.datastore.mapping.engine.event.ValidationEvent;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassUtils;
import org.grails.datastore.mapping.reflect.EntityReflector;
import org.grails.datastore.mapping.validation.ValidationException;
import org.grails.orm.hibernate.AbstractHibernateGormValidationApi;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * <p>Also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'.
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 * @since 1.3.5
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClosureEventListener implements SaveOrUpdateEventListener,
        PreLoadEventListener,
        PostLoadEventListener,
        PostInsertEventListener,
        PostUpdateEventListener,
        PostDeleteEventListener,
        PreDeleteEventListener,
        PreUpdateEventListener {

    private static final long serialVersionUID = 1;

    protected static final Logger LOG = LoggerFactory.getLogger(ClosureEventListener.class);

    private final EventTriggerCaller saveOrUpdateCaller;

    private final EventTriggerCaller beforeInsertCaller;

    private final EventTriggerCaller preLoadEventCaller;

    private final EventTriggerCaller postLoadEventListener;

    private final EventTriggerCaller postInsertEventListener;

    private final EventTriggerCaller postUpdateEventListener;

    private final EventTriggerCaller postDeleteEventListener;

    private final EventTriggerCaller preDeleteEventListener;

    private final EventTriggerCaller preUpdateEventListener;

    private final BeforeValidateEventTriggerCaller beforeValidateEventListener;

    private final PersistentEntity persistentEntity;

    private final MetaClass domainMetaClass;

    private final boolean isMultiTenant;

    private final boolean failOnErrorEnabled;

    private final Map validateParams;

    private Field actionQueueUpdatesField;

    private Field entityUpdateActionStateField;

    public ClosureEventListener(PersistentEntity persistentEntity, boolean failOnError, List failOnErrorPackages) {
        this.persistentEntity = persistentEntity;
        Class domainClazz = persistentEntity.getJavaClass();
        this.domainMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(domainClazz);
        this.isMultiTenant = ClassUtils.isMultiTenant(domainClazz);
        this.saveOrUpdateCaller = buildCaller(AbstractPersistenceEvent.ONLOAD_SAVE, domainClazz);
        this.beforeInsertCaller = buildCaller(AbstractPersistenceEvent.BEFORE_INSERT_EVENT, domainClazz);
        EventTriggerCaller preLoadEventCaller = buildCaller(AbstractPersistenceEvent.ONLOAD_EVENT, domainClazz);
        if (preLoadEventCaller == null) {
            this.preLoadEventCaller = buildCaller(AbstractPersistenceEvent.BEFORE_LOAD_EVENT, domainClazz);
        }
        else {
            this.preLoadEventCaller = preLoadEventCaller;
        }

        this.postLoadEventListener = buildCaller(AbstractPersistenceEvent.AFTER_LOAD_EVENT, domainClazz);
        this.postInsertEventListener = buildCaller(AbstractPersistenceEvent.AFTER_INSERT_EVENT, domainClazz);
        this.postUpdateEventListener = buildCaller(AbstractPersistenceEvent.AFTER_UPDATE_EVENT, domainClazz);
        this.postDeleteEventListener = buildCaller(AbstractPersistenceEvent.AFTER_DELETE_EVENT, domainClazz);
        this.preDeleteEventListener = buildCaller(AbstractPersistenceEvent.BEFORE_DELETE_EVENT, domainClazz);
        this.preUpdateEventListener = buildCaller(AbstractPersistenceEvent.BEFORE_UPDATE_EVENT, domainClazz);

        this.beforeValidateEventListener = new BeforeValidateEventTriggerCaller(domainClazz, this.domainMetaClass);

        if (failOnErrorPackages.size() > 0) {
            this.failOnErrorEnabled = ClassUtils.isClassBelowPackage(domainClazz, failOnErrorPackages);
        }
        else {
            this.failOnErrorEnabled = failOnError;
        }

        this.validateParams = new HashMap();
        this.validateParams.put(AbstractHibernateGormValidationApi.ARGUMENT_DEEP_VALIDATE, Boolean.FALSE);

        try {
            this.actionQueueUpdatesField = ReflectionUtils.findField(ActionQueue.class, "updates");
            this.actionQueueUpdatesField.setAccessible(true);
            this.entityUpdateActionStateField = ReflectionUtils.findField(EntityUpdateAction.class, "state");
            this.entityUpdateActionStateField.setAccessible(true);
        }
        catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
        // no-op, merely a hook for plugins to override
    }

    @Override
    public void onPreLoad(final PreLoadEvent event) {
        if (this.preLoadEventCaller == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {

            @Override
            public Object call() {
                ClosureEventListener.this.preLoadEventCaller.call(event.getEntity());
                return null;
            }

        });
    }

    @Override
    public void onPostLoad(final PostLoadEvent event) {
        if (this.postLoadEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {

            @Override
            public Object call() {
                ClosureEventListener.this.postLoadEventListener.call(event.getEntity());
                return null;
            }

        });
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        final Object entity = event.getEntity();
        if (this.postInsertEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {

            @Override
            public Object call() {
                ClosureEventListener.this.postInsertEventListener.call(entity);
                return null;
            }

        });
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    public void onPostUpdate(PostUpdateEvent event) {
        final Object entity = event.getEntity();
        if (this.postUpdateEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                ClosureEventListener.this.postUpdateEventListener.call(entity);
                return null;
            }
        });
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        final Object entity = event.getEntity();
        if (this.postDeleteEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {

            @Override
            public Object call() {
                ClosureEventListener.this.postDeleteEventListener.call(entity);
                return null;
            }

        });
    }

    @Override
    public boolean onPreDelete(final PreDeleteEvent event) {
        if (this.preDeleteEventListener == null) {
            return false;
        }

        return doWithManualSession(event, new Closure<Boolean>(this) {

            @Override
            public Boolean call() {
                return ClosureEventListener.this.preDeleteEventListener.call(event.getEntity());
            }

        });
    }

    @Override
    public boolean onPreUpdate(final PreUpdateEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {

            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean evict = false;
                if (ClosureEventListener.this.preUpdateEventListener != null) {
                    evict = ClosureEventListener.this.preUpdateEventListener.call(entity);
                    if (!evict) {
                        synchronizePersisterState(event, event.getState());
                    }
                }
                return evict || doValidate(entity);
            }

        });
    }

    public boolean onPreInsert(final PreInsertEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {

            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean synchronizeState = false;
                if (ClosureEventListener.this.beforeInsertCaller != null) {
                    if (ClosureEventListener.this.beforeInsertCaller.call(entity)) {
                        return true;
                    }
                    synchronizeState = true;
                }
                if (synchronizeState) {
                    synchronizePersisterState(event, event.getState());
                }
                return doValidate(entity);
            }

        });
    }

    public void onValidate(ValidationEvent event) {
        this.beforeValidateEventListener.call(event.getEntityObject(), event.getValidatedFields());
    }

    protected boolean doValidate(Object entity) {
        boolean evict = false;
        GormValidateable validateable = (GormValidateable) entity;
        if (!validateable.shouldSkipValidation() && !validateable.validate(this.validateParams)) {
            evict = true;
            if (this.failOnErrorEnabled) {
                Errors errors = validateable.getErrors();
                throw ValidationException.newInstance("Validation error whilst flushing entity [" + entity.getClass().getName()
                        + "]", errors);
            }
        }
        return evict;
    }

    private EventTriggerCaller buildCaller(String eventName, Class<?> domainClazz) {
        return EventTriggerCaller.buildCaller(eventName, domainClazz, this.domainMetaClass, null);
    }

    private void synchronizePersisterState(AbstractPreDatabaseOperationEvent event, Object[] state) {
        EntityPersister persister = event.getPersister();
        synchronizePersisterState(event, state, persister, persister.getPropertyNames());
    }

    private void synchronizePersisterState(AbstractPreDatabaseOperationEvent event, Object[] state,
            EntityPersister persister, String[] propertyNames) {
        Object entity = event.getEntity();
        EntityReflector reflector = this.persistentEntity.getReflector();
        HashMap<Integer, Object> changedState = new HashMap<>();
        EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
        for (int i = 0; i < propertyNames.length; i++) {
            String p = propertyNames[i];
            Integer index = entityMetamodel.getPropertyIndexOrNull(p);
            if (index == null) {
                continue;
            }

            PersistentProperty property = this.persistentEntity.getPropertyByName(p);
            if (property == null) {
                continue;
            }
            String propertyName = property.getName();

            if (GormProperties.VERSION.equals(propertyName)) {
                continue;
            }

            Object value = reflector.getProperty(entity, propertyName);
            if (state[index] != value) {
                changedState.put(i, value);
            }
            state[index] = value;
        }

        synchronizeEntityUpdateActionState(event, entity, changedState);
    }

    private void synchronizeEntityUpdateActionState(AbstractPreDatabaseOperationEvent event, Object entity,
            HashMap<Integer, Object> changedState) {
        if (this.actionQueueUpdatesField != null && event instanceof PreInsertEvent && changedState.size() > 0) {
            try {
                ExecutableList<EntityUpdateAction> updates =
                        (ExecutableList<EntityUpdateAction>) this.actionQueueUpdatesField.get(event.getSession().getActionQueue());
                if (updates != null) {
                    for (EntityUpdateAction updateAction : updates) {
                        if (updateAction.getInstance() == entity) {
                            Object[] updateState = (Object[]) this.entityUpdateActionStateField.get(updateAction);
                            if (updateState != null) {
                                for (Map.Entry<Integer, Object> entry : changedState.entrySet()) {
                                    updateState[entry.getKey()] = entry.getValue();
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Error synchronizing object state with Hibernate: " + e.getMessage(), e);
            }
        }
    }

    private <T> T doWithManualSession(AbstractEvent event, Closure<T> callable) {
        Session session = event.getSession();
        FlushMode current = session.getHibernateFlushMode();
        try {
            session.setHibernateFlushMode(FlushMode.MANUAL);
            return callable.call();
        }
        finally {
            session.setHibernateFlushMode(current);
        }
    }

}
