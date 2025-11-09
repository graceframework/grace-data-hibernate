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

import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException

/**
 * @author Burt Beckwith
 */
class OptimisticLockingSpec extends GormDatastoreSpec {

    void 'Test versioning'() {
        given:
        def o = new OptLockVersioned(name: 'locked')

        when:
        o.save flush: true

        then:
        o.version == 0

        when:
        session.clear()
        o = OptLockVersioned.get(o.id)
        o.name = 'Fred'
        o.save flush: true

        then:
        o.version == 1

        when:
        session.clear()
        o = OptLockVersioned.get(o.id)

        then:
        o.name == 'Fred'
        o.version == 1
    }

    void 'Test optimistic locking'() {
        given:
        def o = new OptLockVersioned(name: 'locked').save(flush: true)
        session.clear()
        setupClass.transactionManager.commit setupClass.transactionStatus
        setupClass.transactionStatus = null

        when:
        OptLockVersioned.withTransaction {
            o = OptLockVersioned.get(o.id)

            Thread.start {
                OptLockVersioned.withTransaction { s ->
                    def reloaded = OptLockVersioned.get(o.id)
                    assert reloaded
                    assert reloaded != o
                    reloaded.name += ' in new session'
                    reloaded.save(flush: true)
                    assert reloaded.version == 1
                    assert o.version == 0
                }
            }.join()

            o.name += ' in main session'
            o.save(flush: true)

            session.clear()
            o = OptLockVersioned.get(o.id)
        }

        then:
        thrown HibernateOptimisticLockingFailureException
    }

    void "Test optimistic locking disabled with 'version false'"() {
        given:
        def o = new OptLockNotVersioned(name: 'locked').save(flush: true)
        session.clear()
        setupClass.transactionManager.commit setupClass.transactionStatus
        setupClass.transactionStatus = null

        when:
        def ex
        OptLockNotVersioned.withTransaction {
            o = OptLockNotVersioned.get(o.id)

            Thread.start {
                OptLockNotVersioned.withTransaction { s ->
                    def reloaded = OptLockNotVersioned.get(o.id)
                    reloaded.name += ' in new session'
                    reloaded.save(flush: true)
                }
            }.join()

            o.name += ' in main session'

            try {
                o.save(flush: true)
            }
            catch (e) {
                ex = e
                e.printStackTrace()
            }

            session.clear()
            o = OptLockNotVersioned.get(o.id)
        }

        then:
        ex == null
        o.name == 'locked in main session'
    }

}
