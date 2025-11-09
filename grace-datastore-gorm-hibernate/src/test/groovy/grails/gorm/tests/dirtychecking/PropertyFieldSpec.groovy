/*
 * Copyright 2017-2025 the original author or authors.
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
package grails.gorm.tests.dirtychecking

import spock.lang.AutoCleanup
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

import grails.gorm.annotation.Entity
import grails.gorm.transactions.Rollback

import org.grails.orm.hibernate.HibernateDatastore

/**
 * Created by graemerocher on 05/05/2017.
 */
class PropertyFieldSpec extends Specification {

    @Shared
    Map config = [
            'dataSource.url'     : 'jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000',
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect' : 'org.hibernate.dialect.H2Dialect'
    ]

    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore = new HibernateDatastore(config, getClass().getPackage())

    @Rollback
    @Issue('https://github.com/grails/grails-data-mapping/issues/934')
    void "test domain class with property named 'property'"() {
        expect:
        Book book = new Book(title: 'book', property: new Property(name: 'p1'))
        book.save()
        book.title == 'book'
    }

}

@Entity
class Property {

    String name

}

@Entity
class Book {

    String title
    Property property

}
