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
package grails.orm;

import java.util.Collection;
import java.util.Map;

import jakarta.persistence.criteria.JoinType;

import groovy.lang.Closure;

import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.BuildableCriteria;
import org.grails.datastore.mapping.query.api.Criteria;
import org.grails.datastore.mapping.query.api.ProjectionList;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.orm.hibernate.query.AbstractHibernateCriteriaBuilder;

/**
 * <p>Wraps the Hibernate Criteria API in a builder. The builder can be retrieved through the "createCriteria()" dynamic static
 * method of Grails domain classes (Example in Groovy):
 * <pre>
 *         def c = Account.createCriteria()
 *         def results = c {
 *             projections {
 *                 groupProperty("branch")
 *             }
 *             like("holderFirstName", "Fred%")
 *             and {
 *                 between("balance", 500, 1000)
 *                 eq("branch", "London")
 *             }
 *             maxResults(10)
 *             order("holderLastName", "desc")
 *         }
 * </pre>
 * <p>The builder can also be instantiated standalone with a SessionFactory and persistent Class instance:
 * <pre>
 *      new HibernateCriteriaBuilder(clazz, sessionFactory).list {
 *         eq("firstName", "Fred")
 *      }
 * </pre>
 *
 * @author Graeme Rocher
 */
public class HibernateCriteriaBuilder extends AbstractHibernateCriteriaBuilder {

    @Override
    public Class getTargetClass() {
        return null;
    }

    @Override
    public Criteria exists(QueryableCriteria<?> subquery) {
        return null;
    }

    @Override
    public Criteria notExists(QueryableCriteria<?> subquery) {
        return null;
    }

    @Override
    public Criteria idEquals(Object value) {
        return null;
    }

    @Override
    public Criteria isEmpty(String propertyName) {
        return null;
    }

    @Override
    public Criteria isNotEmpty(String propertyName) {
        return null;
    }

    @Override
    public Criteria isNull(String propertyName) {
        return null;
    }

    @Override
    public Criteria isNotNull(String propertyName) {
        return null;
    }

    @Override
    public Criteria eq(String propertyName, Object propertyValue) {
        return null;
    }

    @Override
    public Criteria idEq(Object propertyValue) {
        return null;
    }

    @Override
    public Criteria ne(String propertyName, Object propertyValue) {
        return null;
    }

    @Override
    public Criteria between(String propertyName, Object start, Object finish) {
        return null;
    }

    @Override
    public Criteria gte(String property, Object value) {
        return null;
    }

    @Override
    public Criteria ge(String property, Object value) {
        return null;
    }

    @Override
    public Criteria gt(String property, Object value) {
        return null;
    }

    @Override
    public Criteria lte(String property, Object value) {
        return null;
    }

    @Override
    public Criteria le(String property, Object value) {
        return null;
    }

    @Override
    public Criteria lt(String property, Object value) {
        return null;
    }

    @Override
    public Criteria like(String propertyName, Object propertyValue) {
        return null;
    }

    @Override
    public Criteria ilike(String propertyName, Object propertyValue) {
        return null;
    }

    @Override
    public Criteria rlike(String propertyName, Object propertyValue) {
        return null;
    }

    @Override
    public Criteria and(Closure callable) {
        return null;
    }

    @Override
    public Criteria or(Closure callable) {
        return null;
    }

    @Override
    public Criteria not(Closure callable) {
        return null;
    }

    @Override
    public Criteria in(String propertyName, Collection values) {
        return null;
    }

    @Override
    public Criteria in(String propertyName, QueryableCriteria<?> subquery) {
        return null;
    }

    @Override
    public Criteria inList(String propertyName, QueryableCriteria<?> subquery) {
        return null;
    }

    @Override
    public Criteria in(String propertyName, Closure<?> subquery) {
        return null;
    }

    @Override
    public Criteria inList(String propertyName, Closure<?> subquery) {
        return null;
    }

    @Override
    public Criteria inList(String propertyName, Collection values) {
        return null;
    }

    @Override
    public Criteria inList(String propertyName, Object[] values) {
        return null;
    }

