package grails.gorm.tests.autoimport.other

import grails.gorm.annotation.Entity

@Entity
class A {

    static mapping = {
        autoImport false
    }
}
