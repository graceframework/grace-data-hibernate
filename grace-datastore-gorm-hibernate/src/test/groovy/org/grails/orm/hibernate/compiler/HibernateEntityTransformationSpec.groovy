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
package org.grails.orm.hibernate.compiler

import org.hibernate.engine.spi.ManagedEntity
import org.hibernate.engine.spi.PersistentAttributeInterceptable
import org.hibernate.engine.spi.PersistentAttributeInterceptor
import spock.lang.Specification

/**
 * Created by graemerocher on 15/11/16.
 */
class HibernateEntityTransformationSpec extends Specification {

    void 'test hibernate entity transformation'() {
        when: 'A hibernate interceptor is set'
        Class cls = new GroovyClassLoader().parseClass('''
import grails.gorm.hibernate.annotation.ManagedEntity
@ManagedEntity
class MyEntity {
    String name
    String lastName
    int age

    String getLastName() {
        return this.lastName
    }

    void setLastName(String name) {
        this.lastName = name
    }
}
''')
        then:
        PersistentAttributeInterceptable.isAssignableFrom(cls)
        ManagedEntity.isAssignableFrom(cls)

        when:
        Object myEntity = cls.newInstance()

        ((PersistentAttributeInterceptable) myEntity).$$_hibernate_setInterceptor(
                new PersistentAttributeInterceptor() {

                    @Override
                    boolean readBoolean(Object obj, String name, boolean oldValue) {
                    }

                    @Override
                    boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
                        return false
                    }

                    @Override
                    byte readByte(Object obj, String name, byte oldValue) {
                        return 0
                    }

                    @Override
                    byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
                        return 0
                    }

                    @Override
                    char readChar(Object obj, String name, char oldValue) {
                        return 0
                    }

                    @Override
                    char writeChar(Object obj, String name, char oldValue, char newValue) {
                        return 0
                    }

                    @Override
                    short readShort(Object obj, String name, short oldValue) {
                        return 0
                    }

                    @Override
                    short writeShort(Object obj, String name, short oldValue, short newValue) {
                        return 0
                    }

                    @Override
                    int readInt(Object obj, String name, int oldValue) {
                        return 10
                    }

                    @Override
                    int writeInt(Object obj, String name, int oldValue, int newValue) {
                        return 10
                    }

                    @Override
                    float readFloat(Object obj, String name, float oldValue) {
                        return 0
                    }

                    @Override
                    float writeFloat(Object obj, String name, float oldValue, float newValue) {
                        return 0
                    }

                    @Override
                    double readDouble(Object obj, String name, double oldValue) {
                        return 0
                    }

                    @Override
                    double writeDouble(Object obj, String name, double oldValue, double newValue) {
                        return 0
                    }

                    @Override
                    long readLong(Object obj, String name, long oldValue) {
                        return 0
                    }

                    @Override
                    long writeLong(Object obj, String name, long oldValue, long newValue) {
                        return 0
                    }

                    @Override
                    Object readObject(Object obj, String name, Object oldValue) {
                        return 'good'
                    }

                    @Override
                    Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
                        return 'changed'
                    }

                    @Override
                    Set<String> getInitializedLazyAttributeNames() {
                        return Collections.emptySet()
                    }

                    @Override
                    void attributeInitialized(String name) {
                    }

                }
        )

        then: 'the interceptor is used when reading a property'
        myEntity.name == 'good'
        myEntity.lastName == 'good'
        myEntity.age == 10

        when: 'A setter is set'
        myEntity.name = 'something'
        myEntity.age = 5
        ((PersistentAttributeInterceptable) myEntity).$$_hibernate_setInterceptor(null)

        then: 'The value is changed'
        myEntity.name == 'changed'
    }

}
