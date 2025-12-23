/*
 * Copyright 2010-2025 the original author or authors.
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

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.SessionFactory;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Projections;
import org.grails.orm.hibernate.HibernateSession;

/**
 * Bridges the Query API with the Hibernate Criteria API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateQuery extends AbstractHibernateQuery {

    private HibernateSession hibernateSession;

    public HibernateQuery(HibernateSession session, PersistentEntity entity) {
        super(session, entity);
        this.hibernateSession = session;
    }

    @Override
    public List executeQuery(PersistentEntity entity, Junction criteria) {
        System.out.println("HibernateQuery.executeQuery>>" + entity.getJavaClass());
        SessionFactory sessionFactory = hibernateSession.getSessionFactory();
        CriteriaBuilder criteriaBuilder = sessionFactory.getCriteriaBuilder();

        if (projections().getProjectionList().contains(Projections.COUNT_PROJECTION)) {
            CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
            Root root = criteriaQuery.from(entity.getJavaClass());
            criteriaQuery.select(criteriaBuilder.count(root));
            return sessionFactory.openSession().createQuery(criteriaQuery).list();
        }
        else {
            CriteriaQuery<?> criteriaQuery = criteriaBuilder.createQuery(entity.getJavaClass());
            Root root = criteriaQuery.from(entity.getJavaClass());
            criteriaQuery.select(root);
            return sessionFactory.openSession().createQuery(criteriaQuery).list();
        }
    }

}
