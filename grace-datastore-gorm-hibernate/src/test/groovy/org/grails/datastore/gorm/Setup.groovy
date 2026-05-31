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
package org.grails.datastore.gorm

import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.h2.Driver
import org.hibernate.SessionFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.ApplicationContext
import org.springframework.orm.hibernate5.SessionFactoryUtils
import org.springframework.orm.hibernate5.SessionHolder
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionSynchronizationManager

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration

class Setup {

    static HibernateDatastore hibernateDatastore
    static hibernateSession
    static GrailsHibernateTransactionManager transactionManager
    static SessionFactory sessionFactory
    static TransactionStatus transactionStatus
    static HibernateMappingContextConfiguration hibernateConfig
    static ApplicationContext applicationContext

    @CompileStatic
    static destroy() {
        if (transactionStatus != null) {
            def tx = transactionStatus
            transactionStatus = null
            transactionManager.rollback(tx)
        }
        if (hibernateSession != null) {
            SessionFactoryUtils.closeSession((org.hibernate.Session) hibernateSession)
        }

        if (hibernateConfig != null) {
            hibernateConfig = null
        }
        hibernateDatastore.destroy()
        hibernateDatastore = null
        hibernateSession = null
        transactionManager = null
        sessionFactory = null
        if (applicationContext instanceof DisposableBean) {
            ((DisposableBean) applicationContext).destroy()
        }
        applicationContext = null
        shutdownInMemDb()
    }

    static shutdownInMemDb() {
        Sql sql = null
        try {
            sql = Sql.newInstance('jdbc:h2:mem:grailsDb', 'sa', '', Driver.name)
            sql.executeUpdate('SHUTDOWN')
        }
        catch (ignore) {
            // already closed, ignore
        }
        finally {
            try {
                sql?.close()
            }
            catch (ignored) {
            }
        }
    }

    static Session setup(List<Class> classes, ConfigObject grailsConfig = new ConfigObject(), boolean isTransactional = true) {
        System.setProperty('hibernate5.gorm.suite', 'true')

        grailsConfig.dataSource.dbCreate = 'create-drop'
        grailsConfig.dataSource.url = 'jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000'
        hibernateDatastore = new HibernateDatastore(DatastoreUtils.createPropertyResolver(grailsConfig), classes as Class[])
        transactionManager = hibernateDatastore.getTransactionManager()
        sessionFactory = hibernateDatastore.sessionFactory
        if (transactionStatus == null && isTransactional) {
            transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition())
        }
        else if (isTransactional) {
            throw new RuntimeException('new transaction started during active transaction')
        }
        if (!isTransactional) {
            hibernateSession = sessionFactory.openSession()
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(hibernateSession))
        }
        else {
            hibernateSession = sessionFactory.currentSession
        }

        return hibernateDatastore.connect()
    }

}
