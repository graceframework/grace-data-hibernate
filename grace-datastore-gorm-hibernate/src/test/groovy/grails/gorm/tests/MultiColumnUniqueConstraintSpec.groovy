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

import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Issue

import grails.gorm.annotation.Entity

@Issue('https://github.com/grails/grails-data-mapping/issues/617')
class MultiColumnUniqueConstraintSpec extends GormDatastoreSpec {

    void 'test generated unique constraints'() {
        expect:
        new DomainOne(controller: 'project', action: 'update').save(flush: true)
        new DomainOne(controller: 'project', action: 'delete').save(flush: true)
        new DomainOne(controller: 'projectTask', action: 'update').save(flush: true)
    }

    void 'test generated unique constraints violation'() {
        when:
        new DomainOne(controller: 'project', action: 'update').save(flush: true)
        new DomainOne(controller: 'project', action: 'update').save(flush: true, validate: false)

        then:
        thrown DataIntegrityViolationException
    }

    void 'test generated unique constraints for related domains'() {
        given: 'two existing tasks'
        Task task1 = new Task(name: 'task1').save(flush: true, failOnError: true)
        Task task2 = new Task(name: 'task2').save(flush: true, failOnError: true)

        when: 'saving task links for the same toTask but not breaking unique index'
        TaskLink taskLink1 = new TaskLink(fromTask: task1, toTask: task2).save(flush: true, validate: false)
        TaskLink taskLink2 = new TaskLink(fromTask: task2, toTask: task2).save(flush: true, validate: false)

        then: 'both links may be saved'
        taskLink1
        taskLink2

        when: 'instance which breaks unique index is saved'
        new TaskLink(fromTask: task1, toTask: task2).save(flush: true, validate: false)

        then: 'DataIntegrityViolationException is thrown'
        thrown DataIntegrityViolationException
    }

    @Override
    List getDomainClasses() {
        [DomainOne, Task, TaskLink]
    }

}

@Entity
class DomainOne {

    String controller
    String action

    static constraints = {
        action unique: 'controller'
    }

}

@Entity
class Task {

    String name

}

@Entity
class TaskLink {

    Task toTask
    Task fromTask

    static constraints = {
        toTask unique: ['fromTask']
    }

}
