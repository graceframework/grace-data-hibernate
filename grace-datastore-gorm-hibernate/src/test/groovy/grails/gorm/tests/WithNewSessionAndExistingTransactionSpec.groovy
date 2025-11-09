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
package grails.gorm.tests

import javax.sql.DataSource

import org.hibernate.Session
import org.springframework.orm.hibernate5.SessionHolder
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import spock.lang.Issue

import org.grails.datastore.gorm.Setup
import org.grails.orm.hibernate.GormSpec
import org.grails.orm.hibernate.HibernateDatastore

/**
 * Created by graemerocher on 26/08/2016.
 */
class WithNewSessionAndExistingTransactionSpec extends GormSpec {

    @Override
    List getDomainClasses() {
        [Book]
    }

    void 'Test withNewSession when an existing transaction is present'() {
        when: 'An existing transaction not to pick up the current session'
        sessionFactory.currentSession
        SessionHolder previousSessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        Book.withNewSession { Session session ->
            // access the current session
            assert !previousSessionHolder.is(TransactionSynchronizationManager.getResource(sessionFactory))
            session.sessionFactory.currentSession
        }
        // reproduce session closed problem
        int result = Book.count()
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        DataSource dataSource = ((HibernateDatastore) session.datastore).connectionSources.defaultConnectionSource.dataSource
        org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = dataSource.targetDataSource.targetDataSource

        then: 'The result is correct'
        dataSource != null
        tomcatDataSource != null
        tomcatDataSource.pool.active == 1
        sessionHolder.is(previousSessionHolder)
        TransactionSynchronizationManager.isSynchronizationActive()
        sessionHolder.session.isOpen()
        sessionHolder.isSynchronizedWithTransaction()
        sessionFactory.currentSession.isOpen()
        result == 0
        Book.count() == 0
        sessionFactory.currentSession == Setup.hibernateSession
        Setup.hibernateSession.isOpen()
    }

    @Issue('https://github.com/grails/grails-core/issues/10426')
    void 'Test with withNewSession with nested transaction'() {
        when: 'An existing transaction not to pick up the current session'
        sessionFactory.currentSession
        SessionHolder previousSessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        Book.withNewSession { Session session ->
            assert !previousSessionHolder.is(TransactionSynchronizationManager.getResource(sessionFactory))
            // access the current session
            session.sessionFactory.currentSession
            // reproduce "Pre-bound JDBC Connection found!" problem
            Book.withNewTransaction {
                assert !previousSessionHolder.is(TransactionSynchronizationManager.getResource(sessionFactory))
                new Book(title: 'The Stand', author: 'Stephen King').save()
            }
        }

        Book.count()
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)

        DataSource dataSource = ((HibernateDatastore) session.datastore).connectionSources.defaultConnectionSource.dataSource
        org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = dataSource.targetDataSource.targetDataSource

        then: 'The result is correct'
        dataSource != null
        tomcatDataSource != null
        tomcatDataSource.pool.active == 1
        sessionHolder.is(previousSessionHolder)
        TransactionSynchronizationManager.isSynchronizationActive()
        sessionHolder.session.isOpen()
        sessionHolder.isSynchronizedWithTransaction()
        sessionFactory.currentSession.isOpen()
        sessionFactory.currentSession == Setup.hibernateSession
        Setup.hibernateSession.isOpen()
    }

    @Issue('https://github.com/grails/grails-core/issues/10448')
    void 'Test with withNewSession with existing transaction'() {
        when: 'the connection pool is obtained'
        DataSource dataSource = ((HibernateDatastore) session.datastore).connectionSources.defaultConnectionSource.dataSource
        org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = dataSource.targetDataSource.targetDataSource

        then: 'the active count is correct'
        dataSource != null
        tomcatDataSource != null
        tomcatDataSource.pool.active == 0

        when: 'An existing transaction not to pick up the current session'
        sessionFactory.currentSession
        SessionHolder previousSessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        Book.withNewTransaction { TransactionStatus status ->
            // reproduce "java.lang.IllegalStateException: No value for key" problem
            Book.withNewSession { Session session ->
                // access the current session
                assert !previousSessionHolder.is(TransactionSynchronizationManager.getResource(sessionFactory))
                session.sessionFactory.currentSession

                new Book(title: 'The Stand', author: 'Stephen King').save()
            }
        }

        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)

        then: 'After withNewSession is completed all connections are closed'
        tomcatDataSource.pool.active == 0

        when: 'A count is executed that uses the current connection'
        Book.count()

        then: 'The result is correct'
        tomcatDataSource.pool.active == 1
        sessionHolder.is(previousSessionHolder)
        TransactionSynchronizationManager.isSynchronizationActive()
        sessionHolder.session.isOpen()
        sessionHolder.isSynchronizedWithTransaction()
        sessionFactory.currentSession.isOpen()
        sessionFactory.currentSession == Setup.hibernateSession
        Setup.hibernateSession.isOpen()
    }

}
