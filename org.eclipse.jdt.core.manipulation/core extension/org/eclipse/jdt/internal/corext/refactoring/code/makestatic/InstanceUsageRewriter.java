/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 *
 * The InstanceUsageRewriter class represents a utility class for the MakeStaticRefactoring class.
 * It is used for rewriting instance usages within a method.
 *
 * <p>
 * This class extends {@code ASTVisitor} and provides functionality to determine whether there is
 * access to instance variables or instance methods within the body of the method being visited.
 * </p>
 *
 * It also provides functionality to transform class instance creations.
 *
 * If an explicit super method invocation will be found by this visitor a status error will be
 * thrown.
 *
 */
public final class InstanceUsageRewriter extends ASTVisitor {

	/**
	 * Indicates whether there is access to instance variables or instance methods within the body
	 * of the method.
	 */
	public boolean fTargetMethodhasInstanceUsage;

	private final RefactoringStatus fstatus;

	/**
	 * The name of the parameter that is used to access instance variables or instance methods after
	 * refactoring. For example "this" and "super" will be transformed to the value of fParamName.
	 */
	private final String fParamName;

	private final ASTRewrite fRewrite;

	private final AST fAst;

	private final MethodDeclaration fTargetMethodDeclaration;


	/**
	 * Constructs a new InstanceUsageRewriter with the specified parameters.
	 *
	 * @param paramName The name of the parameter used to access instance variables or instance
	 *            methods.
	 * @param rewrite The ASTRewrite object used for rewriting the AST.
	 * @param ast The AST object representing the abstract syntax tree.
	 * @param status The RefactoringStatus object to track the status of the refactoring.
	 * @param methodDeclaration The MethodDeclaration object representing the target method being
	 *            refactored.
	 */
	public InstanceUsageRewriter(String paramName, ASTRewrite rewrite, AST ast, RefactoringStatus status, MethodDeclaration methodDeclaration) {
		fParamName= paramName;
		fRewrite= rewrite;
		fAst= ast;
		fstatus= status;
		fTargetMethodDeclaration= methodDeclaration;
	}

	public boolean getTargetMethodhasInstanceUsage() {
		return fTargetMethodhasInstanceUsage;
	}

	@Override
	public boolean visit(SimpleName node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding) {
			modifyFieldUsage(node, binding);
		} else if (binding instanceof IMethodBinding) {
			modifyInstanceMethodUsage(node, binding);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(ThisExpression node) {
		ASTNode parentNode= node.getParent();

		Name qualifier= node.getQualifier();

		if (qualifier != null) {
			IBinding qualifierBinding= qualifier.resolveBinding();
			ITypeBinding typeBinding= (ITypeBinding) qualifierBinding;
			if (isInsideAnonymousClass(typeBinding)) {
				return super.visit(node);
			}
		} else {
			if (parentIsAnonymousClass(parentNode)) {
				return super.visit(node);
			}
		}

		// 'this' keyword is not inside an anonymous class
		replaceThisExpression(node);

		return super.visit(node);
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		ITypeBinding typeBinding= node.getType().resolveBinding();
		if (typeBinding != null && typeBinding.isMember() && !Modifier.isStatic(typeBinding.getModifiers())) {
			fTargetMethodhasInstanceUsage= true;
			replaceClassInstanceCreation(node);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		fstatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_explicit_super_method_invocation));
		return super.visit(node);
	}

	private void modifyFieldUsage(SimpleName node, IBinding binding) {
		IVariableBinding variableBinding= (IVariableBinding) binding;

		//Check if we are inside a anonymous class
		ITypeBinding declaringClass= variableBinding.getDeclaringClass();
		if (isInsideAnonymousClass(declaringClass)) {
			return;
		}

		if (variableBinding.isField() && !Modifier.isStatic(variableBinding.getModifiers())) {
			ASTNode parent= node.getParent();

			//this ensures only the leftmost SimpleName or QualifiedName gets changed see "testConcatenatedFieldAccessAndQualifiedNames"
			if (isConcatenatedFieldAccessOrQualifiedName(node, parent)) {
				return;
			}

			replaceFieldAccess(node, parent);

			fTargetMethodhasInstanceUsage= true;
		}
	}

