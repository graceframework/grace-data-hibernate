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
package org.grails.orm.hibernate.connections

import org.hibernate.boot.Metadata
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import grails.gorm.annotation.Entity

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.orm.hibernate.HibernateDatastore

class MultipleDataSourceMetadataSpec extends Specification {

    @Shared
    Map config = [
            'dataSource.url'             : 'jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000',
            'dataSources.apples.url'     : 'jdbc:h2:mem:apples;LOCK_TIMEOUT=10000',
            'dataSources.apples.dialect' : 'org.hibernate.dialect.H2Dialect',
            'dataSources.oranges.url'    : 'jdbc:h2:mem:oranges;LOCK_TIMEOUT=10000',
            'dataSources.oranges.dialect': 'org.hibernate.dialect.H2Dialect'
    ]

    @AutoCleanup
    @Shared
    HibernateDatastore datastore = new HibernateDatastore(DatastoreUtils.createPropertyResolver(config), Apple, Orange)

    void 'test metadata retrieval for multiple dataSources'() {
        when: 'the metadata for the default dataSource is retrieved'
        Metadata metadataDefault = datastore.metadata

        then: 'the metadata is set and does not contain entityBindings or tableMappings'
        metadataDefault.entityBindings.size() == 0
        metadataDefault.collectTableMappings().size() == 0

        when: 'the metadata for the apples dataSource is retrieved'
        Metadata metadataApples = datastore.getDatastoreForConnection('apples').metadata

        then: 'the metadata is set and does contain the correct entityBinding and tableMapping'
        metadataApples.entityBindings.size() == 1
        metadataApples.entityBindings.first().getMappedClass() == Apple
        metadataApples.collectTableMappings().size() == 1
        metadataApples.collectTableMappings().first().name == 'apple'

        when: 'the metadata for the oranges dataSource is retrieved'
        Metadata metadataOranges = datastore.getDatastoreForConnection('oranges').metadata

        then: 'the metadata is set and does contain the correct entityBinding and tableMapping'
        metadataOranges.entityBindings.size() == 1
        metadataOranges.entityBindings.first().getMappedClass() == Orange
        metadataOranges.collectTableMappings().size() == 1
        metadataOranges.collectTableMappings().first().name == 'orange'
    }

}

@Entity
class Apple {

    String name

    static mapping = {
        datasource 'apples'
    }

}

@Entity
class Orange {

    Integer age

    static mapping = {
        datasource 'oranges'
    }

}
