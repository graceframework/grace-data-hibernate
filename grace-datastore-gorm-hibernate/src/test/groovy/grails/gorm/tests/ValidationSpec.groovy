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

import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Tests validation semantics.
 */
class ValidationSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        return [ClassWithListArgBeforeValidate, ClassWithNoArgBeforeValidate,
                ClassWithOverloadedBeforeValidate]
    }

    void 'Test validate() method'() {
        // test assumes name cannot be blank
        given:
        def t

        when:
        t = new TestEntity(name: '')
        boolean validationResult = t.validate()
        def errors = t.errors

        then:
        !validationResult
        t.hasErrors()
        errors != null
        errors.hasErrors()

        when:
        t.clearErrors()

        then:
        !t.hasErrors()
    }

    void 'Test that validate is called on save()'() {
        given:
        def t

        when:
        t = new TestEntity(name: '')

        then:
        t.save() == null
        t.hasErrors() == true
        TestEntity.count() == 0

        when:
        t.clearErrors()
        t.name = 'Bob'
        t.age = 45
        t.child = new ChildEntity(name: 'Fred')
        t = t.save()

        then:
        t != null
        TestEntity.count() == 1
    }

    void 'Test beforeValidate gets called on save()'() {
        given:
        def entityWithNoArgBeforeValidateMethod
        def entityWithListArgBeforeValidateMethod
        def entityWithOverloadedBeforeValidateMethod

        when:
        entityWithNoArgBeforeValidateMethod = new ClassWithNoArgBeforeValidate()
        entityWithListArgBeforeValidateMethod = new ClassWithListArgBeforeValidate()
        entityWithOverloadedBeforeValidateMethod = new ClassWithOverloadedBeforeValidate()
        entityWithNoArgBeforeValidateMethod.save()
        entityWithListArgBeforeValidateMethod.save()
        entityWithOverloadedBeforeValidateMethod.save()

        then:
        entityWithNoArgBeforeValidateMethod.noArgCounter == 1
        entityWithListArgBeforeValidateMethod.listArgCounter == 1
        entityWithOverloadedBeforeValidateMethod.noArgCounter == 1
        entityWithOverloadedBeforeValidateMethod.listArgCounter == 0
    }

    void 'Test beforeValidate gets called on validate()'() {
        given:
        def entityWithNoArgBeforeValidateMethod
        def entityWithListArgBeforeValidateMethod
        def entityWithOverloadedBeforeValidateMethod

        when:
        entityWithNoArgBeforeValidateMethod = new ClassWithNoArgBeforeValidate()
        entityWithListArgBeforeValidateMethod = new ClassWithListArgBeforeValidate()
        entityWithOverloadedBeforeValidateMethod = new ClassWithOverloadedBeforeValidate()
        entityWithNoArgBeforeValidateMethod.validate()
        entityWithListArgBeforeValidateMethod.validate()
        entityWithOverloadedBeforeValidateMethod.validate()

        then:
        entityWithNoArgBeforeValidateMethod.noArgCounter == 1
        entityWithListArgBeforeValidateMethod.listArgCounter == 1
        entityWithOverloadedBeforeValidateMethod.noArgCounter == 1
        entityWithOverloadedBeforeValidateMethod.listArgCounter == 0
    }

    void 'Test beforeValidate gets called on validate() and passing a list of field names to validate'() {
        given:
        def entityWithNoArgBeforeValidateMethod
        def entityWithListArgBeforeValidateMethod
        def entityWithOverloadedBeforeValidateMethod

        when:
        entityWithNoArgBeforeValidateMethod = new ClassWithNoArgBeforeValidate()
        entityWithListArgBeforeValidateMethod = new ClassWithListArgBeforeValidate()
        entityWithOverloadedBeforeValidateMethod = new ClassWithOverloadedBeforeValidate()
        entityWithNoArgBeforeValidateMethod.validate(['name'])
        entityWithListArgBeforeValidateMethod.validate(['name'])
        entityWithOverloadedBeforeValidateMethod.validate(['name'])

        then:
        entityWithNoArgBeforeValidateMethod.noArgCounter == 1
        entityWithListArgBeforeValidateMethod.listArgCounter == 1
        entityWithOverloadedBeforeValidateMethod.noArgCounter == 0
        entityWithOverloadedBeforeValidateMethod.listArgCounter == 1
        entityWithOverloadedBeforeValidateMethod.propertiesPassedToBeforeValidate == ['name']
    }

    void 'Test that validate works without a bound Session'() {
        given:
        def t

        when:
        session.disconnect()
        def resource
        if (TransactionSynchronizationManager.hasResource(session.datastore.sessionFactory)) {
            resource = TransactionSynchronizationManager.unbindResource(session.datastore.sessionFactory)
        }

        t = new TestEntity(name: '')

        then:
        TransactionSynchronizationManager.getResource(session.datastore.sessionFactory) == null
        t.save() == null
        t.hasErrors() == true

        when:
        TransactionSynchronizationManager.bindResource(session.datastore.sessionFactory, resource)

        then:
        t.errors.allErrors.size() == 1
        TestEntity.count() == 0

        when:
        t.clearErrors()
        t.name = 'Bob'
        t.age = 45
        t.child = new ChildEntity(name: 'Fred')
        t = t.save(flush: true)

        then:
        t != null
        TestEntity.count() == 1
    }

}
