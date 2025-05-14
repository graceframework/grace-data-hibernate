package functional.tests

class Product {

    String name
    String price

    static constraints = {
        price(scale: 2)
    }
}
