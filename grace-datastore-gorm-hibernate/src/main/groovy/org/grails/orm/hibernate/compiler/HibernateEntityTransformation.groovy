/*
 * Copyright 2003-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.compiler

import java.lang.reflect.Modifier

import jakarta.persistence.Transient

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.sc.StaticCompilationVisitor
import org.hibernate.engine.spi.EntityEntry
import org.hibernate.engine.spi.ManagedEntity
import org.hibernate.engine.spi.PersistentAttributeInterceptable
import org.hibernate.engine.spi.PersistentAttributeInterceptor

import grails.gorm.dirty.checking.DirtyCheckedProperty

import org.grails.compiler.gorm.GormEntityTransformation
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.reflect.AstUtils
import org.grails.datastore.mapping.reflect.NameUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.equalsNullX
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifS
import static org.codehaus.groovy.ast.tools.GeneralUtils.neX
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GeneralUtils.params
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.ternaryX
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

/**
 * A transformation that transforms entities that implement the {@link grails.gorm.hibernate.annotation.ManagedEntity} trait,
 * adding logic that intercepts getter and setter access to eliminate the need for proxies.
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class HibernateEntityTransformation implements ASTTransformation, CompilationUnitAware {

    private static final ClassNode MY_TYPE = new ClassNode(grails.gorm.hibernate.annotation.ManagedEntity.class);
    private static final Object APPLIED_MARKER = new Object();

//    final boolean available = ClassUtils.isPresent("org.hibernate.SessionFactory") && Boolean.valueOf(System.getProperty("hibernate.enhance", "true"))
    CompilationUnit compilationUnit

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];

        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: ${node.getClass()} / ${parent.getClass()}");
        }

        if (!MY_TYPE.equals(node.getClassNode()) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode cNode = (ClassNode) parent;

        visit(cNode, sourceUnit)
    }

    void visit(ClassNode classNode, SourceUnit sourceUnit) {
        if (classNode.getNodeMetaData(AstUtils.TRANSFORM_APPLIED_MARKER) == APPLIED_MARKER) {
            return
        }

        if ((classNode instanceof InnerClassNode) || classNode.isEnum()) {
            // do not apply transform to enums or inner classes
            return
        }

        def mapWith = AstUtils.getPropertyFromHierarchy(classNode, GormProperties.MAPPING_STRATEGY)
        String mapWithValue = mapWith?.initialExpression?.text

        if (mapWithValue != null && (mapWithValue != ('hibernate') || mapWithValue != GormProperties.DEFAULT_MAPPING_STRATEGY)) {
            return
        }

        new GormEntityTransformation(compilationUnit: compilationUnit).visit(classNode, sourceUnit)

        ClassNode managedEntityClassNode = ClassHelper.make(ManagedEntity)
        ClassNode attributeInterceptableClassNode = ClassHelper.make(PersistentAttributeInterceptable)
        ClassNode entityEntryClassNode = ClassHelper.make(EntityEntry)
        ClassNode persistentAttributeInterceptorClassNode = ClassHelper.make(PersistentAttributeInterceptor)

        classNode.addInterface(managedEntityClassNode)
        classNode.addInterface(attributeInterceptableClassNode)
        String interceptorFieldName = '$$_hibernate_attributeInterceptor'
        String entryHolderFieldName = '$$_hibernate_entityEntryHolder'
        String previousManagedEntityFieldName = '$$_hibernate_previousManagedEntity'
        String nextManagedEntityFieldName = '$$_hibernate_nextManagedEntity'

        def staticCompilationVisitor = new StaticCompilationVisitor(sourceUnit, classNode)

        AnnotationNode transientAnnotationNode = new AnnotationNode(ClassHelper.make(Transient.class))
        FieldNode entityEntryHolderField = classNode.addField(entryHolderFieldName, Modifier.PRIVATE | Modifier.TRANSIENT, entityEntryClassNode, null)
        entityEntryHolderField
                .addAnnotation(transientAnnotationNode)

        FieldNode previousManagedEntityField = classNode.addField(previousManagedEntityFieldName, Modifier.PRIVATE | Modifier.TRANSIENT, managedEntityClassNode, null)
        previousManagedEntityField
                .addAnnotation(transientAnnotationNode)

        FieldNode nextManagedEntityField = classNode.addField(nextManagedEntityFieldName, Modifier.PRIVATE | Modifier.TRANSIENT, managedEntityClassNode, null)
        nextManagedEntityField
                .addAnnotation(transientAnnotationNode)

        FieldNode interceptorField = classNode.addField(interceptorFieldName, Modifier.PRIVATE | Modifier.TRANSIENT, persistentAttributeInterceptorClassNode, null)
        interceptorField
                .addAnnotation(transientAnnotationNode)


        // add method: PersistentAttributeInterceptor $$_hibernate_getInterceptor()
        def getInterceptorMethod = new MethodNode(
                '$$_hibernate_getInterceptor',
                Modifier.PUBLIC,
                persistentAttributeInterceptorClassNode,
                AstUtils.ZERO_PARAMETERS,
                null,
                returnS(varX(interceptorField))
        )
        classNode.addMethod(getInterceptorMethod)
        staticCompilationVisitor.visitMethod(getInterceptorMethod)

        // add method: void $$_hibernate_setInterceptor(PersistentAttributeInterceptor interceptor)
        def p1 = param(persistentAttributeInterceptorClassNode, "interceptor")
        def setInterceptorMethod = new MethodNode(
                '$$_hibernate_setInterceptor',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                params(p1),
                null,
                assignS(varX(interceptorField), varX(p1))
        )
        classNode.addMethod(setInterceptorMethod)
        staticCompilationVisitor.visitMethod(setInterceptorMethod)

        // add method: Object $$_hibernate_getEntityInstance()
        def getEntityInstanceMethod = new MethodNode(
                '$$_hibernate_getEntityInstance',
                Modifier.PUBLIC,
                ClassHelper.OBJECT_TYPE,
                AstUtils.ZERO_PARAMETERS,
                null,
                returnS(varX("this"))
        )
        classNode.addMethod(getEntityInstanceMethod)
        staticCompilationVisitor.visitMethod(getEntityInstanceMethod)


        // add method: EntityEntry $$_hibernate_getEntityEntry()
        def getEntityEntryMethod = new MethodNode(
                '$$_hibernate_getEntityEntry',
                Modifier.PUBLIC,
                entityEntryClassNode,
                AstUtils.ZERO_PARAMETERS,
                null,
                returnS(varX(entityEntryHolderField))
        )
        classNode.addMethod(getEntityEntryMethod)
        staticCompilationVisitor.visitMethod(getEntityEntryMethod)

        // add method: void $$_hibernate_setEntityEntry(EntityEntry entityEntry)
        def entityEntryParam = param(entityEntryClassNode, "entityEntry")
        def setEntityEntryMethod = new MethodNode(
                '$$_hibernate_setEntityEntry',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                params(entityEntryParam),
                null,
                assignS(varX(entityEntryHolderField), varX(entityEntryParam))
        )
        classNode.addMethod(setEntityEntryMethod)
        staticCompilationVisitor.visitMethod(setEntityEntryMethod)

        // add method: ManagedEntity $$_hibernate_getPreviousManagedEntity()
        def getPreviousManagedEntityMethod = new MethodNode(
                '$$_hibernate_getPreviousManagedEntity',
                Modifier.PUBLIC,
                managedEntityClassNode,
                AstUtils.ZERO_PARAMETERS,
                null,
                returnS(varX(previousManagedEntityField))
        )
        classNode.addMethod(getPreviousManagedEntityMethod)
        staticCompilationVisitor.visitMethod(getPreviousManagedEntityMethod)

        // add method: ManagedEntity $$_hibernate_getNextManagedEntity() {
        def getNextManagedEntityMethod = new MethodNode(
                '$$_hibernate_getNextManagedEntity',
                Modifier.PUBLIC,
                managedEntityClassNode,
                AstUtils.ZERO_PARAMETERS,
                null,
                returnS(varX(nextManagedEntityField))
        )
        classNode.addMethod(getNextManagedEntityMethod)
        staticCompilationVisitor.visitMethod(getNextManagedEntityMethod)

        // add method: void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous)
        def previousParam = param(managedEntityClassNode, "previous")
        def setPreviousManagedEntityMethod = new MethodNode(
                '$$_hibernate_setPreviousManagedEntity',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                params(previousParam),
                null,
                assignS(varX(previousManagedEntityField), varX(previousParam))
        )
        classNode.addMethod(setPreviousManagedEntityMethod)
        staticCompilationVisitor.visitMethod(setPreviousManagedEntityMethod)

        // add method: void $$_hibernate_setNextManagedEntity(ManagedEntity next)
        def nextParam = param(managedEntityClassNode, "next")
        def setNextManagedEntityMethod = new MethodNode(
                '$$_hibernate_setNextManagedEntity',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                params(nextParam),
                null,
                assignS(varX(nextManagedEntityField), varX(nextParam))
        )
        classNode.addMethod(setNextManagedEntityMethod)
        staticCompilationVisitor.visitMethod(setNextManagedEntityMethod)

        List<MethodNode> allMethods = classNode.getMethods()
        for (MethodNode methodNode in allMethods) {
            if (methodNode.getAnnotations(ClassHelper.make(DirtyCheckedProperty))) {
                if (AstUtils.isGetter(methodNode)) {
                    def codeVisitor = new ClassCodeVisitorSupport() {

                        @Override
                        protected SourceUnit getSourceUnit() {
                            return sourceUnit
                        }

                        @Override
                        void visitReturnStatement(ReturnStatement statement) {
                            ReturnStatement rs = (ReturnStatement) statement
                            def i = varX(interceptorField)
                            def propertyName = NameUtils.getPropertyNameForGetterOrSetter(methodNode.getName())

                            def returnType = methodNode.getReturnType()
                            final boolean isPrimitive = ClassHelper.isPrimitiveType(returnType)
                            String readMethodName = isPrimitive ? "read${NameUtils.capitalize(returnType.getName())}" : "readObject"
                            def readObjectCall = callX(i, readMethodName, args(varX("this"), constX(propertyName), rs.getExpression()))
                            def ternaryExpr = ternaryX(
                                    equalsNullX(varX(interceptorField)),
                                    rs.getExpression(),
                                    readObjectCall
                            )
                            staticCompilationVisitor.visitTernaryExpression ternaryExpr
                            rs.setExpression(ternaryExpr)

                        }
                    }
                    codeVisitor.visitMethod(methodNode)
                }
                else {
                    Statement code = methodNode.code
                    if (code instanceof BlockStatement) {
                        BlockStatement bs = (BlockStatement) code
                        Parameter parameter = methodNode.getParameters()[0]
                        ClassNode parameterType = parameter.type
                        final boolean isPrimitive = ClassHelper.isPrimitiveType(parameterType)
                        String writeMethodName = isPrimitive ? "write${NameUtils.capitalize(parameterType.getName())}" : "writeObject"
                        String propertyName = NameUtils.getPropertyNameForGetterOrSetter(methodNode.getName())
                        def interceptorFieldExpr = fieldX(interceptorField)
                        def ifStatement = ifS(neX(interceptorFieldExpr, constX(null)),
                                assignS(
                                        varX(parameter),
                                        callX(interceptorFieldExpr, writeMethodName, args(varX("this"), constX(propertyName), propX(varX("this"), propertyName), varX(parameter)))
                                )
                        )
                        staticCompilationVisitor.visitIfElse((IfStatement) ifStatement)
                        bs.getStatements().add(0, ifStatement)
                    }
                }

            }
        }

        classNode.putNodeMetaData(AstUtils.TRANSFORM_APPLIED_MARKER, APPLIED_MARKER)
    }
}