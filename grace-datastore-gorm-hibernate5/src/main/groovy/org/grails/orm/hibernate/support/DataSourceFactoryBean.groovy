package org.grails.orm.hibernate.support

import javax.sql.DataSource

import org.springframework.beans.factory.FactoryBean

import org.grails.orm.hibernate.AbstractHibernateDatastore
import org.grails.orm.hibernate.connections.HibernateConnectionSource

/**
 * A factory class to retrieve a {@link javax.sql.DataSource} from the Hibernate datastore
 *
 * @author James Kleeh
 */
class DataSourceFactoryBean implements FactoryBean<DataSource> {

    AbstractHibernateDatastore datastore
    String connectionName

    DataSourceFactoryBean(AbstractHibernateDatastore datastore, String connectionName) {
        this.datastore = datastore
        this.connectionName = connectionName
    }

    @Override
    DataSource getObject() throws Exception {
        ((HibernateConnectionSource) datastore.connectionSources.getConnectionSource(connectionName)).dataSource
    }

    @Override
    Class<?> getObjectType() {
        DataSource
    }

    @Override
    boolean isSingleton() {
        true
    }

}
