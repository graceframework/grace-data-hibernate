/*
 * Copyright 2010-2026 the original author or authors.
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
package org.grails.orm.hibernate.query;

import java.util.Iterator;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.PropertyMapping;

import grails.orm.HibernateCriteriaBuilder;
import grails.orm.RlikeExpression;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.orm.hibernate.AbstractHibernateSession;
import org.grails.orm.hibernate.GrailsHibernateTemplate;
import org.grails.orm.hibernate.HibernateSession;

/**
 * Bridges the Query API with the Hibernate Criteria API
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateQuery extends AbstractHibernateQuery {

    public static final HibernateCriterionAdapter HIBERNATE_CRITERION_ADAPTER = new HibernateCriterionAdapter();

    public HibernateQuery(org.hibernate.Criteria criteria, AbstractHibernateSession session, PersistentEntity entity) {
        super(criteria, session, entity);
    }

    public HibernateQuery(org.hibernate.Criteria criteria, PersistentEntity entity) {
        super(criteria, null, entity);
    }

    public HibernateQuery(org.hibernate.Criteria subCriteria, AbstractHibernateSession session, PersistentEntity associatedEntity, String newAlias) {
        super(subCriteria, session, associatedEntity, newAlias);
    }

    public HibernateQuery(org.hibernate.criterion.DetachedCriteria criteria, PersistentEntity entity) {
        super(criteria, entity);
    }

    /**
     * @return The hibernate criteria
     */
    public org.hibernate.Criteria getHibernateCriteria() {
        return this.criteria;
    }

    @Override
    protected AbstractHibernateCriterionAdapter createHibernateCriterionAdapter() {
        return HIBERNATE_CRITERION_ADAPTER;
    }

    protected org.hibernate.criterion.Criterion createRlikeExpression(String propertyName, String value) {
        return new RlikeExpression(propertyName, value);
    }

    protected void setDetachedCriteriaValue(QueryableCriteria value, PropertyCriterion pc) {
        org.hibernate.criterion.DetachedCriteria hibernateDetachedCriteria = HibernateCriteriaBuilder.getHibernateDetachedCriteria(this, value);
        pc.setValue(hibernateDetachedCriteria);
    }

    protected String render(org.hibernate.type.BasicType basic, List<String> columns, SessionFactory sessionFactory, SQLFunction sqlFunction) {
        return sqlFunction.render(basic, columns, (SessionFactoryImplementor) sessionFactory);
    }

    protected PropertyMapping getEntityPersister(String name, SessionFactory sessionFactory) {
        return (PropertyMapping) ((SessionFactoryImplementor) sessionFactory).getEntityPersister(name);
    }

    @Deprecated
    protected org.hibernate.type.TypeResolver getTypeResolver(SessionFactory sessionFactory) {
        return ((SessionFactoryImplementor) sessionFactory).getTypeResolver();
    }

    @Deprecated
    protected Dialect getDialect(SessionFactory sessionFactory) {
        return ((SessionFactoryImplementor) sessionFactory).getDialect();
    }

    @Override
    public Object clone() {
        final org.hibernate.internal.CriteriaImpl impl = (org.hibernate.internal.CriteriaImpl) criteria;
        final HibernateSession hibernateSession = (HibernateSession) getSession();
        final GrailsHibernateTemplate hibernateTemplate = (GrailsHibernateTemplate) hibernateSession.getNativeInterface();
        return hibernateTemplate.execute((GrailsHibernateTemplate.HibernateCallback<Object>) session -> {
            org.hibernate.Criteria newCriteria = session.createCriteria(impl.getEntityOrClassName());

            Iterator iterator = impl.iterateExpressionEntries();
            while (iterator.hasNext()) {
                org.hibernate.internal.CriteriaImpl.CriterionEntry entry = (org.hibernate.internal.CriteriaImpl.CriterionEntry) iterator.next();
                newCriteria.add(entry.getCriterion());
            }
            Iterator subcriteriaIterator = impl.iterateSubcriteria();
            while (subcriteriaIterator.hasNext()) {
                org.hibernate.internal.CriteriaImpl.Subcriteria sub = (org.hibernate.internal.CriteriaImpl.Subcriteria) subcriteriaIterator.next();
                newCriteria.createAlias(sub.getPath(), sub.getAlias(), sub.getJoinType(), sub.getWithClause());
            }
            return new HibernateQuery(newCriteria, hibernateSession, entity);
        });
    }

}
