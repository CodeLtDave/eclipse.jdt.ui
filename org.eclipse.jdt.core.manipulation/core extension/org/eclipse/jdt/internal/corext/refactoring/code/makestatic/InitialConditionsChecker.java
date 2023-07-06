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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * The InitialConditionsChecker class provides static methods to check various initial conditions
 * for a refactoring.
 */
class InitialConditionsChecker {

	/**
	 * Checks if the start position of a text selection is valid. A Selection is valid if the offset
	 * and lengt are greater zero.
	 *
	 * @param selection the text selection to be checked
	 *
	 * @return the refactoring status indicating the validity of the selection
	 */
	public static RefactoringStatus checkTextSelectionStart(Selection selection) {
		RefactoringStatus status= new RefactoringStatus();
		if (selection.getOffset() < 0 || selection.getLength() < 0) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return status;
	}

	/**
	 * Checks the validity of an ICompilationUnit. The ICompilationUnit is valid if it not null.
	 *
	 * @param iCompilationUnit the ICompilationUnit to be checked
	 * @return the refactoring status indicating the validity of the ICompilationUnit
	 */
	public static RefactoringStatus checkValidICompilationUnit(ICompilationUnit iCompilationUnit) {
		RefactoringStatus status= new RefactoringStatus();
		if (iCompilationUnit == null) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return status;
	}


	/**
	 * Checks the validity of an ASTNode as a method. The check fails if the AStNode is not an
	 * instance of MethodDeclaration or MethodInvocation. It also fails if the ASTNode is null or
	 * instance of SuperMethodInvocation.
	 *
	 * @param selectedNode the ASTNode to be checked
	 *
	 * @return the refactoring status indicating the validity of the selectedNode
	 */
	public static RefactoringStatus checkASTNodeIsValidMethod(ASTNode selectedNode) {
		RefactoringStatus status= new RefactoringStatus();
		if (selectedNode == null) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		} else if (selectedNode instanceof SuperMethodInvocation) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations));
		}
		if (!(selectedNode instanceof MethodDeclaration || selectedNode instanceof MethodInvocation)) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return status;
	}


	/**
	 * Checks the validity of an IMethod. The IMethod is valid if it is not null and if its
	 * declaring type is not an annotation.
	 *
	 * @param iMethod the IMethod to be checked
	 *
	 * @return the refactoring status indicating the validity of the IMethod
	 */
	public static RefactoringStatus checkIMethodIsValid(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		if (iMethod == null) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
		}

		try {
			if (iMethod.getDeclaringType().isAnnotation()) {
				status.addFatalError(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_annotation);
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Checks if the method is not declared in a local or anonymous class.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return the refactoring status indicating the validity of the method's declaring type
	 */
	public static RefactoringStatus checkMethodNotInLocalOrAnonymousClass(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		try {
			if (iMethod.getDeclaringType().isLocal() || iMethod.getDeclaringType().isAnonymous()) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types);
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Checks if the method is not a constructor.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return the refactoring status indicating the validity of the method
	 */
	public static RefactoringStatus checkMethodIsNotConstructor(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		try {
			if (iMethod.isConstructor()) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors);
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Checks if the method is not already static.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return the refactoring status indicating the validity of the method
	 */
	public static RefactoringStatus checkMethodNotStatic(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		try {
			int flags= iMethod.getFlags();
			if (Modifier.isStatic(flags)) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static);
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Checks if the method is not overridden in any subtype.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return the refactoring status indicating the validity of the method's override status
	 */
	public static RefactoringStatus checkMethodNotOverridden(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		try {
			if (isOverridden(iMethod.getDeclaringType(), iMethod)) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_is_overridden_in_subtype);
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Checks if the source code is available for the selected method.
	 *
	 * @param iMethod the IMethod to be checked
	 * @return the refactoring status indicating the availability of the source code
	 */
	public static RefactoringStatus checkSourceAvailable(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		if (iMethod.getCompilationUnit() == null) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_source_not_available_for_selected_method);
		}
		return status;
	}

	private static boolean isOverridden(IType type, IMethod iMethod) throws JavaModelException { //TODO duplicate isOverriding()?
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] subtypes= hierarchy.getAllSubtypes(type);
		for (IType subtype : subtypes) {
			IMethod[] methods= subtype.getMethods();
			for (IMethod method : methods) {
				if (method.isSimilar(iMethod)) {
					int flags= method.getFlags();
					if (!Flags.isPrivate(flags) || (!Flags.isStatic(flags))) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
