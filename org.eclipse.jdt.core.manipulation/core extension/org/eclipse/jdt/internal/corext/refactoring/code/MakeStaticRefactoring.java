/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *s
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ChangeCalculator;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ContextCalculator;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ContextCalculator.SelectionInputType;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.FinalConditionsChecker;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.InitialConditionsChecker;

/**
 *
 * The {@code MakeStaticRefactoring} class represents a refactoring operation to convert a method
 * into a static method. It provides the capability to transform an instance method into a static
 * method by modifying the method declaration, updating method invocations, and handling related
 * changes.
 *
 * @since 3.29
 *
 */
public class MakeStaticRefactoring extends Refactoring {

	/**
	 * The {@code IMethod} object representing the selected method on which the refactoring should
	 * be performed.
	 */
	private IMethod fTargetMethod;

	/**
	 * Provides all invocations of the refactored method in the workspace.
	 */
	private TargetProvider fTargetProvider;

	/**
	 * The {@code MethodDeclaration} object representing the selected method on which the
	 * refactoring should be performed. This field is used to analyze and modify the method's
	 * declaration during the refactoring process.
	 */
	private MethodDeclaration fTargetMethodDeclaration;

	/**
	 * Represents the status of a refactoring operation.
	 */
	private RefactoringStatus fStatus;

	/**
	 * The {@code IMethodBinding} object representing the binding of the refactored method.
	 */
	private IMethodBinding fTargetMethodBinding;

	private ContextCalculator fContextCalculator;

	private ChangeCalculator fChangeCalculator;

	public MakeStaticRefactoring(ICompilationUnit inputAsICompilationUnit, int selectionStart, int selectionLength) {
		Selection targetSelection= Selection.createFromStartLength(selectionStart, selectionLength);
		fContextCalculator= new ContextCalculator(inputAsICompilationUnit, targetSelection);
	}

	/**
	 * Constructs a new {@code MakeStaticRefactoring} object. This constructor is called when
	 * performing the refactoring on a method in the outline menu.
	 *
	 * @param method The target method the refactoring should be performed on.
	 */
	public MakeStaticRefactoring(IMethod method) {
		fContextCalculator= new ContextCalculator(method);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The name of the refactoring operation.
	 */
	@Override
	public String getName() {
		return RefactoringCoreMessages.MakeStaticRefactoring_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) {
		fStatus= new RefactoringStatus();

		SelectionInputType selectionInputType= fContextCalculator.getSelectionInputType();

		if (selectionInputType == SelectionInputType.TEXT_SELECTION) {
			fStatus.merge(InitialConditionsChecker.checkTextSelectionStart(fContextCalculator.getSelectionEditorText()));
			fStatus.merge(InitialConditionsChecker.checkValidICompilationUnit(fContextCalculator.getSelectionICompilationUnit()));
			if (fStatus.hasError()) {
				return fStatus;
			}

			fStatus.merge(InitialConditionsChecker.checkASTNodeIsValidMethod(fContextCalculator.getOrComputeSelectionASTNode()));
			if (fStatus.hasError()) {
				return fStatus;
			}
		}

		fStatus.merge(InitialConditionsChecker.checkSourceAvailable(fContextCalculator.getOrComputeTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkIMethodIsValid(fContextCalculator.getOrComputeTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkValidICompilationUnit(fContextCalculator.getOrComputeTargetICompilationUnit()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodIsNotConstructor(fContextCalculator.getOrComputeTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodNotInLocalOrAnonymousClass(fContextCalculator.getOrComputeTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodNotStatic(fContextCalculator.getOrComputeTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodNotOverridden(fContextCalculator.getOrComputeTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}


		fTargetMethod= fContextCalculator.getOrComputeTargetIMethod(); //TODO remove those which dont have to necessarily be fields
		fTargetMethodBinding= fContextCalculator.getOrComputeTargetIMethodBinding();

		try {
			fTargetMethodDeclaration= fContextCalculator.getOrComputeTargetMethodDeclaration();
		} catch (JavaModelException e) {
			fStatus.addFatalError(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
		}

		return fStatus;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor progressMonitor) throws CoreException {
		fTargetProvider= TargetProvider.create(fTargetMethodDeclaration);
		fTargetProvider.initialize();
		fChangeCalculator= new ChangeCalculator(fTargetMethodDeclaration, fTargetMethod, fTargetProvider, fTargetMethodBinding);

		fChangeCalculator.addStaticModifierToTargetMethod();
		fChangeCalculator.rewriteInstanceUsages();

		boolean targetMethodhasInstanceUsage= fChangeCalculator.getTargetMethodhasInstanceUsage();

		if (targetMethodhasInstanceUsage) {
			//Adding an instance parameter to the newly static method to ensure it can still access class-level state and behavior.
			fChangeCalculator.addInstanceAsParamIfUsed();
		}

		//check if method would unintentionally hide method of parent class
		fStatus.merge(FinalConditionsChecker.checkMethodWouldHideParentMethod(targetMethodhasInstanceUsage, fTargetMethod));
		//While refactoring the method signature might change; ensure the revised method doesn't unintentionally override an existing one.
		fStatus.merge(FinalConditionsChecker.checkMethodIsNotDuplicate(fTargetMethodDeclaration, fTargetMethod));

		//Updates typeParamList of MethodDeclaration and inserts new typeParams to JavaDoc
		fStatus.merge(fChangeCalculator.updateMethodTypeParamList());

		//A static method can't have override annotations
		fChangeCalculator.deleteOverrideAnnotation();
		fChangeCalculator.computeMethodDeclarationEdit();

		//Find and Modify MethodInvocations
		fStatus.merge(fChangeCalculator.handleMethodInvocations(progressMonitor));

		return fStatus;
	}



	/**
	 * {@inheritDoc}
	 *
	 * Creates a {@code Change} object representing the refactoring changes to be performed. This
	 * method constructs a composite change that encapsulates all the individual changes managed by
	 * the {@code fChangeManager}.
	 *
	 * @param progressMonitor The progress monitor used to track the progress of the operation.
	 * @return A {@code Change} object representing all refactoring changes.
	 * @throws CoreException If an error occurs during the creation of the change.
	 * @throws OperationCanceledException If the operation is canceled by the user.
	 */
	@Override
	public Change createChange(IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException {
		CompositeChange multiChange= new CompositeChange(RefactoringCoreMessages.MakeStaticRefactoring_name, fChangeCalculator.getChanges());
		return multiChange;
	}
}
