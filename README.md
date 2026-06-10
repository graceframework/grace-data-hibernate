[![Main branch build status](https://github.com/graceframework/grace-data-hibernate/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-data-hibernate/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework/grace-datastore-gorm-hibernate.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework)
[![Grace Document](https://img.shields.io/badge/Grace_Document-latest-blue?style=flat&logo=asciidoctor&logoColor=E40046&labelColor=ffffff&color=f49b06)](https://graceframework.org/grace-data-hibernate/latest/)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://x.com/graceframework)

[![Groovy Version](https://img.shields.io/badge/Groovy-4.0.32-blue?style=flat&color=4298b8)](https://groovy-lang.org/releasenotes/groovy-4.0.html)
[![Grace Version](https://img.shields.io/badge/Grace-2024.1.0-blue?style=flat&color=f49b06)](https://github.com/graceframework/grace-framework/releases/tag/v2024.1.0-RC2)

# Grace Data for Hibernate

This project implements [GORM](https://github.com/graceframework/grace-data) for the [Hibernate ORM](https://hibernate.org/orm/).

> [!IMPORTANT]
> Currently, this plugin has been migrate to Jakarta Namespace, and support [Hibernate 5.6](https://hibernate.org/orm/releases/5.6/). Grace Framework and Grace Data 2024.2 will upgrade to Hibernate 6.x. 

From version 2023.0.0-M5, this plugin has been renamed from the original `hibernate5` to `hibernate`.

From version 2024.0.0-M1, `hibernate` plugin has been merged into Grace framework as a submodule `grace-boot-hibernate`.

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

    // After 2024.0.0-M1
    implementation "org.graceframework:grace-boot-hibernate"
}
```

## Versions

To make it easier for users to use and upgrade, Grace Data Hibernate adopts a version policy consistent with the [Grace Framework](https://github.com/graceframework/grace-framework).

| GORM Hibernate Version | Grace Version |
|------------------------|---------------|
| 2024.1.x               | 2024.1.x      |
| 2024.0.x               | 2024.0.x      |
| 2023.3.x               | 2023.3.x      |
| 2023.2.x               | 2023.2.x      |
| 2023.1.x               | 2023.1.x      |
| 2023.0.x               | 2023.0.x      |
| 2022.2.x               | 2022.2.x      |
| 2022.1.x               | 2022.1.x      |
| 2022.0.x               | 2022.0.x      |

## Ducumentation

* [2024.1.x](https://graceframework.org/grace-data-hibernate/2024.1.x/)

## License

This plugin is available as open source under the terms of the [APACHE LICENSE, VERSION 2.0](http://apache.org/Licenses/LICENSE-2.0)

## Links

- [Grace Framework](https://github.com/graceframework/grace-framework)
- [Grace Data](https://github.com/graceframework/grace-data)
- [Hibernate ORM](https://hibernate.org/orm/)
