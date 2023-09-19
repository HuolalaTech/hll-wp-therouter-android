package com.therouter.ksp

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSDynamicReference
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSParenthesizedReference
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.therouter.apt.BuildConfig.DEBUG

open class TheRouterVisitor(private val logger: KSPLogger) : KSVisitorVoid() {
    override fun visitDeclaration(declaration: KSDeclaration, data: Unit) {
        super.visitDeclaration(declaration, data)
        if (DEBUG) {
            logger.warn("visitDeclaration：" + declaration.containingFile?.filePath)
        }
    }

    override fun visitNode(node: KSNode, data: Unit) {
        super.visitNode(node, data)
        if (DEBUG) {
            logger.warn("visitNode：" + node.containingFile?.filePath)
        }
    }

    override fun visitAnnotated(annotated: KSAnnotated, data: Unit) {
        super.visitAnnotated(annotated, data)
        if (DEBUG) {
            logger.warn("visitNode:" + annotated.containingFile?.filePath)
        }
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: Unit) {
        super.visitAnnotation(annotation, data)
        if (DEBUG) {
            logger.warn("visitAnnotation:")
        }
    }

    override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: Unit) {
        super.visitModifierListOwner(modifierListOwner, data)
        if (DEBUG) {
            logger.warn("visitModifierListOwner:")
        }
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: Unit) {
        super.visitDeclarationContainer(declarationContainer, data)
        if (DEBUG) {
            logger.warn("visitDeclarationContainer:")
        }
    }

    override fun visitDynamicReference(reference: KSDynamicReference, data: Unit) {
        super.visitDynamicReference(reference, data)
        if (DEBUG) {
            logger.warn("visitDynamicReference:")
        }
    }

    override fun visitFile(file: KSFile, data: Unit) {
        super.visitFile(file, data)
        if (DEBUG) {
            logger.warn("visitFile:")
        }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        super.visitFunctionDeclaration(function, data)
        if (DEBUG) {
            logger.warn("visitFunctionDeclaration：" + function.qualifiedName?.asString())
        }
    }

    override fun visitCallableReference(reference: KSCallableReference, data: Unit) {
        super.visitCallableReference(reference, data)
        if (DEBUG) {
            logger.warn("visitCallableReference:")
        }
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: Unit) {
        super.visitParenthesizedReference(reference, data)
        if (DEBUG) {
            logger.warn("visitParenthesizedReference:")
        }
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
        super.visitPropertyDeclaration(property, data)
        if (DEBUG) {
            logger.warn("visitPropertyDeclaration：" + property.qualifiedName?.asString())
        }
    }

    override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: Unit) {
        super.visitPropertyAccessor(accessor, data)
        if (DEBUG) {
            logger.warn("visitPropertyAccessor:")
        }
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: Unit) {
        super.visitPropertyGetter(getter, data)
        if (DEBUG) {
            logger.warn("visitPropertyGetter:")
        }
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: Unit) {
        super.visitPropertySetter(setter, data)
        if (DEBUG) {
            logger.warn("visitPropertySetter:")
        }
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: Unit) {
        super.visitClassifierReference(reference, data)
        if (DEBUG) {
            logger.warn("visitClassifierReference:")
        }
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: Unit) {
        super.visitReferenceElement(element, data)
        if (DEBUG) {
            logger.warn("visitReferenceElement:")
        }
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: Unit) {
        super.visitTypeAlias(typeAlias, data)
        if (DEBUG) {
            logger.warn("visitTypeAlias:")
        }
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: Unit) {
        super.visitTypeArgument(typeArgument, data)
        if (DEBUG) {
            logger.warn("visitTypeArgument:")
        }
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: Unit) {
        super.visitTypeParameter(typeParameter, data)
        if (DEBUG) {
            logger.warn("visitTypeParameter:")
        }
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
        super.visitTypeReference(typeReference, data)
        if (DEBUG) {
            logger.warn("visitTypeReference:")
        }
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
        super.visitValueParameter(valueParameter, data)
        if (DEBUG) {
            logger.warn("visitValueParameter:")
        }
    }

    override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {
        super.visitValueArgument(valueArgument, data)
        if (DEBUG) {
            logger.warn("visitValueArgument:")
        }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        super.visitClassDeclaration(classDeclaration, data)
        if (DEBUG) {
            logger.warn("visitClassDeclaration：" + classDeclaration.qualifiedName?.asString())
        }
    }
}