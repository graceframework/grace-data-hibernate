package grails.gorm.tests

import static grails.gorm.hibernate.mapping.MappingBuilder.define

import grails.gorm.annotation.Entity
import grails.gorm.transactions.Rollback
import org.grails.orm.hibernate.HibernateDatastore
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 26/01/2017.
 */
class CompositeIdWithJoinTableSpec extends Specification {
    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': 'org.hibernate.dialect.H2Dialect'
    ]

    @AutoCleanup @Shared
    HibernateDatastore datastore = new HibernateDatastore(config, CompositeIdParent, CompositeIdChild)
    @Shared
    PlatformTransactionManager transactionManager = datastore.transactionManager

    @Rollback
    void "test composite id with join table"() {
        when:"A parent with a composite id and a join table is saved"
        new CompositeIdParent(name: "Test" , last:"Test 2")
                .addToChildren(new CompositeIdChild())
                .save(flush:true)


        then:"The entity was saved"
        CompositeIdParent.count() == 1
        CompositeIdParent.list().first().children.size() == 1
    }
}

@Entity
class CompositeIdParent implements Serializable {
    String name
    String last
    static hasMany = [children:CompositeIdChild]
    static mapping = define {
        id composite('name','last')
        property("children") {
            joinTable {
                name "child_parent"
                column "child_id"
            }
            column {
                name "foo"
            }
            column {
                name "bar"
            }
        }
    }
}

@Entity
class CompositeIdChild {

    static mapping = {

    }
    static constraints = {
    }
}