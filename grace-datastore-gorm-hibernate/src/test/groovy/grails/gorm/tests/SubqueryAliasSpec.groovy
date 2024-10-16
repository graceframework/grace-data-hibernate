package grails.gorm.tests

import grails.gorm.transactions.Rollback
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
import org.grails.orm.hibernate.HibernateDatastore
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 01/03/2017.
 */
@ApplyDetachedCriteriaTransform
class SubqueryAliasSpec extends Specification {

    @Shared Map config = [
            'dataSource.url':"jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000",
            'dataSource.dbCreate': 'create-drop',
            'dataSource.dialect': 'org.hibernate.dialect.H2Dialect'
    ]
    @AutoCleanup @Shared HibernateDatastore datastore = new HibernateDatastore(
            config, Club, Team
    )

    @Shared PlatformTransactionManager transactionManager = datastore.getTransactionManager()

    @Rollback
    void "Test subquery with root alias"() {
        given:
        Club c = new Club(name: "Manchester United").save()
        new Team(name: "First Team", club: c).save(flush:true)

        when:
        Team t = Team.where {
            def t = Team
            name == "First Team"
            exists(Club.where {
                id == t.club
            }.property('name'))

        }.find()

        then:
        t != null
    }
}
