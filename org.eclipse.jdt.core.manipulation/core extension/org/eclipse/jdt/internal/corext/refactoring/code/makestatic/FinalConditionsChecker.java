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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;


/**
 * The FinalConditionsChecker class provides static methods to check various final conditions for a
 * refactoring.
 */
public class FinalConditionsChecker {
	/**
	 * Checks if a duplicate method with the same signature as the refactored method exists. The
	 * MakeStaticRefactoring introduces a new parameter if fields or instance methods are used in
	 * the body of the selected method. This check ensures that there is no conflict with the
	 * signature of another method.
	 *
	 * @param methodDeclaration the MethodDeclaration to be checked
	 *
	 * @param imethod the IMethod representing the method declaration
	 *
	 * @return the refactoring status indicating if a duplicate method was found.
	 *
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public static RefactoringStatus checkMethodIsNotDuplicate(MethodDeclaration methodDeclaration, IMethod imethod) throws JavaModelException {
		RefactoringStatus status= new RefactoringStatus();
		int parameterAmount= methodDeclaration.parameters().size() + 1;
		String methodName= methodDeclaration.getName().getIdentifier();
		IMethodBinding methodBinding= methodDeclaration.resolveBinding();
		ITypeBinding typeBinding= methodBinding.getDeclaringClass();
		IType type= (IType) typeBinding.getJavaElement();

		IMethod method= Checks.findMethod(methodName, parameterAmount, false, type);

		if (method == null) {
			return new RefactoringStatus();
		}

		//check if parameter types match (also compare new parameter that is added by refactoring)
		String className= ((TypeDeclaration) methodDeclaration.getParent()).getName().toString();
		String extendedClassName= "Q" + className + ";"; //$NON-NLS-1$ //$NON-NLS-2$
		boolean contains;
		String[] paramTypesOfFoundMethod= method.getParameterTypes();
		String[] paramTypesOfSelectedMethodExtended= new String[parameterAmount];
		paramTypesOfSelectedMethodExtended[0]= extendedClassName;
		String[] paramTypesOfSelectedMethod= imethod.getParameterTypes();

		for (int parameterNumber= 0; parameterNumber < paramTypesOfSelectedMethod.length; parameterNumber++) {
			paramTypesOfSelectedMethodExtended[parameterNumber + 1]= paramTypesOfSelectedMethod[parameterNumber];
		}

		for (int parameterNumber= 0; parameterNumber < paramTypesOfFoundMethod.length; parameterNumber++) {
			contains= paramTypesOfSelectedMethodExtended[parameterNumber].equals(paramTypesOfFoundMethod[parameterNumber]);
			if (!contains) {
				return status;
			}
		}
		status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_duplicate_method_signature));
		return status;
	}

	/**
	 * Checks if a method that overrides a method of parent type will not hide the parent method
	 * after MakeStaticRefactoring.
	 *
	 * @param methodhasInstanceUsage indicates if the method has any instance usage
	 * @param iMethod the IMethod to be checked
	 * @return the refactoring status indicating the if the method would hide its parent method
	 * @throws JavaModelException if an exception occurs while accessing the Java model
	 */
	public static RefactoringStatus checkMethodWouldHideParentMethod(boolean methodhasInstanceUsage, IMethod iMethod) throws JavaModelException {
		RefactoringStatus status= new RefactoringStatus();
		if (!methodhasInstanceUsage && isOverriding(iMethod.getDeclaringType(), iMethod)) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
		}
		return status;
	}


	/**
	 * Checks if the bound does not contains a wildcard type.
	 *
	 * @param bound the bound to be checked
	 * @return the refactoring status indicating if the bound contains a wildcard type
	 */
	public static RefactoringStatus checkBoundNotContainingWildCardType(String bound) {
		RefactoringStatus status= new RefactoringStatus();
		if (bound.contains("?")) { //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_wildCardTypes_as_bound));
		}
		return status;
	}

	/**
	 * Checks if the method reference is not referring to the specified method.
	 *
	 * @param methodReference the MethodReference to be checked
	 * @param targetMethodBinding the IMethodBinding representing the target method
	 * @return the refactoring status indicating if the method references refers to the target
	 *         method
	 */
	public static RefactoringStatus checkMethodReferenceNotReferingToMethod(MethodReference methodReference, IMethodBinding targetMethodBinding) {
		RefactoringStatus status= new RefactoringStatus();
		IMethodBinding methodReferenceBinding= methodReference.resolveMethodBinding();
		ITypeBinding typeBindingOfMethodReference= methodReferenceBinding.getDeclaringClass();
		ITypeBinding typeBindingOfTargetMethod= targetMethodBinding.getDeclaringClass();
		if (targetMethodBinding.isEqualTo(methodReferenceBinding) && typeBindingOfMethodReference.isEqualTo(typeBindingOfTargetMethod)) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
		}
		return status;
	}

	private static boolean isOverriding(IType type, IMethod iMethod) throws JavaModelException { //TODO duplicate isOverriding()?
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] supertypes= hierarchy.getAllSupertypes(type);
		for (IType supertype : supertypes) {
			IMethod[] methods= supertype.getMethods();
			for (IMethod method : methods) {
				if (method.isSimilar(iMethod)) {
					return true;
				}
			}
		}
		return false;
	}
}
