package grails.gorm.tests.traits

import grails.gorm.annotation.Entity
import grails.gorm.transactions.Rollback
import org.grails.orm.hibernate.HibernateDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 02/05/2017.
 */
class TraitPropertySpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': 'org.hibernate.dialect.H2Dialect'
    ]
    @Shared @AutoCleanup HibernateDatastore datastore = new HibernateDatastore(config, getClass().getPackage())

    @Rollback
    void "test entity with trait property"() {
        when:
        new EntityWithTrait(name: "test", bar: "test2").save(flush:true)
        EntityWithTrait obj = EntityWithTrait.first()

        then:
        obj.name == "test"
        obj.bar == "test2"
    }
}

trait Foo {
    String bar
}

@Entity
class EntityWithTrait implements Foo {
    String name
}
