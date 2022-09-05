package org.grails.orm.hibernate.access;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.groovy.transform.trait.Traits;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;
import org.hibernate.property.access.spi.SetterMethodImpl;
import org.springframework.util.ReflectionUtils;

import org.grails.datastore.mapping.reflect.NameUtils;

/**
 * Support reading and writing trait fields with Hibernate 5+
 *
 * @author Graeme Rocher
 * @since 6.1.3
 */
public class TraitPropertyAccessStrategy implements PropertyAccessStrategy {

    @Override
    public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName) {
        Method readMethod = ReflectionUtils.findMethod(containerJavaType, NameUtils.getGetterName(propertyName));
        if (readMethod == null) {
            throw new IllegalStateException("TraitPropertyAccessStrategy used on property [" + propertyName + "] of class [" + containerJavaType.getName() + "] that is not provided by a trait!");
        }
        else {
            Traits.Implemented traitImplemented = readMethod.getAnnotation(Traits.Implemented.class);
            final String traitFieldName;
            if (traitImplemented == null) {
                Traits.TraitBridge traitBridge = readMethod.getAnnotation(Traits.TraitBridge.class);
                if (traitBridge != null) {
                    traitFieldName = getTraitFieldName(traitBridge.traitClass(), propertyName);
                }
                else {
                    throw new IllegalStateException("TraitPropertyAccessStrategy used on property [" + propertyName + "] of class [" + containerJavaType.getName() + "] that is not provided by a trait!");
                }
            }
            else {
                traitFieldName = getTraitFieldName(readMethod.getDeclaringClass(), propertyName);
            }


            Field field = ReflectionUtils.findField(containerJavaType, traitFieldName);
            final Getter getter;
            final Setter setter;
            if (field == null) {
                getter = new GetterMethodImpl(containerJavaType, propertyName, readMethod);
                Method writeMethod = ReflectionUtils.findMethod(containerJavaType, NameUtils.getSetterName(propertyName), readMethod.getReturnType());
                setter = new SetterMethodImpl(containerJavaType, propertyName, writeMethod);
            }
            else {

                getter = new GetterFieldImpl(containerJavaType, propertyName, field);
                setter = new SetterFieldImpl(containerJavaType, propertyName, field);
            }

            return new PropertyAccess() {
                @Override
                public PropertyAccessStrategy getPropertyAccessStrategy() {
                    return TraitPropertyAccessStrategy.this;
                }

                @Override
                public Getter getGetter() {
                    return getter;
                }

                @Override
                public Setter getSetter() {
                    return setter;
                }
            };
        }
    }

    private String getTraitFieldName(Class traitClass, String fieldName) {
        return traitClass.getName().replace('.', '_') + "__" + fieldName;
    }

}
