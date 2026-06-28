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
package org.grails.orm.hibernate.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import grails.orm.RlikeExpression;
import org.grails.datastore.gorm.query.criteria.DetachedAssociationCriteria;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.query.criteria.FunctionCallingCriterion;

/**
 * Adapts Grails datastore Query API to Hibernate Criterion API.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
public class HibernateCriterionAdapter {

    protected static final Map<Class<?>, CriterionAdaptor<?>> criterionAdaptors = new HashMap<>();

    protected static boolean initialized;

    protected static final String ALIAS = "_alias";

    public HibernateCriterionAdapter() {
        initialize();
    }

    protected void initialize() {
        if (initialized) {
            return;
        }

        synchronized (criterionAdaptors) {
            // add simple property criterions (idEq, eq, ne, gt, lt, ge, le)
            addSimplePropertyCriterionAdapters();

            // add like operators (rlike, like, ilike)
            addLikeCriterionAdapters();

            //add simple size criterions (sizeEq, sizeGt, sizeLt, sizeGe, sizeLe)
            addSizeComparisonCriterionAdapters();

            //add simple criterions (isNull, isNotNull, isEmpty, isNotEmpty)
            addSimpleCriterionAdapters();

            //add simple property comparison criterions (eqProperty, neProperty, gtProperty, geProperty, ltProperty, leProperty)
            addPropertyComparisonCriterionAdapters();

            // add range queries (in, between)
            addRangeQueryCriterionAdapters();

            // add subquery adapters (gtAll, geAll, gtSome, ltAll, leAll)
            addSubqueryCriterionAdapters();

            // add junctions (conjunction, disjunction, negation)
            addJunctionCriterionAdapters();

            // add association query adapters
            addAssociationQueryCriterionAdapters();
        }

        initialized = true;
    }

    protected void addSubqueryCriterionAdapters() {
        criterionAdaptors.put(Query.GreaterThanAll.class, new CriterionAdaptor<Query.GreaterThanAll>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanAll criterion, String alias) {
                QueryableCriteria subQuery = criterion.getValue();
                String propertyName = getPropertyName(criterion, alias);
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, subQuery);
                return org.hibernate.criterion.Property.forName(propertyName).gtAll(detachedCriteria);
            }
        });

        criterionAdaptors.put(Query.GreaterThanEqualsAll.class, new CriterionAdaptor<Query.GreaterThanEqualsAll>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanEqualsAll criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).geAll(detachedCriteria);
            }
        });
        criterionAdaptors.put(Query.LessThanAll.class, new CriterionAdaptor<Query.LessThanAll>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanAll criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).ltAll(detachedCriteria);
            }
        });
        criterionAdaptors.put(Query.LessThanEqualsAll.class, new CriterionAdaptor<Query.LessThanEqualsAll>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanEqualsAll criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).leAll(detachedCriteria);
            }
        });

        criterionAdaptors.put(Query.GreaterThanSome.class, new CriterionAdaptor<Query.GreaterThanSome>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanSome criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).gtSome(detachedCriteria);
            }
        });
        criterionAdaptors.put(Query.GreaterThanEqualsSome.class, new CriterionAdaptor<Query.GreaterThanEqualsSome>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanEqualsSome criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).geSome(detachedCriteria);
            }
        });
        criterionAdaptors.put(Query.LessThanSome.class, new CriterionAdaptor<Query.LessThanSome>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanSome criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).ltSome(detachedCriteria);
            }
        });
        criterionAdaptors.put(Query.LessThanEqualsSome.class, new CriterionAdaptor<Query.LessThanEqualsSome>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanEqualsSome criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getValue());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).leSome(detachedCriteria);
            }
        });

        criterionAdaptors.put(Query.NotIn.class, new CriterionAdaptor<Query.NotIn>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.NotIn criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getSubquery());
                return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).notIn(detachedCriteria);
            }
        });

        criterionAdaptors.put(Query.Exists.class, new CriterionAdaptor<Query.Exists>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Exists criterion, String alias) {
                final QueryableCriteria subquery = criterion.getSubquery();
                String subqueryAlias = subquery.getAlias();
                if (subquery.getAlias() == null) {
                    subqueryAlias = criterion.getSubquery().getPersistentEntity().getJavaClass().getSimpleName() + ALIAS;
                }
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, subquery, subqueryAlias);
                return org.hibernate.criterion.Subqueries.exists(detachedCriteria);
            }
        });

        criterionAdaptors.put(Query.NotExists.class, new CriterionAdaptor<Query.NotExists>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.NotExists criterion, String alias) {
                org.hibernate.criterion.DetachedCriteria detachedCriteria = toHibernateDetachedCriteria(hibernateQuery, criterion.getSubquery());
                return org.hibernate.criterion.Subqueries.notExists(detachedCriteria);
            }
        });
    }

    protected void addAssociationQueryCriterionAdapters() {
        criterionAdaptors.put(DetachedAssociationCriteria.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                DetachedAssociationCriteria<?> existing = (DetachedAssociationCriteria<?>) criterion;
                alias = hibernateQuery.handleAssociationQuery(existing.getAssociation(), existing.getCriteria());
                Association association = existing.getAssociation();
                hibernateQuery.associationStack.add(association);
                org.hibernate.criterion.Junction conjunction = org.hibernate.criterion.Restrictions.conjunction();
                try {
                    applySubCriteriaToJunction(association.getAssociatedEntity(), hibernateQuery, existing.getCriteria(), conjunction, alias);
                    return conjunction;
                }
                finally {
                    hibernateQuery.associationStack.removeLast();
                }
            }
        });
        criterionAdaptors.put(AssociationQuery.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                AssociationQuery existing = (AssociationQuery) criterion;
                org.hibernate.criterion.Junction conjunction = org.hibernate.criterion.Restrictions.conjunction();
                String newAlias = hibernateQuery.handleAssociationQuery(existing.getAssociation(), existing.getCriteria().getCriteria());
                if (alias == null) {
                    alias = newAlias;
                }
                else {
                    alias += '.' + newAlias;
                }
                applySubCriteriaToJunction(existing.getAssociation().getAssociatedEntity(), hibernateQuery,
                        existing.getCriteria().getCriteria(), conjunction, alias);
                return conjunction;
            }
        });
    }

    protected void addJunctionCriterionAdapters() {
        criterionAdaptors.put(Query.Conjunction.class, new CriterionAdaptor<Query.Conjunction>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Conjunction criterion, String alias) {
                org.hibernate.criterion.Conjunction conjunction = org.hibernate.criterion.Restrictions.conjunction();
                applySubCriteriaToJunction(hibernateQuery.getEntity(), hibernateQuery, criterion.getCriteria(), conjunction, alias);
                return conjunction;
            }
        });
        criterionAdaptors.put(Query.Disjunction.class, new CriterionAdaptor<Query.Disjunction>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Disjunction criterion, String alias) {
                org.hibernate.criterion.Disjunction disjunction = org.hibernate.criterion.Restrictions.disjunction();
                applySubCriteriaToJunction(hibernateQuery.getEntity(), hibernateQuery, criterion.getCriteria(), disjunction, alias);
                return disjunction;
            }
        });
        criterionAdaptors.put(Query.Negation.class, new CriterionAdaptor<Query.Negation>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Negation criterion, String alias) {
                CriterionAdaptor<Query.Disjunction> adapter = (CriterionAdaptor<Query.Disjunction>) criterionAdaptors.get(Query.Disjunction.class);
                return org.hibernate.criterion.Restrictions.not(adapter.toHibernateCriterion(hibernateQuery, new Query.Disjunction(criterion.getCriteria()), alias));
            }
        });
    }

    protected void addRangeQueryCriterionAdapters() {
        criterionAdaptors.put(Query.Between.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Between btwCriterion = (Query.Between) criterion;
                return org.hibernate.criterion.Restrictions.between(calculatePropertyName(btwCriterion.getProperty(), alias), btwCriterion.getFrom(), btwCriterion.getTo());
            }
        });

        criterionAdaptors.put(Query.In.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.In inListQuery = (Query.In) criterion;
                QueryableCriteria subquery = inListQuery.getSubquery();
                if (subquery != null) {
                    return org.hibernate.criterion.Property.forName(getPropertyName(criterion, alias)).in(toHibernateDetachedCriteria(hibernateQuery, subquery));
                }
                else {
                    return org.hibernate.criterion.Restrictions.in(getPropertyName(criterion, alias), inListQuery.getValues());
                }
            }
        });
    }

    protected void addLikeCriterionAdapters() {
        criterionAdaptors.put(Query.RLike.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return createRlikeExpression(getPropertyName(criterion, alias), ((Query.RLike) criterion).getPattern());
            }
        });
        criterionAdaptors.put(Query.Like.class, new CriterionAdaptor<Query.Like>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Like criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                return org.hibernate.criterion.Restrictions.like(propertyName, value);
            }
        });
        criterionAdaptors.put(Query.ILike.class, new CriterionAdaptor<Query.ILike>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.ILike criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                return org.hibernate.criterion.Restrictions.ilike(propertyName, value);
            }
        });
    }

    protected void addPropertyComparisonCriterionAdapters() {
        criterionAdaptors.put(Query.EqualsProperty.class, new CriterionAdaptor<Query.EqualsProperty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.EqualsProperty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.eqProperty(propertyName, criterion.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.GreaterThanProperty.class, new CriterionAdaptor<Query.GreaterThanProperty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanProperty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.gtProperty(propertyName, criterion.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.GreaterThanEqualsProperty.class, new CriterionAdaptor<Query.GreaterThanEqualsProperty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanEqualsProperty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.geProperty(propertyName, criterion.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.LessThanProperty.class, new CriterionAdaptor<Query.LessThanProperty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanProperty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.ltProperty(propertyName, criterion.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.LessThanEqualsProperty.class, new CriterionAdaptor<Query.LessThanEqualsProperty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanEqualsProperty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.leProperty(propertyName, criterion.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.NotEqualsProperty.class, new CriterionAdaptor<Query.NotEqualsProperty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.NotEqualsProperty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.neProperty(propertyName, criterion.getOtherProperty());
            }
        });
    }

    protected void addSimpleCriterionAdapters() {
        criterionAdaptors.put(Query.IsNull.class, new CriterionAdaptor<Query.IsNull>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.IsNull criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.isNull(propertyName);
            }
        });
        criterionAdaptors.put(Query.IsNotNull.class, new CriterionAdaptor<Query.IsNotNull>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.IsNotNull criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.isNotNull(propertyName);
            }
        });
        criterionAdaptors.put(Query.IsEmpty.class, new CriterionAdaptor<Query.IsEmpty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.IsEmpty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.isEmpty(propertyName);
            }
        });
        criterionAdaptors.put(Query.IsNotEmpty.class, new CriterionAdaptor<Query.IsNotEmpty>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.IsNotEmpty criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                return org.hibernate.criterion.Restrictions.isNotEmpty(propertyName);
            }
        });
    }

    protected void addSizeComparisonCriterionAdapters() {
        criterionAdaptors.put(Query.SizeEquals.class, new CriterionAdaptor<Query.SizeEquals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.SizeEquals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                int size = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
                return org.hibernate.criterion.Restrictions.sizeEq(propertyName, size);
            }
        });

        criterionAdaptors.put(Query.SizeGreaterThan.class, new CriterionAdaptor<Query.SizeGreaterThan>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.SizeGreaterThan criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                int size = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
                return org.hibernate.criterion.Restrictions.sizeGt(propertyName, size);
            }
        });

        criterionAdaptors.put(Query.SizeGreaterThanEquals.class, new CriterionAdaptor<Query.SizeGreaterThanEquals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.SizeGreaterThanEquals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                int size = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
                return org.hibernate.criterion.Restrictions.sizeGe(propertyName, size);
            }
        });

        criterionAdaptors.put(Query.SizeLessThan.class, new CriterionAdaptor<Query.SizeLessThan>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.SizeLessThan criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                int size = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
                return org.hibernate.criterion.Restrictions.sizeLt(propertyName, size);
            }
        });

        criterionAdaptors.put(Query.SizeLessThanEquals.class, new CriterionAdaptor<Query.SizeLessThanEquals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.SizeLessThanEquals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                int size = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
                return org.hibernate.criterion.Restrictions.sizeLe(propertyName, size);
            }
        });
    }

    protected void addSimplePropertyCriterionAdapters() {
        criterionAdaptors.put(Query.IdEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return org.hibernate.criterion.Restrictions.idEq(((Query.IdEquals) criterion).getValue());
            }
        });
        criterionAdaptors.put(Query.Equals.class, new CriterionAdaptor<Query.Equals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Equals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                if (value instanceof org.hibernate.criterion.DetachedCriteria) {
                    return org.hibernate.criterion.Property.forName(propertyName).eq((org.hibernate.criterion.DetachedCriteria) value);
                }
                return org.hibernate.criterion.Restrictions.eq(propertyName, value);
            }
        });
        criterionAdaptors.put(Query.NotEquals.class, new CriterionAdaptor<Query.NotEquals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.NotEquals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                if (value instanceof org.hibernate.criterion.DetachedCriteria) {
                    return org.hibernate.criterion.Property.forName(propertyName).ne((org.hibernate.criterion.DetachedCriteria) value);
                }
                return org.hibernate.criterion.Restrictions.ne(propertyName, value);
            }
        });
        criterionAdaptors.put(Query.GreaterThan.class, new CriterionAdaptor<Query.GreaterThan>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThan criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                if (value instanceof org.hibernate.criterion.DetachedCriteria) {
                    return org.hibernate.criterion.Property.forName(propertyName).gt((org.hibernate.criterion.DetachedCriteria) value);
                }
                return org.hibernate.criterion.Restrictions.gt(propertyName, value);
            }
        });
        criterionAdaptors.put(Query.GreaterThanEquals.class, new CriterionAdaptor<Query.GreaterThanEquals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.GreaterThanEquals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                if (value instanceof org.hibernate.criterion.DetachedCriteria) {
                    return org.hibernate.criterion.Property.forName(propertyName).ge((org.hibernate.criterion.DetachedCriteria) value);
                }
                return org.hibernate.criterion.Restrictions.ge(propertyName, value);
            }
        });
        criterionAdaptors.put(Query.LessThan.class, new CriterionAdaptor<Query.LessThan>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThan criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                if (value instanceof org.hibernate.criterion.DetachedCriteria) {
                    return org.hibernate.criterion.Property.forName(propertyName).lt((org.hibernate.criterion.DetachedCriteria) value);
                }
                return org.hibernate.criterion.Restrictions.lt(propertyName, value);
            }
        });
        criterionAdaptors.put(Query.LessThanEquals.class, new CriterionAdaptor<Query.LessThanEquals>() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.LessThanEquals criterion, String alias) {
                String propertyName = getPropertyName(criterion, alias);
                Object value = criterion.getValue();
                if (value instanceof org.hibernate.criterion.DetachedCriteria) {
                    return org.hibernate.criterion.Property.forName(propertyName).le((org.hibernate.criterion.DetachedCriteria) value);
                }
                return org.hibernate.criterion.Restrictions.le(propertyName, value);
            }
        });
    }

    /** utility methods to group and clean up the initialization of the Criterion Adapters**/
    protected org.hibernate.criterion.Criterion createRlikeExpression(String propertyName, String pattern) {
        return new RlikeExpression(propertyName, pattern);
    }

    protected String getPropertyName(Query.Criterion criterion, String alias) {
        return calculatePropertyName(((Query.PropertyNameCriterion) criterion).getProperty(), alias);
    }

    protected String calculatePropertyName(String property, String alias) {
        if (alias != null) {
            return alias + '.' + property;
        }
        return property;
    }

    protected void applySubCriteriaToJunction(PersistentEntity entity, AbstractHibernateQuery hibernateCriteria, List<Query.Criterion> existing,
            org.hibernate.criterion.Junction conjunction, String alias) {
        for (Query.Criterion subCriterion : existing) {
            if (subCriterion instanceof Query.PropertyCriterion) {
                Query.PropertyCriterion pc = (Query.PropertyCriterion) subCriterion;
                if (pc.getValue() instanceof QueryableCriteria) {
                    pc.setValue(toHibernateDetachedCriteria(hibernateCriteria, (QueryableCriteria<?>) pc.getValue()));
                }
                else {
                    AbstractHibernateQuery.doTypeConversionIfNeccessary(entity, pc);
                }
            }
            CriterionAdaptor criterionAdaptor = criterionAdaptors.get(subCriterion.getClass());
            if (criterionAdaptor != null) {
                org.hibernate.criterion.Criterion c = criterionAdaptor.toHibernateCriterion(hibernateCriteria, subCriterion, alias);
                if (c != null) {
                    conjunction.add(c);
                }
            }
            else if (subCriterion instanceof FunctionCallingCriterion) {
                org.hibernate.criterion.Criterion sqlRestriction = hibernateCriteria.getRestrictionForFunctionCall((FunctionCallingCriterion) subCriterion, entity);
                if (sqlRestriction != null) {
                    conjunction.add(sqlRestriction);
                }
            }
        }
    }

    public org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
        final CriterionAdaptor criterionAdaptor = criterionAdaptors.get(criterion.getClass());
        if (criterionAdaptor != null) {
            return criterionAdaptor.toHibernateCriterion(hibernateQuery, criterion, alias);
        }
        return null;
    }

    protected org.hibernate.criterion.DetachedCriteria toHibernateDetachedCriteria(AbstractHibernateQuery hibernateQuery, QueryableCriteria<?> queryableCriteria) {
        return AbstractHibernateCriteriaBuilder.getHibernateDetachedCriteria(hibernateQuery, queryableCriteria);
    }

    protected org.hibernate.criterion.DetachedCriteria toHibernateDetachedCriteria(AbstractHibernateQuery hibernateQuery, QueryableCriteria<?> queryableCriteria,
            String alias) {
        if (alias == null) {
            return toHibernateDetachedCriteria(hibernateQuery, queryableCriteria);
        }
        return AbstractHibernateCriteriaBuilder.getHibernateDetachedCriteria(hibernateQuery, queryableCriteria, alias);
    }

    public abstract static class CriterionAdaptor<T extends Query.Criterion> {

        public abstract org.hibernate.criterion.Criterion toHibernateCriterion(AbstractHibernateQuery hibernateQuery, T criterion, String alias);

        protected Object convertStringValue(Object o) {
            if ((!(o instanceof String)) && (o instanceof CharSequence)) {
                o = o.toString();
            }
            return o;
        }

    }

}
