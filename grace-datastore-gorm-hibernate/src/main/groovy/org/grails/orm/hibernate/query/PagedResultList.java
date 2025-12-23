/*
 * Copyright 2018-2025 the original author or authors.
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

import java.sql.SQLException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.orm.hibernate.GrailsHibernateTemplate;

public class PagedResultList<E> extends grails.gorm.PagedResultList<E> {

    private final CriteriaQuery<E> criteriaQuery;

    private final Root<E> queryRoot;

    private final CriteriaBuilder criteriaBuilder;

    private final PersistentEntity entity;

    private transient GrailsHibernateTemplate hibernateTemplate;

    public PagedResultList(GrailsHibernateTemplate template,
            PersistentEntity entity,
            HibernateHqlQuery hibernateHqlQuery,
            CriteriaQuery<E> criteriaQuery,
            Root<E> queryRoot,
            CriteriaBuilder criteriaBuilder) {
        super(hibernateHqlQuery);
        this.hibernateTemplate = template;
        this.criteriaQuery = criteriaQuery;
        this.queryRoot = queryRoot;
        this.criteriaBuilder = criteriaBuilder;
        this.entity = entity;
    }

    @Override
    protected void initialize() {
        // no-op, already initialized
    }

    @Override
    public int getTotalCount() {
        if (totalCount == Integer.MIN_VALUE) {
            totalCount = this.hibernateTemplate.execute(new GrailsHibernateTemplate.HibernateCallback<Integer>() {

                @Override
                public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                    PagedResultList.this.criteriaQuery.select(PagedResultList.this.queryRoot);
                    final Query<?> query = session.createQuery(PagedResultList.this.criteriaQuery);
                    PagedResultList.this.hibernateTemplate.applySettings(query);
                    return Long.valueOf(query.getResultCount()).intValue();
                }

            });
        }
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

}
