package grails.gorm.tests.txs

import grails.gorm.tests.services.Attribute
import grails.gorm.tests.services.Product
import grails.gorm.transactions.Transactional
import org.grails.orm.hibernate.HibernateDatastore
import org.springframework.transaction.annotation.Isolation
import spock.lang.AutoCleanup
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 16/06/2017.
 */
class CustomIsolationLevelSpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': 'org.hibernate.dialect.H2Dialect'
    ]
    @AutoCleanup @Shared HibernateDatastore hibernateDatastore = new HibernateDatastore(config, Product, Attribute)


    @Issue('https://github.com/grails/grails-data-mapping/issues/952')
    void "test custom isolation level"() {
        expect:
        new ProductService().listProducts().size() == 0
    }


}

class ProductService {
    @Transactional(isolation = Isolation.SERIALIZABLE)
    List<Product> listProducts() {
        Product.list()
    }
}
