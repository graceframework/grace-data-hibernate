package grails.gorm.tests

import grails.gorm.annotation.Entity
import grails.gorm.transactions.Rollback
import org.grails.orm.hibernate.HibernateDatastore
import org.hibernate.engine.spi.SessionImplementor
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification


/**
 * Created by graemerocher on 20/10/16.
 */
class SequenceIdSpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': 'org.hibernate.dialect.H2Dialect'
    ]
    @Shared @AutoCleanup HibernateDatastore datastore = new HibernateDatastore(config, BookWithSequence)
    @Shared PlatformTransactionManager transactionManager = datastore.getTransactionManager()

    @Rollback
    void "test sequence generator"() {
        when:"A book is saved"
        BookWithSequence book = new BookWithSequence(title: 'The Stand')
        book.save(flush:true)

        then:"The entity was saved"
        BookWithSequence.first()

        ((SessionImplementor)datastore.sessionFactory.currentSession).connection().prepareStatement("call NEXT VALUE FOR book_seq;")
                .executeQuery()
                .next()
    }
}
@Entity
class BookWithSequence {
    String title

    static constraints = {
    }

    static mapping = {
        version false
        id generator:'sequence', params:[sequence:'book_seq']
        id index:'book_id_idx'
    }
}