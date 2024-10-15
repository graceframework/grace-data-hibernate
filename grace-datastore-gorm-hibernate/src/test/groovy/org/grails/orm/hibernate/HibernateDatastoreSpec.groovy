package org.grails.orm.hibernate

import spock.lang.Specification

/**
 * Created by graemerocher on 22/09/2016.
 */
class HibernateDatastoreSpec extends Specification {

    void "test configure via map"() {
        when:"The map constructor is used"
        def config = Map.of(
                'dataSource.dbCreate',  "create-drop",
                "dataSource.url", "jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000")
        HibernateDatastore datastore = new HibernateDatastore(config, Book)

        then:"GORM is configured correctly"
        Book.withNewSession {
            Book.count()
        } == 0
    }
}
