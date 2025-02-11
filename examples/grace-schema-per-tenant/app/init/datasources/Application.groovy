package datasources

import grails.boot.Grails
import groovy.transform.CompileStatic
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration

//@EnableAutoConfiguration(exclude = DataSourceTransactionManagerAutoConfiguration)
@CompileStatic
class Application {
    static void main(String[] args) {
        Grails.run(Application)
    }
}