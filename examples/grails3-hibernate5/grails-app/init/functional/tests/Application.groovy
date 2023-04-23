package functional.tests

import grails.boot.Grails
import groovy.transform.CompileStatic

import grails.boot.annotation.GrailsComponentScan

@CompileStatic
@GrailsComponentScan(['functional.tests', 'another'])
class Application {
    static void main(String[] args) {
        Grails.run(Application, args)
    }
}