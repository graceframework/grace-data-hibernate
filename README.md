# Grace Data for Hibernate

[![Main branch build status](https://github.com/graceframework/grace-data-hibernate/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-data-hibernate/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework.plugins/hibernate.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework.plugins)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://twitter.com/graceframework)

This project implements [GORM](https://github.com/graceframework/grace-data) for the Hibernate 5. However, support for Hibernate 6 is in development. 

> [!IMPORTANT]
> From version 2023.0.0-M5, this plugin has been renamed from the original hibernate5 to hibernate.


```gradle
dependencies {
    // Before 2023.0.0-M5
    implementation "org.graceframework:gorm-hibernate5-spring-boot"
    implementation "org.graceframework:grace-datastore-gorm-hibernate5"
    implementation "org.graceframework.plugins:hibernate5"

    // After 2023.0.0-M5
    implementation "org.graceframework:grace-datastore-gorm-hibernate5"
    implementation "org.graceframework.plugins:hibernate"
}
```
