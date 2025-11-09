/*
 * Copyright 2016-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.gorm.tests.inheritance

import grails.gorm.annotation.Entity
import grails.gorm.tests.GormDatastoreSpec

class SubclassToOneProxySpec extends GormDatastoreSpec {

    void 'the hasOne is a proxy and unwraps'() {
        given:
        SubclassProxy dog = new SubclassProxy().save()
        new HasOneProxy(superclassProxy: dog).save()
        session.flush()
        session.clear()
        HasOneProxy owner = HasOneProxy.first()

        expect:
        session.mappingContext.proxyFactory.isProxy(owner.@superclassProxy)
    }

    @Override
    List getDomainClasses() {
        [SuperclassProxy, SubclassProxy, HasOneProxy]
    }

}

@Entity
class SuperclassProxy {

}

@Entity
class SubclassProxy extends SuperclassProxy {

}

@Entity
class HasOneProxy {

    SuperclassProxy superclassProxy

}