    @Override
    public Criteria in(String propertyName, Object[] values) {
        return null;
    }

    @Override
    public Criteria notIn(String propertyName, QueryableCriteria<?> subquery) {
        return null;
    }

    @Override
    public Criteria notIn(String propertyName, Closure<?> subquery) {
        return null;
    }

    @Override
    public Criteria order(String propertyName) {
        return null;
    }

    @Override
    public Criteria order(Query.Order o) {
        return null;
    }

    @Override
    public Criteria order(String propertyName, String direction) {
        return null;
    }

    @Override
    public Criteria sizeEq(String propertyName, int size) {
        return null;
    }

    @Override
    public Criteria sizeGt(String propertyName, int size) {
        return null;
    }

    @Override
    public Criteria sizeGe(String propertyName, int size) {
        return null;
    }

    @Override
    public Criteria sizeLe(String propertyName, int size) {
        return null;
    }

    @Override
    public Criteria sizeLt(String propertyName, int size) {
        return null;
    }

    @Override
    public Criteria sizeNe(String propertyName, int size) {
        return null;
    }

    @Override
    public Criteria eqProperty(String propertyName, String otherPropertyName) {
        return null;
    }

    @Override
    public Criteria neProperty(String propertyName, String otherPropertyName) {
        return null;
    }

    @Override
    public Criteria gtProperty(String propertyName, String otherPropertyName) {
        return null;
    }

    @Override
    public Criteria geProperty(String propertyName, String otherPropertyName) {
        return null;
    }

    @Override
    public Criteria ltProperty(String propertyName, String otherPropertyName) {
        return null;
    }

    @Override
    public Criteria leProperty(String propertyName, String otherPropertyName) {
        return null;
    }

    @Override
    public Criteria allEq(Map<String, Object> propertyValues) {
        return null;
    }

    @Override
    public Criteria eqAll(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria gtAll(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria ltAll(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria geAll(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria leAll(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria eqAll(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria gtAll(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria ltAll(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria geAll(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria leAll(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria gtSome(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria gtSome(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria geSome(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria geSome(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria ltSome(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria ltSome(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public Criteria leSome(String propertyName, QueryableCriteria propertyValue) {
        return null;
    }

    @Override
    public Criteria leSome(String propertyName, Closure<?> propertyValue) {
        return null;
    }

    @Override
    public BuildableCriteria cache(boolean cache) {
        return null;
    }

    @Override
    public BuildableCriteria readOnly(boolean readOnly) {
        return null;
    }

    @Override
    public BuildableCriteria join(String property) {
        return null;
    }

    @Override
    public BuildableCriteria join(String property, JoinType joinType) {
        return null;
    }

    @Override
    public BuildableCriteria select(String property) {
        return null;
    }

    @Override
    public Object list(Closure closure) {
        return null;
    }

    @Override
    public Object list(Map params, Closure closure) {
        return null;
    }

    @Override
    public Object listDistinct(Closure closure) {
        return null;
    }

    @Override
    public Object scroll(Closure closure) {
        return null;
    }

    @Override
    public Object get(Closure closure) {
        return null;
    }

    @Override
    public ProjectionList id() {
        return null;
    }

    @Override
    public ProjectionList count() {
        return null;
    }

    @Override
    public ProjectionList countDistinct(String property) {
        return null;
    }

    @Override
    public ProjectionList groupProperty(String property) {
        return null;
    }

    @Override
    public ProjectionList distinct() {
        return null;
    }

    @Override
    public ProjectionList distinct(String property) {
        return null;
    }

    @Override
    public ProjectionList rowCount() {
        return null;
    }

    @Override
    public ProjectionList property(String name) {
        return null;
    }

    @Override
    public ProjectionList sum(String name) {
        return null;
    }

    @Override
    public ProjectionList min(String name) {
        return null;
    }

    @Override
    public ProjectionList max(String name) {
        return null;
    }

    @Override
    public ProjectionList avg(String name) {
        return null;
    }

}
