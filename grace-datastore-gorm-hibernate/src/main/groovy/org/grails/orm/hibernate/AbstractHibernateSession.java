/*
 * Copyright 2011-2025 the original author or authors.
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
package org.grails.orm.hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.grails.datastore.mapping.core.AbstractAttributeStoringSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.query.api.QueryAliasAwareSession;
import org.grails.datastore.mapping.transactions.Transaction;

/**
 * Session implementation that wraps a Hibernate {@link Session}.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractHibernateSession extends AbstractAttributeStoringSession implements QueryAliasAwareSession {

    protected AbstractHibernateDatastore datastore;

    protected boolean connected = true;

    protected IHibernateTemplate hibernateTemplate;

    protected AbstractHibernateSession(AbstractHibernateDatastore hibernateDatastore, SessionFactory sessionFactory) {
        this.datastore = hibernateDatastore;
    }

    @Override
    public boolean isSchemaless() {
        return false;
    }

    public Serializable insert(Object o) {
        return persist(o);
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void disconnect() {
        this.connected = false; // don't actually do any disconnection here. This will be handled by OSVI
    }

    @Override
    public Transaction beginTransaction() {
        throw new UnsupportedOperationException("Use HibernatePlatformTransactionManager instead");
    }

    @Override
    public Transaction beginTransaction(TransactionDefinition definition) {
        throw new UnsupportedOperationException("Use HibernatePlatformTransactionManager instead");
    }

    @Override
    public MappingContext getMappingContext() {
        return getDatastore().getMappingContext();
    }

    @Override
    public Serializable persist(Object o) {
        return this.hibernateTemplate.save(o);
    }

    @Override
    public void refresh(Object o) {
        this.hibernateTemplate.refresh(o);
    }

    @Override
    public void attach(Object o) {
        this.hibernateTemplate.lock(o, LockMode.NONE);
    }

    @Override
    public void flush() {
        this.hibernateTemplate.flush();
    }

    @Override
    public void clear() {
        this.hibernateTemplate.clear();
    }

    @Override
    public void clear(Object o) {
        this.hibernateTemplate.evict(o);
    }

    @Override
    public boolean contains(Object o) {
        return this.hibernateTemplate.contains(o);
    }

    @Override
    public void lock(Object o) {
        this.hibernateTemplate.lock(o, LockMode.PESSIMISTIC_WRITE);
    }

    @Override
    public void unlock(Object o) {
        // do nothing
    }

    @Override
    public List<Serializable> persist(Iterable objects) {
        List<Serializable> identifiers = new ArrayList<>();
        for (Object object : objects) {
            identifiers.add(this.hibernateTemplate.save(object));
        }
        return identifiers;
    }

    @Override
    public <T> T retrieve(Class<T> type, Serializable key) {
        return this.hibernateTemplate.get(type, key);
    }

    @Override
    public <T> T proxy(Class<T> type, Serializable key) {
        return this.hibernateTemplate.load(type, key);
    }

    @Override
    public <T> T lock(Class<T> type, Serializable key) {
        return this.hibernateTemplate.get(type, key, LockMode.PESSIMISTIC_WRITE);
    }

    @Override
    public void delete(Iterable objects) {
        Collection list = getIterableAsCollection(objects);
        this.hibernateTemplate.deleteAll(list);
    }

    @SuppressWarnings("unchecked")
    protected Collection getIterableAsCollection(Iterable objects) {
        Collection list;
        if (objects instanceof Collection) {
            list = (Collection) objects;
        }
        else {
            list = new ArrayList();
            for (Object object : objects) {
                list.add(object);
            }
        }
        return list;
    }

    @Override
    public void delete(Object obj) {
        this.hibernateTemplate.delete(obj);
    }

    @Override
    public List retrieveAll(Class type, Serializable... keys) {
        return retrieveAll(type, Arrays.asList(keys));
    }

    @Override
    public Persister getPersister(Object o) {
        return null;
    }

    @Override
    public Transaction getTransaction() {
        throw new UnsupportedOperationException("Use HibernatePlatformTransactionManager instead");
    }

    @Override
    public boolean hasTransaction() {
        Object resource = TransactionSynchronizationManager.getResource(this.hibernateTemplate.getSessionFactory());
        return resource != null;
    }

    @Override
    public Datastore getDatastore() {
        return this.datastore;
    }

    @Override
    public boolean isDirty(Object o) {
        // not used, Hibernate manages dirty checking itself
        return true;
    }

    @Override
    public Object getNativeInterface() {
        return this.hibernateTemplate;
    }

    @Override
    public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
        // no-op
    }

}
