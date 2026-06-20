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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;

import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Metamodel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.TypeHelper;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;

import grails.gorm.DetachedCriteria;
import grails.gorm.MultiTenant;

import org.grails.datastore.mapping.multitenancy.MultiTenancySettings;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.BuildableCriteria;
import org.grails.datastore.mapping.query.api.Criteria;
import org.grails.datastore.mapping.query.api.ProjectionList;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.reflect.NameUtils;
import org.grails.orm.hibernate.AbstractHibernateDatastore;

/**
 * Abstract super class for sharing code between Hibernate 3 and 4 implementations of HibernateCriteriaBuilder
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0.7
 */
public abstract class AbstractHibernateCriteriaBuilder extends GroovyObjectSupport
        implements BuildableCriteria, ProjectionList {

    public static final String AND = "and"; // builder

    public static final String IS_NULL = "isNull"; // builder

    public static final String IS_NOT_NULL = "isNotNull"; // builder

    public static final String NOT = "not"; // builder

    public static final String OR = "or"; // builder

    public static final String ID_EQUALS = "idEq"; // builder

    public static final String IS_EMPTY = "isEmpty"; //builder

    public static final String IS_NOT_EMPTY = "isNotEmpty"; //builder

    public static final String RLIKE = "rlike"; //method

    public static final String BETWEEN = "between"; //method

    public static final String EQUALS = "eq"; //method

    public static final String EQUALS_PROPERTY = "eqProperty"; //method

    public static final String GREATER_THAN = "gt"; //method

    public static final String GREATER_THAN_PROPERTY = "gtProperty"; //method

    public static final String GREATER_THAN_OR_EQUAL = "ge"; //method

    public static final String GREATER_THAN_OR_EQUAL_PROPERTY = "geProperty"; //method

    public static final String ILIKE = "ilike"; //method

    public static final String IN = "in"; //method

    public static final String LESS_THAN = "lt"; //method

    public static final String LESS_THAN_PROPERTY = "ltProperty"; //method

    public static final String LESS_THAN_OR_EQUAL = "le"; //method

    public static final String LESS_THAN_OR_EQUAL_PROPERTY = "leProperty"; //method

    public static final String LIKE = "like"; //method

    public static final String NOT_EQUAL = "ne"; //method

    public static final String NOT_EQUAL_PROPERTY = "neProperty"; //method

    public static final String SIZE_EQUALS = "sizeEq"; //method

    public static final String ORDER_DESCENDING = "desc";

    public static final String ORDER_ASCENDING = "asc";

    protected static final String ROOT_DO_CALL = "doCall";

    protected static final String ROOT_CALL = "call";

    protected static final String LIST_CALL = "list";

    protected static final String LIST_DISTINCT_CALL = "listDistinct";

    protected static final String COUNT_CALL = "count";

    protected static final String GET_CALL = "get";

    protected static final String SCROLL_CALL = "scroll";

    protected static final String SET_RESULT_TRANSFORMER_CALL = "setResultTransformer";

    protected static final String PROJECTIONS = "projections";

    protected SessionFactory sessionFactory;

    protected Session hibernateSession;

    protected Class<?> targetClass;

    protected org.hibernate.Criteria criteria;

    protected MetaClass criteriaMetaClass;

    protected boolean uniqueResult = false;

    protected List<LogicalExpression> logicalExpressionStack = new ArrayList<>();

    protected List<String> associationStack = new ArrayList<>();

    protected boolean participate;

    protected boolean scroll;

    protected boolean count;

    protected org.hibernate.criterion.ProjectionList projectionList = org.hibernate.criterion.Projections.projectionList();

    protected List<String> aliasStack = new ArrayList<>();

    protected List<org.hibernate.Criteria> aliasInstanceStack = new ArrayList<>();

    protected Map<String, String> aliasMap = new HashMap<>();

    protected static final String ALIAS = "_alias";

    protected ResultTransformer resultTransformer;

    protected int aliasCount;

    protected boolean paginationEnabledList = false;

    protected List<org.hibernate.criterion.Order> orderEntries;

    protected ConversionService conversionService;

    protected int defaultFlushMode;

    protected AbstractHibernateDatastore datastore;

    @SuppressWarnings("rawtypes")
    public AbstractHibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory) {
        this.targetClass = targetClass;
        this.sessionFactory = sessionFactory;
    }

    @SuppressWarnings("rawtypes")
    public AbstractHibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory, boolean uniqueResult) {
        this.targetClass = targetClass;
        this.sessionFactory = sessionFactory;
        this.uniqueResult = uniqueResult;
    }

    public void setDatastore(AbstractHibernateDatastore datastore) {
        this.datastore = datastore;
        if (MultiTenant.class.isAssignableFrom(this.targetClass) &&
                datastore.getMultiTenancyMode() == MultiTenancySettings.MultiTenancyMode.DISCRIMINATOR) {
            datastore.enableMultiTenancyFilter();
        }
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * A projection that selects a property name
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList property(String propertyName) {
        return property(propertyName, null);
    }

    /**
     * A projection that selects a property name
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public ProjectionList property(String propertyName, String alias) {
        final org.hibernate.criterion.PropertyProjection propertyProjection = org.hibernate.criterion.Projections.property(calculatePropertyName(propertyName));
        addProjectionToList(propertyProjection, alias);
        return this;
    }

    /**
     * Adds a projection to the projectList for the given alias
     *
     * @param propertyProjection The projection
     * @param alias              The alias
     */
    protected void addProjectionToList(org.hibernate.criterion.Projection propertyProjection, String alias) {
        if (alias != null) {
            this.projectionList.add(propertyProjection, alias);
        }
        else {
            this.projectionList.add(propertyProjection);
        }
    }

    /**
     * Adds a sql projection to the criteria
     *
     * @param sql         SQL projecting a single value
     * @param columnAlias column alias for the projected value
     * @param type        the type of the projected value
     */
    protected void sqlProjection(String sql, String columnAlias, Type type) {
        sqlProjection(sql, Collections.singletonList(columnAlias), Collections.singletonList(type));
    }

    /**
     * Adds a sql projection to the criteria
     *
     * @param sql           SQL projecting
     * @param columnAliases List of column aliases for the projected values
     * @param types         List of types for the projected values
     */
    protected void sqlProjection(String sql, List<String> columnAliases, List<Type> types) {
        this.projectionList.add(org.hibernate.criterion.Projections.sqlProjection(sql, columnAliases.toArray(new String[0]), types.toArray(new Type[0])));
    }

    /**
     * Adds a sql projection to the criteria
     *
     * @param sql           SQL projecting
     * @param groupBy       group by clause
     * @param columnAliases List of column aliases for the projected values
     * @param types         List of types for the projected values
     */
    protected void sqlGroupProjection(String sql, String groupBy, List<String> columnAliases, List<Type> types) {
        this.projectionList.add(org.hibernate.criterion.Projections.sqlGroupProjection(sql, groupBy, columnAliases.toArray(new String[0]), types.toArray(new Type[0])));
    }

    /**
     * A projection that selects a distince property name
     *
     * @param propertyName The property name
     */
    public ProjectionList distinct(String propertyName) {
        distinct(propertyName, null);
        return this;
    }

    /**
     * A projection that selects a distince property name
     *
     * @param propertyName The property name
     * @param alias        The alias to use
     */
    public ProjectionList distinct(String propertyName, String alias) {
        final org.hibernate.criterion.Projection proj = org.hibernate.criterion.Projections.distinct(org.hibernate.criterion.Projections.property(calculatePropertyName(propertyName)));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * A distinct projection that takes a list
     *
     * @param propertyNames The list of distince property names
     */
    @SuppressWarnings("rawtypes")
    public ProjectionList distinct(Collection propertyNames) {
        return distinct(propertyNames, null);
    }

    /**
     * A distinct projection that takes a list
     *
     * @param propertyNames The list of distince property names
     * @param alias         The alias to use
     */
    @SuppressWarnings("rawtypes")
    public ProjectionList distinct(Collection propertyNames, String alias) {
        org.hibernate.criterion.ProjectionList list = org.hibernate.criterion.Projections.projectionList();
        for (Object o : propertyNames) {
            list.add(org.hibernate.criterion.Projections.property(calculatePropertyName(o.toString())));
        }
        final org.hibernate.criterion.Projection proj = org.hibernate.criterion.Projections.distinct(list);
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to return the property average value
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList avg(String propertyName) {
        return avg(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to return the property average value
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public ProjectionList avg(String propertyName, String alias) {
        final org.hibernate.criterion.AggregateProjection aggregateProjection = org.hibernate.criterion.Projections.avg(calculatePropertyName(propertyName));
        addProjectionToList(aggregateProjection, alias);
        return this;
    }

    /**
     * Use a join query
     *
     * @param associationPath The path of the association
     */
    @Override
    public BuildableCriteria join(String associationPath) {
        this.criteria.setFetchMode(calculatePropertyName(associationPath), FetchMode.JOIN);
        return this;
    }

    @Override
    public BuildableCriteria join(String property, JoinType joinType) {
        this.criteria.setFetchMode(calculatePropertyName(property), FetchMode.JOIN);
        return this;
    }

    /**
     * Whether a pessimistic lock should be obtained.
     *
     * @param shouldLock True if it should
     */
    public void lock(boolean shouldLock) {
        String lastAlias = getLastAlias();

        if (shouldLock) {
            if (lastAlias != null) {
                this.criteria.setLockMode(lastAlias, LockMode.PESSIMISTIC_WRITE);
            }
            else {
                this.criteria.setLockMode(LockMode.PESSIMISTIC_WRITE);
            }
        }
        else {
            if (lastAlias != null) {
                this.criteria.setLockMode(lastAlias, LockMode.NONE);
            }
            else {
                this.criteria.setLockMode(LockMode.NONE);
            }
        }
    }

    /**
     * Use a select query
     *
     * @param associationPath The path of the association
     */
    @Override
    public BuildableCriteria select(String associationPath) {
        this.criteria.setFetchMode(calculatePropertyName(associationPath), FetchMode.SELECT);
        return this;
    }

    /**
     * Whether to use the query cache
     *
     * @param shouldCache True if the query should be cached
     */
    @Override
    public BuildableCriteria cache(boolean shouldCache) {
        this.criteria.setCacheable(shouldCache);
        return this;
    }

    /**
     * Whether to check for changes on the objects loaded
     *
     * @param readOnly True to disable dirty checking
     */
    @Override
    public BuildableCriteria readOnly(boolean readOnly) {
        this.criteria.setReadOnly(readOnly);
        return this;
    }

    /**
     * Calculates the property name including any alias paths
     *
     * @param propertyName The property name
     * @return The calculated property name
     */
    protected String calculatePropertyName(String propertyName) {
        String lastAlias = getLastAlias();
        if (lastAlias != null) {
            return lastAlias + '.' + propertyName;
        }

        return propertyName;
    }

    private String getLastAlias() {
        if (this.aliasStack.size() > 0) {
            return this.aliasStack.get(this.aliasStack.size() - 1).toString();
        }
        return null;
    }

    @Override
    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    /**
     * Calculates the property value, converting GStrings if necessary
     *
     * @param propertyValue The property value
     * @return The calculated property value
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object calculatePropertyValue(Object propertyValue) {
        if (propertyValue instanceof CharSequence) {
            return propertyValue.toString();
        }
        if (propertyValue instanceof QueryableCriteria) {
            propertyValue = convertToHibernateCriteria((QueryableCriteria<?>) propertyValue);
        }
        else if (propertyValue instanceof Closure) {
            propertyValue = convertToHibernateCriteria(
                    new DetachedCriteria(this.targetClass).build((Closure<?>) propertyValue));
        }
        return propertyValue;
    }

    protected abstract org.hibernate.criterion.DetachedCriteria convertToHibernateCriteria(QueryableCriteria<?> queryableCriteria);

    /**
     * Adds a projection that allows the criteria to return the property count
     *
     * @param propertyName The name of the property
     */
    public void count(String propertyName) {
        count(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to return the property count
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public void count(String propertyName, String alias) {
        final org.hibernate.criterion.CountProjection proj = org.hibernate.criterion.Projections.count(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
    }

    @Override
    public ProjectionList id() {
        final org.hibernate.criterion.IdentifierProjection proj = org.hibernate.criterion.Projections.id();
        addProjectionToList(proj, null);
        return this;
    }

    @Override
    public ProjectionList count() {
        return rowCount();
    }

    /**
     * Adds a projection that allows the criteria to return the distinct property count
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList countDistinct(String propertyName) {
        return countDistinct(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to return the distinct property count
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList groupProperty(String propertyName) {
        groupProperty(propertyName, null);
        return this;
    }

    @Override
    public ProjectionList distinct() {
        this.criteria.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to return the distinct property count
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public ProjectionList countDistinct(String propertyName, String alias) {
        final org.hibernate.criterion.CountProjection proj = org.hibernate.criterion.Projections.countDistinct(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }


    /**
     * Adds a projection that allows the criteria's result to be grouped by a property
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public ProjectionList groupProperty(String propertyName, String alias) {
        final org.hibernate.criterion.PropertyProjection proj = org.hibernate.criterion.Projections.groupProperty(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  maximum property value
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList max(String propertyName) {
        return max(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  maximum property value
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public ProjectionList max(String propertyName, String alias) {
        final org.hibernate.criterion.AggregateProjection proj = org.hibernate.criterion.Projections.max(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  minimum property value
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList min(String propertyName) {
        return min(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  minimum property value
     *
     * @param alias The alias to use
     */
    public ProjectionList min(String propertyName, String alias) {
        final org.hibernate.criterion.AggregateProjection aggregateProjection = org.hibernate.criterion.Projections.min(calculatePropertyName(propertyName));
        addProjectionToList(aggregateProjection, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to return the row count
     */
    @Override
    public ProjectionList rowCount() {
        return rowCount(null);
    }

    /**
     * Adds a projection that allows the criteria to return the row count
     *
     * @param alias The alias to use
     */
    public ProjectionList rowCount(String alias) {
        final org.hibernate.criterion.Projection proj = org.hibernate.criterion.Projections.rowCount();
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to retrieve the sum of the results of a property
     *
     * @param propertyName The name of the property
     */
    @Override
    public ProjectionList sum(String propertyName) {
        return sum(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to retrieve the sum of the results of a property
     *
     * @param propertyName The name of the property
     * @param alias        The alias to use
     */
    public ProjectionList sum(String propertyName, String alias) {
        final org.hibernate.criterion.AggregateProjection proj = org.hibernate.criterion.Projections.sum(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Sets the fetch mode of an associated path
     *
     * @param associationPath The name of the associated path
     * @param fetchMode       The fetch mode to set
     */
    public void fetchMode(String associationPath, FetchMode fetchMode) {
        if (this.criteria != null) {
            this.criteria.setFetchMode(associationPath, fetchMode);
        }
    }

    /**
     * Sets the resultTransformer.
     *
     * @param transformer The result transformer to use.
     */
    public void resultTransformer(org.hibernate.transform.ResultTransformer transformer) {
        if (this.criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [resultTransformer] not supported here"));
        }
        this.resultTransformer = transformer;
    }

    /**
     * Join an association, assigning an alias to the joined association.
     * <p>
     * Functionally equivalent to createAlias(String, String, int) using
     * CriteriaSpecificationINNER_JOIN for the joinType.
     *
     * @param associationPath A dot-seperated property path
     * @param alias           The alias to assign to the joined association (for later reference).
     * @return this (for method chaining)
     * #see {@link #createAlias(String, String, int)}
     * @throws HibernateException Indicates a problem creating the sub criteria
     */
    public org.hibernate.Criteria createAlias(String associationPath, String alias) {
        return this.criteria.createAlias(associationPath, alias);
    }

    /**
     * Creates a Criterion that compares to class properties for equality
     *
     * @param propertyName      The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    @Override
    public Criteria eqProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [eqProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.eqProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that compares to class properties for !equality
     *
     * @param propertyName      The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    @Override
    public Criteria neProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [neProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.neProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is greater than the second property
     *
     * @param propertyName      The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    @Override
    public Criteria gtProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [gtProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.gtProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is greater than or equal to the second property
     *
     * @param propertyName      The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    @Override
    public Criteria geProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [geProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.geProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is less than the second property
     *
     * @param propertyName      The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    @Override
    public Criteria ltProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ltProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.ltProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is less than or equal to the second property
     *
     * @param propertyName      The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    @Override
    public Criteria leProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [leProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.leProperty(propertyName, otherPropertyName));
        return this;
    }

    @Override
    public Criteria allEq(Map<String, Object> propertyValues) {
        addToCriteria(org.hibernate.criterion.Restrictions.allEq(propertyValues));
        return this;
    }

    /**
     * Creates a subquery criterion that ensures the given property is equal to all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Criteria eqAll(String propertyName, Closure<?> propertyValue) {
        return eqAll(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Criteria gtAll(String propertyName, Closure<?> propertyValue) {
        return gtAll(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Criteria ltAll(String propertyName, Closure<?> propertyValue) {
        return ltAll(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Criteria geAll(String propertyName, Closure<?> propertyValue) {
        return geAll(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Criteria leAll(String propertyName, Closure<?> propertyValue) {
        return leAll(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is equal to all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria eqAll(String propertyName,
            @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).eqAll(convertToHibernateCriteria(propertyValue)));
        return this;
    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria gtAll(String propertyName,
            @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).gtAll(convertToHibernateCriteria(propertyValue)));
        return this;
    }

    @Override
    public Criteria gtSome(String propertyName, QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).gtSome(convertToHibernateCriteria(propertyValue)));
        return this;
    }

    @Override
    public Criteria gtSome(String propertyName, Closure<?> propertyValue) {
        return gtSome(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    @Override
    public Criteria geSome(String propertyName, QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).geSome(convertToHibernateCriteria(propertyValue)));
        return this;
    }

    @Override
    public Criteria geSome(String propertyName, Closure<?> propertyValue) {
        return geSome(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    @Override
    public Criteria ltSome(String propertyName, QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).ltSome(convertToHibernateCriteria(propertyValue)));
        return this;
    }

    @Override
    public Criteria ltSome(String propertyName, Closure<?> propertyValue) {
        return ltSome(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    @Override
    public Criteria leSome(String propertyName, QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).leSome(convertToHibernateCriteria(propertyValue)));
        return this;
    }

    @Override
    public Criteria leSome(String propertyName, Closure<?> propertyValue) {
        return leSome(propertyName, new DetachedCriteria(this.targetClass).build(propertyValue));
    }

    @Override
    public Criteria in(String propertyName, QueryableCriteria<?> subquery) {
        return inList(propertyName, subquery);
    }

    @Override
    public Criteria inList(String propertyName, QueryableCriteria<?> subquery) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).in(convertToHibernateCriteria(subquery)));
        return this;
    }

    @Override
    public Criteria in(String propertyName, Closure<?> subquery) {
        return inList(propertyName, new DetachedCriteria(this.targetClass).build(subquery));
    }

    @Override
    public Criteria inList(String propertyName, Closure<?> subquery) {
        return inList(propertyName, new DetachedCriteria(this.targetClass).build(subquery));
    }

    @Override
    public Criteria notIn(String propertyName, QueryableCriteria<?> subquery) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).notIn(convertToHibernateCriteria(subquery)));
        return this;
    }

    @Override
    public Criteria notIn(String propertyName, Closure<?> subquery) {
        return notIn(propertyName, new DetachedCriteria(this.targetClass).build(subquery));
    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria ltAll(String propertyName,
            @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).ltAll(convertToHibernateCriteria(propertyValue)));
        return this;

    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria geAll(String propertyName,
            @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).geAll(convertToHibernateCriteria(propertyValue)));
        return this;

    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria leAll(String propertyName,
            @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(org.hibernate.criterion.Property.forName(propertyName).leAll(convertToHibernateCriteria(propertyValue)));
        return this;

    }

    /**
     * Creates a "greater than" Criterion based on the specified property name and value
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria gt(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [gt] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);

        org.hibernate.criterion.Criterion gt;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            gt = org.hibernate.criterion.Property.forName(propertyName).gt((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            gt = org.hibernate.criterion.Restrictions.gt(propertyName, propertyValue);
        }
        addToCriteria(gt);
        return this;
    }

    @Override
    public Criteria lte(String s, Object o) {
        return le(s, o);
    }

    /**
     * Creates a "greater than or equal to" Criterion based on the specified property name and value
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria ge(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ge] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);

        org.hibernate.criterion.Criterion ge;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            ge = org.hibernate.criterion.Property.forName(propertyName).ge((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            ge = org.hibernate.criterion.Restrictions.ge(propertyName, propertyValue);
        }
        addToCriteria(ge);
        return this;
    }

    /**
     * Creates a "less than" Criterion based on the specified property name and value
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria lt(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [lt] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        org.hibernate.criterion.Criterion lt;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            lt = org.hibernate.criterion.Property.forName(propertyName).lt((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            lt = org.hibernate.criterion.Restrictions.lt(propertyName, propertyValue);
        }
        addToCriteria(lt);
        return this;
    }

    /**
     * Creates a "less than or equal to" Criterion based on the specified property name and value
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria le(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [le] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        org.hibernate.criterion.Criterion le;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            le = org.hibernate.criterion.Property.forName(propertyName).le((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            le = org.hibernate.criterion.Restrictions.le(propertyName, propertyValue);
        }
        addToCriteria(le);
        return this;
    }

    @Override
    public Criteria idEquals(Object o) {
        return idEq(o);
    }

    @Override
    public Criteria exists(QueryableCriteria<?> subquery) {
        addToCriteria(org.hibernate.criterion.Subqueries.exists(convertToHibernateCriteria(subquery)));
        return this;
    }

    @Override
    public Criteria notExists(QueryableCriteria<?> subquery) {
        addToCriteria(org.hibernate.criterion.Subqueries.notExists(convertToHibernateCriteria(subquery)));
        return this;
    }

    @Override
    public Criteria isEmpty(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(org.hibernate.criterion.Restrictions.isEmpty(propertyName));
        return this;
    }

    @Override
    public Criteria isNotEmpty(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(org.hibernate.criterion.Restrictions.isNotEmpty(propertyName));
        return this;
    }

    @Override
    public Criteria isNull(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(org.hibernate.criterion.Restrictions.isNull(propertyName));
        return this;
    }

    @Override
    public Criteria isNotNull(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(org.hibernate.criterion.Restrictions.isNotNull(propertyName));
        return this;
    }

    @Override
    public Criteria and(Closure callable) {
        return executeLogicalExpression(callable, AND);
    }

    @Override
    public Criteria or(Closure callable) {
        return executeLogicalExpression(callable, OR);
    }

    @Override
    public Criteria not(Closure callable) {
        return executeLogicalExpression(callable, NOT);
    }

    protected Criteria executeLogicalExpression(Closure callable, String logicalOperator) {
        this.logicalExpressionStack.add(new LogicalExpression(logicalOperator));
        try {
            invokeClosureNode(callable);
        }
        finally {
            LogicalExpression logicalExpression = this.logicalExpressionStack.remove(this.logicalExpressionStack.size() - 1);
            if (logicalExpression != null) {
                addToCriteria(logicalExpression.toCriterion());
            }
        }

        return this;
    }

    /**
     * Creates an "equals" Criterion based on the specified property name and value. Case-sensitive.
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @Override
    public Criteria eq(String propertyName, Object propertyValue) {
        return eq(propertyName, propertyValue, Collections.emptyMap());
    }

    @Override
    public Criteria idEq(Object o) {
        return eq("id", o);
    }

    /**
     * Groovy moves the map to the first parameter if using the idiomatic form, e.g.
     * <code>eq 'firstName', 'Fred', ignoreCase: true</code>.
     *
     * @param params        optional map with customization parameters; currently only 'ignoreCase' is supported.
     * @param propertyName
     * @param propertyValue
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Criteria eq(Map params, String propertyName, Object propertyValue) {
        return eq(propertyName, propertyValue, params);
    }

    /**
     * Creates an "equals" Criterion based on the specified property name and value.
     * Supports case-insensitive search if the <code>params</code> map contains <code>true</code>
     * under the 'ignoreCase' key.
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @param params        optional map with customization parameters; currently only 'ignoreCase' is supported.
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Criteria eq(String propertyName, Object propertyValue, Map params) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [eq] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        org.hibernate.criterion.Criterion eq;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            eq = org.hibernate.criterion.Property.forName(propertyName).eq((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            eq = org.hibernate.criterion.Restrictions.eq(propertyName, propertyValue);
        }
        if (params != null && (eq instanceof org.hibernate.criterion.SimpleExpression)) {
            Object ignoreCase = params.get("ignoreCase");
            if (ignoreCase instanceof Boolean && (Boolean) ignoreCase) {
                eq = ((org.hibernate.criterion.SimpleExpression) eq).ignoreCase();
            }
        }
        addToCriteria(eq);
        return this;
    }

    /**
     * Applies a sql restriction to the results to allow something like:
     *
     * @param sqlRestriction the sql restriction
     * @return a Criteria instance
     */
    public Criteria sqlRestriction(String sqlRestriction) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sqlRestriction] with value [" +
                    sqlRestriction + "] not allowed here."));
        }
        return sqlRestriction(sqlRestriction, Collections.EMPTY_LIST);
    }

    /**
     * Applies a sql restriction to the results to allow something like:
     *
     * @param sqlRestriction the sql restriction
     * @param values         jdbc parameters
     * @return a Criteria instance
     */
    public Criteria sqlRestriction(String sqlRestriction, List<?> values) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sqlRestriction] with value [" +
                    sqlRestriction + "] not allowed here."));
        }
        final int numberOfParameters = values.size();

        final Type[] typesArray = new Type[numberOfParameters];
        final Object[] valuesArray = new Object[numberOfParameters];

        if (numberOfParameters > 0) {
            final TypeHelper typeHelper = this.sessionFactory.getTypeHelper();
            for (int i = 0; i < typesArray.length; i++) {
                final Object value = values.get(i);
                typesArray[i] = typeHelper.basic(value.getClass());
                valuesArray[i] = value;
            }
        }
        addToCriteria(org.hibernate.criterion.Restrictions.sqlRestriction(sqlRestriction, valuesArray, typesArray));
        return this;
    }

    /**
     * Creates a Criterion with from the specified property name and "like" expression
     *
     * @param propertyName  The property name
     * @param propertyValue The like value
     * @return A Criterion instance
     */
    @Override
    public Criteria like(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [like] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(org.hibernate.criterion.Restrictions.like(propertyName, propertyValue));
        return this;
    }

    /**
     * Creates a Criterion with from the specified property name and "rlike" (a regular expression version of "like") expression
     *
     * @param propertyName  The property name
     * @param propertyValue The ilike value
     * @return A Criterion instance
     */
    @Override
    public abstract Criteria rlike(String propertyName, Object propertyValue);

    /**
     * Creates a Criterion with from the specified property name and "ilike" (a case sensitive version of "like") expression
     *
     * @param propertyName  The property name
     * @param propertyValue The ilike value
     * @return A Criterion instance
     */
    @Override
    public Criteria ilike(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ilike] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(org.hibernate.criterion.Restrictions.ilike(propertyName, propertyValue));
        return this;
    }

    /**
     * Applys a "in" contrain on the specified property
     *
     * @param propertyName The property name
     * @param values       A collection of values
     * @return A Criterion instance
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Criteria in(String propertyName, Collection values) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [in] with propertyName [" +
                    propertyName + "] and values [" + values + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);

        if (values instanceof List) {
            values = convertArgumentList((List) values);
        }
        addToCriteria(org.hibernate.criterion.Restrictions.in(propertyName, values == null ? Collections.EMPTY_LIST : values));
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected List convertArgumentList(List argList) {
        List convertedList = new ArrayList(argList.size());
        for (Object item : argList) {
            if (item instanceof CharSequence) {
                item = item.toString();
            }
            convertedList.add(item);
        }
        return convertedList;
    }

    /**
     * Delegates to in as in is a Groovy keyword
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Criteria inList(String propertyName, Collection values) {
        return in(propertyName, values);
    }

    /**
     * Delegates to in as in is a Groovy keyword
     */
    @Override
    public Criteria inList(String propertyName, Object[] values) {
        return in(propertyName, values);
    }

    /**
     * Applys a "in" contrain on the specified property
     *
     * @param propertyName The property name
     * @param values       A collection of values
     * @return A Criterion instance
     */
    @Override
    public Criteria in(String propertyName, Object[] values) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [in] with propertyName [" +
                    propertyName + "] and values [" + values + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.in(propertyName, values));
        return this;
    }

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param propertyName The property name to order by
     * @return An Order instance
     */
    @Override
    public Criteria order(String propertyName) {
        if (this.criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [order] with propertyName [" +
                    propertyName + "]not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        org.hibernate.criterion.Order o = org.hibernate.criterion.Order.asc(propertyName);
        addOrderInternal(this.criteria, o);
        return this;
    }

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param o The property name to order by
     * @return A Order instance
     */
    public Criteria order(org.hibernate.criterion.Order o) {
        final org.hibernate.Criteria criteria = this.criteria;
        addOrderInternal(criteria, o);
        return this;
    }

    private void addOrderInternal(org.hibernate.Criteria criteria, org.hibernate.criterion.Order o) {
        if (criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [order] not allowed here."));
        }
        if (this.paginationEnabledList) {
            this.orderEntries.add(o);
        }
        else {
            criteria.addOrder(o);
        }
    }

    @Override
    public Criteria order(Query.Order o) {

        final org.hibernate.Criteria criteria = this.criteria;
        final String property = o.getProperty();
        addOrderInternal(criteria, o, property);
        return this;
    }

    private void addOrderInternal(org.hibernate.Criteria criteria, Query.Order o, String property) {
        final int i = property.indexOf('.');
        if (i == -1) {
            org.hibernate.criterion.Order order = convertOrder(o, property);
            addOrderInternal(criteria, order);
        }
        else {
            String sortHead = property.substring(0, i);
            String sortTail = property.substring(i + 1);
            createAliasIfNeccessary(sortHead, sortHead, org.hibernate.sql.JoinType.INNER_JOIN.getJoinTypeValue());
            final org.hibernate.Criteria sub = this.aliasInstanceStack.get(this.aliasInstanceStack.size() - 1);
            addOrderInternal(sub, o, sortTail);
        }
    }

    protected org.hibernate.criterion.Order convertOrder(Query.Order o, String property) {
        org.hibernate.criterion.Order order;
        switch (o.getDirection()) {
            case DESC:
                order = org.hibernate.criterion.Order.desc(property);
                break;
            default:
                order = org.hibernate.criterion.Order.asc(property);
                break;
        }
        if (o.isIgnoreCase()) {
            order.ignoreCase();
        }
        return order;
    }

    /**
     * Orders by the specified property name and direction
     *
     * @param propertyName The property name to order by
     * @param direction    Either "asc" for ascending or "desc" for descending
     * @return An Order instance
     */
    @Override
    public Criteria order(String propertyName, String direction) {
        if (this.criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [order] with propertyName [" +
                    propertyName + "]not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        org.hibernate.criterion.Order o;
        if (direction.equals(ORDER_DESCENDING)) {
            o = org.hibernate.criterion.Order.desc(propertyName);
        }
        else {
            o = org.hibernate.criterion.Order.asc(propertyName);
        }
        if (this.paginationEnabledList) {
            this.orderEntries.add(o);
        }
        else {
            this.criteria.addOrder(o);
        }
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property by size
     *
     * @param propertyName The property name
     * @param size         The size to constrain by
     * @return A Criterion instance
     */
    @Override
    public Criteria sizeEq(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeEq] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.sizeEq(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be greater than the given size
     *
     * @param propertyName The property name
     * @param size         The size to constrain by
     * @return A Criterion instance
     */
    @Override
    public Criteria sizeGt(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeGt] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.sizeGt(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be greater than or equal to the given size
     *
     * @param propertyName The property name
     * @param size         The size to constrain by
     * @return A Criterion instance
     */
    @Override
    public Criteria sizeGe(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeGe] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.sizeGe(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be less than or equal to the given size
     *
     * @param propertyName The property name
     * @param size         The size to constrain by
     * @return A Criterion instance
     */
    @Override
    public Criteria sizeLe(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeLe] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.sizeLe(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be less than to the given size
     *
     * @param propertyName The property name
     * @param size         The size to constrain by
     * @return A Criterion instance
     */
    @Override
    public Criteria sizeLt(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeLt] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.sizeLt(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be not equal to the given size
     *
     * @param propertyName The property name
     * @param size         The size to constrain by
     * @return A Criterion instance
     */
    @Override
    public Criteria sizeNe(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeNe] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.sizeNe(propertyName, size));
        return this;
    }

    /**
     * Creates a "not equal" Criterion based on the specified property name and value
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return The criterion object
     */
    @Override
    public Criteria ne(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ne] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(org.hibernate.criterion.Restrictions.ne(propertyName, propertyValue));
        return this;
    }

    public Criteria notEqual(String propertyName, Object propertyValue) {
        return ne(propertyName, propertyValue);
    }

    /**
     * Creates a "between" Criterion based on the property name and specified lo and hi values
     *
     * @param propertyName The property name
     * @param lo           The low value
     * @param hi           The high value
     * @return A Criterion instance
     */
    @Override
    public Criteria between(String propertyName, Object lo, Object hi) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [between] with propertyName [" +
                    propertyName + "]  not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(org.hibernate.criterion.Restrictions.between(propertyName, lo, hi));
        return this;
    }

    @Override
    public Criteria gte(String s, Object o) {
        return ge(s, o);
    }

    protected boolean validateSimpleExpression() {
        return this.criteria != null;
    }

    @Override
    public Object list(@DelegatesTo(org.hibernate.Criteria.class) Closure c) {
        return invokeMethod(LIST_CALL, new Object[] { c });
    }

    @Override
    public Object list(Map params, @DelegatesTo(org.hibernate.Criteria.class) Closure c) {
        return invokeMethod(LIST_CALL, new Object[] { params, c });
    }

    @Override
    public Object listDistinct(@DelegatesTo(org.hibernate.Criteria.class) Closure c) {
        return invokeMethod(LIST_DISTINCT_CALL, new Object[] { c });
    }

    @Override
    public Object get(@DelegatesTo(org.hibernate.Criteria.class) Closure c) {
        return invokeMethod(GET_CALL, new Object[] { c });
    }

    @Override
    public Object scroll(@DelegatesTo(org.hibernate.Criteria.class) Closure c) {
        return invokeMethod(SCROLL_CALL, new Object[] { c });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object invokeMethod(String name, Object obj) {
        Object[] args = obj.getClass().isArray() ? (Object[]) obj : new Object[] { obj };

        if (this.paginationEnabledList && SET_RESULT_TRANSFORMER_CALL.equals(name) && args.length == 1 &&
                args[0] instanceof org.hibernate.transform.ResultTransformer) {
            this.resultTransformer = (org.hibernate.transform.ResultTransformer) args[0];
            return null;
        }

        if (isCriteriaConstructionMethod(name, args)) {
            if (this.criteria != null) {
                throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
            }

            if (name.equals(GET_CALL)) {
                this.uniqueResult = true;
            }
            else if (name.equals(SCROLL_CALL)) {
                this.scroll = true;
            }
            else if (name.equals(COUNT_CALL)) {
                this.count = true;
            }
            else if (name.equals(LIST_DISTINCT_CALL)) {
                this.resultTransformer = org.hibernate.criterion.CriteriaSpecification.DISTINCT_ROOT_ENTITY;
            }

            createCriteriaInstance();

            // Check for pagination params
            if (name.equals(LIST_CALL) && args.length == 2) {
                this.paginationEnabledList = true;
                this.orderEntries = new ArrayList<>();
                invokeClosureNode(args[1]);
            }
            else {
                invokeClosureNode(args[0]);
            }

            if (this.resultTransformer != null) {
                this.criteria.setResultTransformer(this.resultTransformer);
            }
            Object result;
            if (!this.uniqueResult) {
                if (this.scroll) {
                    result = this.criteria.scroll();
                }
                else if (this.count) {
                    this.criteria.setProjection(org.hibernate.criterion.Projections.rowCount());
                    result = this.criteria.uniqueResult();
                }
                else if (this.paginationEnabledList) {
                    // Calculate how many results there are in total. This has been
                    // moved to before the 'list()' invocation to avoid any "ORDER
                    // BY" clause added by 'populateArgumentsForCriteria()', otherwise
                    // an exception is thrown for non-string sort fields (GRAILS-2690).
                    this.criteria.setFirstResult(0);
                    this.criteria.setMaxResults(Integer.MAX_VALUE);

                    // Restore the previous projection, add settings for the pagination parameters,
                    // and then execute the query.
                    boolean isProjection = (this.projectionList != null && this.projectionList.getLength() > 0);
                    this.criteria.setProjection(isProjection ? this.projectionList : null);

                    for (org.hibernate.criterion.Order orderEntry : this.orderEntries) {
                        this.criteria.addOrder(orderEntry);
                    }
                    if (this.resultTransformer == null) {
                        // GRAILS-9644 - Use projection transformer
                        this.criteria.setResultTransformer(isProjection ?
                                org.hibernate.criterion.CriteriaSpecification.PROJECTION :
                                org.hibernate.criterion.CriteriaSpecification.ROOT_ENTITY
                        );
                    }
                    else if (this.paginationEnabledList) {
                        // relevant to GRAILS-5692
                        this.criteria.setResultTransformer(this.resultTransformer);
                    }
                    // GRAILS-7324 look if we already have association to sort by
                    Map argMap = (Map) args[0];
                    final String sort = (String) argMap.get(HibernateQueryConstants.ARGUMENT_SORT);
                    if (sort != null) {
                        boolean ignoreCase = true;
                        Object caseArg = argMap.get(HibernateQueryConstants.ARGUMENT_IGNORE_CASE);
                        if (caseArg instanceof Boolean) {
                            ignoreCase = (Boolean) caseArg;
                        }
                        final String orderParam = (String) argMap.get(HibernateQueryConstants.ARGUMENT_ORDER);
                        final String order = HibernateQueryConstants.ORDER_DESC.equalsIgnoreCase(orderParam) ?
                                HibernateQueryConstants.ORDER_DESC : HibernateQueryConstants.ORDER_ASC;
                        int lastPropertyPos = sort.lastIndexOf('.');
                        String associationForOrdering = lastPropertyPos >= 0 ? sort.substring(0, lastPropertyPos) : null;
                        if (associationForOrdering != null && this.aliasMap.containsKey(associationForOrdering)) {
                            addOrder(this.criteria, this.aliasMap.get(associationForOrdering) + "." + sort.substring(lastPropertyPos + 1),
                                    order, ignoreCase);
                            // remove sort from arguments map to exclude from default processing.
                            @SuppressWarnings("unchecked") Map argMap2 = new HashMap(argMap);
                            argMap2.remove(HibernateQueryConstants.ARGUMENT_SORT);
                            argMap = argMap2;
                        }
                    }
                    result = createPagedResultList(argMap);
                }
                else {
                    result = this.criteria.list();
                }
            }
            else {
                result = executeUniqueResultWithProxyUnwrap();
            }
            if (!this.participate) {
                closeSession();
            }
            return result;
        }

        if (this.criteria == null) {
            createCriteriaInstance();
        }

        MetaMethod metaMethod = getMetaClass().getMetaMethod(name, args);
        if (metaMethod != null) {
            return metaMethod.invoke(this, args);
        }

        metaMethod = this.criteriaMetaClass.getMetaMethod(name, args);
        if (metaMethod != null) {
            return metaMethod.invoke(this.criteria, args);
        }
        metaMethod = this.criteriaMetaClass.getMetaMethod(NameUtils.getSetterName(name), args);
        if (metaMethod != null) {
            return metaMethod.invoke(this.criteria, args);
        }

        if (isAssociationQueryMethod(args) || isAssociationQueryWithJoinSpecificationMethod(args)) {
            final boolean hasMoreThanOneArg = args.length > 1;
            Object callable = hasMoreThanOneArg ? args[1] : args[0];
            int joinType = hasMoreThanOneArg ? (Integer) args[0] : org.hibernate.sql.JoinType.INNER_JOIN.getJoinTypeValue();

            if (name.equals(AND) || name.equals(OR) || name.equals(NOT)) {
                if (this.criteria == null) {
                    throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
                }

                this.logicalExpressionStack.add(new LogicalExpression(name));
                invokeClosureNode(callable);

                LogicalExpression logicalExpression = this.logicalExpressionStack.remove(this.logicalExpressionStack.size() - 1);
                addToCriteria(logicalExpression.toCriterion());

                return name;
            }

            if (name.equals(PROJECTIONS) && args.length == 1 && (args[0] instanceof Closure)) {
                if (this.criteria == null) {
                    throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
                }

                this.projectionList = org.hibernate.criterion.Projections.projectionList();
                invokeClosureNode(callable);

                if (this.projectionList != null && this.projectionList.getLength() > 0) {
                    this.criteria.setProjection(this.projectionList);
                }

                return name;
            }

            final PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.targetClass, name);
            if (pd != null && pd.getReadMethod() != null) {
                final Metamodel metamodel = this.sessionFactory.getMetamodel();
                final EntityType<?> entityType = metamodel.entity(this.targetClass);
                final Attribute<?, ?> attribute = entityType.getAttribute(name);

                if (attribute.isAssociation()) {
                    Class oldTargetClass = this.targetClass;
                    this.targetClass = getClassForAssociationType(attribute);
                    if (this.targetClass.equals(oldTargetClass) && !hasMoreThanOneArg) {
                        joinType = org.hibernate.sql.JoinType.LEFT_OUTER_JOIN.getJoinTypeValue(); // default to left join if joining on the same table
                    }
                    this.associationStack.add(name);
                    final String associationPath = getAssociationPath();
                    createAliasIfNeccessary(name, associationPath, joinType);
                    // the criteria within an association node are grouped with an implicit AND
                    this.logicalExpressionStack.add(new LogicalExpression(AND));
                    invokeClosureNode(callable);
                    this.aliasStack.remove(this.aliasStack.size() - 1);
                    if (!this.aliasInstanceStack.isEmpty()) {
                        this.aliasInstanceStack.remove(this.aliasInstanceStack.size() - 1);
                    }
                    LogicalExpression logicalExpression = this.logicalExpressionStack.remove(this.logicalExpressionStack.size() - 1);
                    if (!logicalExpression.args.isEmpty()) {
                        addToCriteria(logicalExpression.toCriterion());
                    }
                    this.associationStack.remove(this.associationStack.size() - 1);
                    this.targetClass = oldTargetClass;

                    return name;
                }
                if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
                    this.associationStack.add(name);
                    this.logicalExpressionStack.add(new LogicalExpression(AND));
                    Class oldTargetClass = this.targetClass;
                    this.targetClass = pd.getPropertyType();
                    invokeClosureNode(callable);
                    this.targetClass = oldTargetClass;
                    LogicalExpression logicalExpression = this.logicalExpressionStack.remove(this.logicalExpressionStack.size() - 1);
                    if (!logicalExpression.args.isEmpty()) {
                        addToCriteria(logicalExpression.toCriterion());
                    }
                    this.associationStack.remove(this.associationStack.size() - 1);
                    return name;
                }
            }
        }
        else if (args.length == 1 && args[0] != null) {
            if (this.criteria == null) {
                throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
            }

            Object value = args[0];
            org.hibernate.criterion.Criterion c = null;
            if (name.equals(ID_EQUALS)) {
                return eq("id", value);
            }

            if (name.equals(IS_NULL) ||
                    name.equals(IS_NOT_NULL) ||
                    name.equals(IS_EMPTY) ||
                    name.equals(IS_NOT_EMPTY)) {
                if (!(value instanceof String)) {
                    throwRuntimeException(new IllegalArgumentException("call to [" + name + "] with value [" +
                            value + "] requires a String value."));
                }
                String propertyName = calculatePropertyName((String) value);
                if (name.equals(IS_NULL)) {
                    c = org.hibernate.criterion.Restrictions.isNull(propertyName);
                }
                else if (name.equals(IS_NOT_NULL)) {
                    c = org.hibernate.criterion.Restrictions.isNotNull(propertyName);
                }
                else if (name.equals(IS_EMPTY)) {
                    c = org.hibernate.criterion.Restrictions.isEmpty(propertyName);
                }
                else if (name.equals(IS_NOT_EMPTY)) {
                    c = org.hibernate.criterion.Restrictions.isNotEmpty(propertyName);
                }
            }

            if (c != null) {
                return addToCriteria(c);
            }
        }

        throw new MissingMethodException(name, getClass(), args);
    }

    protected abstract Object executeUniqueResultWithProxyUnwrap();

    protected abstract List createPagedResultList(Map args);


    private boolean isAssociationQueryMethod(Object[] args) {
        return args.length == 1 && args[0] instanceof Closure;
    }

    private boolean isAssociationQueryWithJoinSpecificationMethod(Object[] args) {
        return args.length == 2 && (args[0] instanceof Number) && (args[1] instanceof Closure);
    }

    private void createAliasIfNeccessary(String associationName, String associationPath, int joinType) {
        String newAlias;
        if (this.aliasMap.containsKey(associationPath)) {
            newAlias = this.aliasMap.get(associationPath);
        }
        else {
            this.aliasCount++;
            newAlias = associationName + ALIAS + this.aliasCount;
            this.aliasMap.put(associationPath, newAlias);
            this.aliasInstanceStack.add(createAlias(associationPath, newAlias, joinType));
        }
        this.aliasStack.add(newAlias);
    }

    private String getAssociationPath() {
        StringBuilder fullPath = new StringBuilder();
        for (Object anAssociationStack : this.associationStack) {
            String propertyName = (String) anAssociationStack;
            if (fullPath.length() > 0) {
                fullPath.append(".");
            }
            fullPath.append(propertyName);
        }
        return fullPath.toString();
    }

    private boolean isCriteriaConstructionMethod(String name, Object[] args) {
        return (name.equals(LIST_CALL) && args.length == 2 && args[0] instanceof Map && args[1] instanceof Closure) ||
                (name.equals(ROOT_CALL) ||
                        name.equals(ROOT_DO_CALL) ||
                        name.equals(LIST_CALL) ||
                        name.equals(LIST_DISTINCT_CALL) ||
                        name.equals(GET_CALL) ||
                        name.equals(COUNT_CALL) ||
                        name.equals(SCROLL_CALL) && args.length == 1 && args[0] instanceof Closure);
    }

    public org.hibernate.Criteria buildCriteria(Closure<?> criteriaClosure) {
        createCriteriaInstance();
        criteriaClosure.setDelegate(this);
        criteriaClosure.call();
        return this.criteria;
    }

    protected abstract void createCriteriaInstance();

    protected abstract void cacheCriteriaMapping();

    private void invokeClosureNode(Object args) {
        Closure<?> callable = (Closure<?>) args;
        callable.setDelegate(this);
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call();
    }

    /**
     * adds and returns the given criterion to the currently active criteria set.
     * this might be either the root criteria or a currently open
     * LogicalExpression.
     */
    protected org.hibernate.criterion.Criterion addToCriteria(org.hibernate.criterion.Criterion c) {
        if (!this.logicalExpressionStack.isEmpty()) {
            this.logicalExpressionStack.get(this.logicalExpressionStack.size() - 1).args.add(c);
        }
        else {
            this.criteria.add(c);
        }
        return c;
    }

    /**
     * Add order directly to criteria.
     */
    private static void addOrder(org.hibernate.Criteria c, String sort, String order, boolean ignoreCase) {
        if (HibernateQueryConstants.ORDER_DESC.equals(order)) {
            c.addOrder(ignoreCase ? org.hibernate.criterion.Order.desc(sort).ignoreCase() : org.hibernate.criterion.Order.desc(sort));
        }
        else {
            c.addOrder(ignoreCase ? org.hibernate.criterion.Order.asc(sort).ignoreCase() : org.hibernate.criterion.Order.asc(sort));
        }
    }

    /**
     * Returns the criteria instance
     *
     * @return The criteria instance
     */
    public org.hibernate.Criteria getInstance() {
        return this.criteria;
    }

    /**
     * Set whether a unique result should be returned
     *
     * @param uniqueResult True if a unique result should be returned
     */
    public void setUniqueResult(boolean uniqueResult) {
        this.uniqueResult = uniqueResult;
    }

    /**
     * Join an association using the specified join-type, assigning an alias
     * to the joined association.
     * sub
     * The joinType is expected to be one of CriteriaSpecification.INNER_JOIN (the default),
     * CriteriaSpecificationFULL_JOIN, or CriteriaSpecificationLEFT_JOIN.
     *
     * @param associationPath A dot-seperated property path
     * @param alias           The alias to assign to the joined association (for later reference).
     * @param joinType        The type of join to use.
     * @return this (for method chaining)
     * @throws org.hibernate.HibernateException Indicates a problem creating the sub criteria
     */
    public abstract org.hibernate.Criteria createAlias(String associationPath, String alias, int joinType);

    protected abstract Class getClassForAssociationType(Attribute<?, ?> type);

    /**
     * Throws a runtime exception where necessary to ensure the session gets closed
     */
    protected void throwRuntimeException(RuntimeException t) {
        closeSessionFollowingException();
        throw t;
    }

    private void closeSessionFollowingException() {
        closeSession();
        this.criteria = null;
    }

    /**
     * Closes the session if it is copen
     */
    protected void closeSession() {
        if (this.hibernateSession != null && this.hibernateSession.isOpen() && !this.participate) {
            this.hibernateSession.close();
        }
    }

    public int getDefaultFlushMode() {
        return this.defaultFlushMode;
    }

    public void setDefaultFlushMode(int defaultFlushMode) {
        this.defaultFlushMode = defaultFlushMode;
    }

    /**
     * instances of this class are pushed onto the logicalExpressionStack
     * to represent all the unfinished "and", "or", and "not" expressions.
     */
    protected class LogicalExpression {

        public final Object name;

        public final List<org.hibernate.criterion.Criterion> args = new ArrayList<>();

        public LogicalExpression(Object name) {
            this.name = name;
        }

        public org.hibernate.criterion.Criterion toCriterion() {
            if (this.name.equals(NOT)) {
                switch (this.args.size()) {
                    case 0:
                        throwRuntimeException(new IllegalArgumentException("Logical expression [not] must contain at least 1 expression"));
                        return null;

                    case 1:
                        return org.hibernate.criterion.Restrictions.not(this.args.get(0));

                    default:
                        // treat multiple sub-criteria as an implicit "OR"
                        return org.hibernate.criterion.Restrictions.not(buildJunction(org.hibernate.criterion.Restrictions.disjunction(), this.args));
                }
            }

            if (this.name.equals(AND)) {
                return buildJunction(org.hibernate.criterion.Restrictions.conjunction(), this.args);
            }

            if (this.name.equals(OR)) {
                return buildJunction(org.hibernate.criterion.Restrictions.disjunction(), this.args);
            }

            throwRuntimeException(new IllegalStateException("Logical expression [" + this.name + "] not handled!"));
            return null;
        }

        // add the Criterion objects in the given list to the given junction.
        public org.hibernate.criterion.Junction buildJunction(org.hibernate.criterion.Junction junction, List<org.hibernate.criterion.Criterion> criterions) {
            for (org.hibernate.criterion.Criterion c : criterions) {
                junction.add(c);
            }

            return junction;
        }

    }

}
