package datasources

import grails.boot.Grails
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration

//@EnableAutoConfiguration(exclude = DataSourceTransactionManagerAutoConfiguration)
class Application {
    static void main(String[] args) {
        Grails.run(Application)
    }
}