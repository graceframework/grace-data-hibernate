# Grace Data for Hibernate

[![Main branch build status](https://github.com/graceframework/grace-data-hibernate/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-data-hibernate/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework.plugins/hibernate.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework.plugins)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://twitter.com/graceframework)

This project implements [GORM](https://github.com/graceframework/grace-data) for the [Hibernate ORM](https://hibernate.org/orm/).

> [!IMPORTANT]
> Currently, this plugin has been migrate to Jakarta Namespace, and support Hibernate 5.6.However, support for Hibernate 6 is in development. 
> From version 2023.0.0-M5, this plugin has been renamed from the original hibernate5 to hibernate.


```gradle
dependencies {
    // Before 2023.0.0-M5
    implementation "org.graceframework:gorm-hibernate5-spring-boot"
    implementation "org.graceframework:grace-datastore-gorm-hibernate5"
    implementation "org.graceframework.plugins:hibernate5"
    implementation "org.hibernate:hibernate-core-jakarta:5.6.15.Final"

    // After 2023.0.0-M5
    implementation "org.graceframework:grace-datastore-gorm-hibernate"
    implementation "org.graceframework.plugins:hibernate"
    implementation "org.hibernate:hibernate-core-jakarta:5.6.15.Final"
}
```

## License

This plugin is available as open source under the terms of the [APACHE LICENSE, VERSION 2.0](http://apache.org/Licenses/LICENSE-2.0)

## Links

- [Grace Framework](https://github.com/graceframework/grace-framework)
- [Hibernate ORM](https://hibernate.org/orm/)
