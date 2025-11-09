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
package org.grails.orm.hibernate

import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext
import spock.lang.Shared

import grails.core.GrailsApplication
import grails.gorm.tests.GormDatastoreSpec

abstract class GormSpec extends GormDatastoreSpec {

    GrailsApplication grailsApplication
    ApplicationContext applicationContext
    SessionFactory sessionFactory

    @Shared
    List savedTestClasses

    def setupSpec() {
        savedTestClasses = new ArrayList(TEST_CLASSES)
        TEST_CLASSES.clear()
    }

    def cleanupSpec() {
        TEST_CLASSES.addAll(savedTestClasses)
    }

    def setup() {
        grailsApplication = setupClass.grailsApplication
        applicationContext = setupClass.applicationContext
        sessionFactory = setupClass.sessionFactory
    }

}