	private void modifyInstanceMethodUsage(SimpleName node, IBinding binding) {
		IMethodBinding methodBinding= (IMethodBinding) binding;
		ITypeBinding declaringClass= methodBinding.getDeclaringClass();
		//Check if we are inside a anonymous class
		if (isInsideAnonymousClass(declaringClass)) {
			return;
		}

		if (!Modifier.isStatic(methodBinding.getModifiers())) {
			fTargetMethodhasInstanceUsage= true;

			//check if method is recursive
			if (isRecursive(node)) {
				fstatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
				return;
			}

			replaceMethodInvocation(node);
		}
	}

	private boolean isInsideAnonymousClass(ITypeBinding declaringClass) {
		if (declaringClass != null) {
			boolean isLocal= declaringClass.isLocal();
			boolean isAnonymous= declaringClass.isAnonymous();
			boolean isMember= declaringClass.isMember();
			boolean isNested= declaringClass.isNested();
			boolean isTopLevel= declaringClass.isTopLevel();
			if (!isTopLevel || isNested || isAnonymous || isLocal || isMember) {
				return true;
			}
		}
		return false;
	}

	private boolean isConcatenatedFieldAccessOrQualifiedName(SimpleName node, ASTNode parent) {
		if (parent instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess) parent;
			if (fieldAccess.getExpression() != node) {
				return true;
			}
		} else if (parent instanceof QualifiedName) {
			QualifiedName qualifiedName= (QualifiedName) parent;
			if (qualifiedName.getQualifier() != node) {
				return true;
			}
		}
		return false;
	}

	private void replaceFieldAccess(SimpleName node, ASTNode parent) {
		FieldAccess replacement= fAst.newFieldAccess();
		replacement.setExpression(fAst.newSimpleName(fParamName));
		replacement.setName(fAst.newSimpleName(node.getIdentifier()));

		if (parent instanceof SuperFieldAccess) {
			//Warning is needed to inform user of possible changes in semantics when SuperFieldAccess is found
			fstatus.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.MakeStaticRefactoring_selected_method_uses_super_field_access));
			fRewrite.replace(parent, replacement, null);
		} else {
			fRewrite.replace(node, replacement, null);
		}
	}

	private boolean isRecursive(SimpleName node) {
		IMethodBinding nodeMethodBinding= (IMethodBinding) node.resolveBinding();
		IMethodBinding outerMethodBinding= fTargetMethodDeclaration.resolveBinding();

		if (nodeMethodBinding.isEqualTo(outerMethodBinding)) {
			return true;
		}
		return false;
	}

	private void replaceMethodInvocation(SimpleName node) {
		ASTNode parent= node.getParent();
		SimpleName replacementExpression= fAst.newSimpleName(fParamName);
		if (parent instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation) parent;
			Expression optionalExpression= methodInvocation.getExpression();

			if (optionalExpression == null) {
				fRewrite.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, replacementExpression, null);
			}
		}
	}

	private boolean parentIsAnonymousClass(ASTNode parentNode) {
		while (parentNode != null) {
			if (parentNode instanceof AnonymousClassDeclaration) {
				// 'this' keyword is inside an anonymous class, skip
				return true;
			}
			parentNode= parentNode.getParent();
		}
		return false;
	}

	private void replaceThisExpression(ThisExpression node) {
		fTargetMethodhasInstanceUsage= true;
		SimpleName replacement= fAst.newSimpleName(fParamName);
		fRewrite.replace(node, replacement, null);
	}

	private void replaceClassInstanceCreation(ClassInstanceCreation node) {
		ClassInstanceCreation replacement= fAst.newClassInstanceCreation();
		replacement.setType((Type) ASTNode.copySubtree(fAst, node.getType()));
		replacement.setExpression(fAst.newSimpleName(fParamName));
		for (Object arg : node.arguments()) {
			replacement.arguments().add(ASTNode.copySubtree(fAst, (Expression) arg));
		}
		fRewrite.replace(node, replacement, null);
	}
}
