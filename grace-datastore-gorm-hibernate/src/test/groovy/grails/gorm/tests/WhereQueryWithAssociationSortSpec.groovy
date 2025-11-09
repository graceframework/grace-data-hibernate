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

import org.hibernate.QueryException
import spock.lang.Issue

/**
 * Created by graemerocher on 03/11/16.
 */
class WhereQueryWithAssociationSortSpec extends GormDatastoreSpec {

    @Issue('https://github.com/grails/grails-core/issues/9860')
    void 'Test sort with where query that queries association'() {
        given: 'some test data'
        def c = new Club(name: 'Manchester United').save()
        def t = new Team(club: c, name: 'MU First Team').save()
        def c2 = new Club(name: 'Arsenal').save()
        def t2 = new Team(club: c2, name: 'Arsenal First Team').save(flush: true)

        when: 'a where query uses a sort on an association'
        def results = Team.where {
            club.name == 'Manchester United'
        }.list(sort: 'club.name')

        then: 'an exception is thrown because no alias is specified'
        thrown QueryException

        when: 'a where query uses a sort on an association'
        results = Team.where {
            def c1 = club
            c1.name ==~ '%e%'
        }.list(sort: 'c1.name')

        then: 'an exception is thrown because no alias is specified'
        results.size() == 2
        results.first().name == 'Arsenal First Team'
    }

    @Override
    List getDomainClasses() {
        [Club, Team]
    }

}
