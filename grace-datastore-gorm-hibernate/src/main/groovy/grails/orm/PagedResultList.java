/*
 * Copyright 2016-2026 the original author or authors.
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
package grails.orm;

import java.sql.SQLException;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.grails.orm.hibernate.GrailsHibernateTemplate;
import org.grails.orm.hibernate.query.HibernateQuery;

/**
 * A result list for Criteria list calls, which is aware of the totalCount for
 * the paged result.
 *
 * @author Siegfried Puchbauer
 * @author Michael Yan
 * @since 1.0
 * @deprecated Use {@link org.grails.orm.hibernate.query.PagedResultList} instead.
 */
@SuppressWarnings({ "rawtypes" })
@Deprecated
public class PagedResultList extends grails.gorm.PagedResultList {

    private transient GrailsHibernateTemplate hibernateTemplate;

    private final org.hibernate.Criteria criteria;

    public PagedResultList(GrailsHibernateTemplate template, org.hibernate.Criteria crit) {
        super(null);
        resultList = crit.list();
        this.criteria = crit;
        this.hibernateTemplate = template;
    }

    public PagedResultList(GrailsHibernateTemplate template, HibernateQuery query) {
        super(null);
        resultList = query.listForCriteria();
        this.criteria = query.getHibernateCriteria();
        this.hibernateTemplate = template;
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
                    org.hibernate.internal.CriteriaImpl impl = (org.hibernate.internal.CriteriaImpl) PagedResultList.this.criteria;
                    org.hibernate.Criteria totalCriteria = session.createCriteria(impl.getEntityOrClassName());
                    PagedResultList.this.hibernateTemplate.applySettings(totalCriteria);

                    Iterator iterator = impl.iterateExpressionEntries();
                    while (iterator.hasNext()) {
                        org.hibernate.internal.CriteriaImpl.CriterionEntry entry = (org.hibernate.internal.CriteriaImpl.CriterionEntry) iterator.next();
                        totalCriteria.add(entry.getCriterion());
                    }
                    Iterator subcriteriaIterator = impl.iterateSubcriteria();
                    while (subcriteriaIterator.hasNext()) {
                        org.hibernate.internal.CriteriaImpl.Subcriteria sub = (org.hibernate.internal.CriteriaImpl.Subcriteria) subcriteriaIterator.next();
                        totalCriteria.createAlias(sub.getPath(), sub.getAlias(), sub.getJoinType(), sub.getWithClause());
                    }
                    totalCriteria.setProjection(impl.getProjection());
                    totalCriteria.setProjection(org.hibernate.criterion.Projections.rowCount());
                    return ((Number) totalCriteria.uniqueResult()).intValue();
                }

            });
        }
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

}
