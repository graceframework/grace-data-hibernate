package grails.gorm.tests.inheritance

import spock.lang.Ignore

import org.grails.orm.hibernate.GormSpec
import spock.lang.Issue

@Issue('https://github.com/grails/gorm-hibernate5/issues/151')
@Ignore("https://issues.apache.org/jira/browse/GROOVY-5106")
class TablePerConcreteClassImportedSpec extends GormSpec {
    void "test that subclasses are added to the imports on the metamodel"() {
        expect:
        sessionFactory.getMetamodel().getImportedClassName('Vehicle')
        sessionFactory.getMetamodel().getImportedClassName('Spaceship')
    }

    @Override
    List getDomainClasses() {
        [Vehicle, Spaceship]
    }
}