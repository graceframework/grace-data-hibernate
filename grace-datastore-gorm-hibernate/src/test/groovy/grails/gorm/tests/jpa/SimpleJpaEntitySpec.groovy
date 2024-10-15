package grails.gorm.tests.jpa

import grails.gorm.hibernate.HibernateEntity
import grails.gorm.transactions.Rollback
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.orm.hibernate.HibernateDatastore
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.validation.ConstraintViolationException
import jakarta.validation.constraints.Digits

/**
 * Created by graemerocher on 22/12/16.
 */
class SimpleJpaEntitySpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': 'org.hibernate.dialect.H2Dialect'
    ]
    @Shared @AutoCleanup HibernateDatastore hibernateDatastore = new HibernateDatastore(config, Customer)
    @Shared PlatformTransactionManager transactionManager = hibernateDatastore.getTransactionManager()

    @Ignore
    @Rollback
    void "test that JPA entities can be treated as GORM entities"() {
        when:"A basic entity is persisted and validated"
        Customer c = new Customer(firstName: "6000.01", lastName: "Flintstone")
        c.save(flush:true, validate:false)

        def query = Customer.where {
            lastName == 'Rubble'
        }
        then:"The object was saved"
        Customer.get(null) == null
        Customer.get("null") == null
        Customer.get(c.id) != null
        !c.errors.hasErrors()
        Customer.count() == 1
        query.count() == 0
    }

    @Ignore
    @Rollback
    void "test that JPA entities can use javax.validation"() {
        when:"A basic entity is persisted and validated"
        Customer c = new Customer(firstName: "Bad", lastName: "Flintstone")
        c.save(flush:true)

        def query = Customer.where {
            lastName == 'Rubble'
        }
        then:"The object was saved"
        c.errors.hasErrors()
        Customer.count() == 0
        query.count() == 0
    }

    @Ignore
    @Rollback
    void "test that JPA entities can use javax.validation and the hibernate interceptor evicts invalid entities"() {
        when:"A basic entity is persisted and validated"
        Customer c = new Customer(firstName: "Bad", lastName: "Flintstone")
        c.save(flush:true, validate:false)

        def query = Customer.where {
            lastName == 'Rubble'
        }
        then:"The object was saved"
        thrown(ConstraintViolationException)
        c.errors.hasErrors()
    }

    @Ignore
    void "Test persistent entity model"() {
        given:
        PersistentEntity entity = hibernateDatastore.mappingContext.getPersistentEntity(Customer.name)

        expect:
        entity.identity.name == 'myId'
        entity.associations.size() == 1
        entity.associations.find { Association a -> a.name == 'related' }
    }
}

@Entity
class Customer implements HibernateEntity<Customer> {
    @Id
    @GeneratedValue
    Long myId
    @Digits(integer = 6, fraction = 2)
    String firstName
    String lastName

    @OneToMany
    Set<Customer> related
}