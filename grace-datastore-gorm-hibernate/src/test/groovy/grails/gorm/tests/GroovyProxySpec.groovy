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

import spock.lang.Ignore

import org.grails.datastore.gorm.proxy.GroovyProxyFactory

/**
 * @author graemerocher
 */
class GroovyProxySpec extends GormDatastoreSpec {

    // this test is ignored because Groovy proxies are not used with Hibernate
    @Ignore
    void 'Test creation and behavior of Groovy proxies'() {
        given:
        session.mappingContext.proxyFactory = new GroovyProxyFactory()
        def id = new Location(name: 'United Kingdom', code: 'UK').save(flush: true)?.id
        session.clear()

        when:
        def location = Location.proxy(id)

        then:
        location != null
        location.id == id
        location.isInitialized() == false
        location.initialized == false

        location.code == 'UK'
        location.namedAndCode() == 'United Kingdom - UK'
        location.isInitialized() == true
        location.initialized == true
        location.target != null
    }

}
