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

import org.hibernate.SessionFactory;

import org.grails.orm.hibernate.GrailsHibernateTemplate;
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
 * @author Michael Yan
 */
public class HibernateCriteriaBuilder extends AbstractHibernateCriteriaBuilder {

    @SuppressWarnings("rawtypes")
    public HibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory) {
        super(targetClass, sessionFactory);
        setDefaultFlushMode(GrailsHibernateTemplate.FLUSH_AUTO);
    }

    @SuppressWarnings("rawtypes")
    public HibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory, boolean uniqueResult) {
        super(targetClass, sessionFactory, uniqueResult);
        setDefaultFlushMode(GrailsHibernateTemplate.FLUSH_AUTO);
    }

}
