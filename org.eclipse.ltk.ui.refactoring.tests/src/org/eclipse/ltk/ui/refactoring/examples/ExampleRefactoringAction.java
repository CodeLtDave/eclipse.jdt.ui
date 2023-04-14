/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.ui.refactoring.examples;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;

/* In plugin.xml:
   <extension
         point="org.eclipse.ui.popupMenus">
      <objectContribution
            objectClass="org.eclipse.core.resources.IFile"
			adaptable="true"
            id="org.eclipse.ltk.ui.refactoring.examples.ExampleRefactoringAction">
         <action
               label="Replace content... (ltk.ui.refactoring.examples)"
               tooltip="Replace content... (ltk.ui.refactoring.examples)"
               class="org.eclipse.ltk.ui.refactoring.examples.ExampleRefactoringAction"
               menubarPath="ExampleRefactoringAction"
               enablesFor="1"
               id="ExampleRefactoringAction">
         </action>
      </objectContribution>
   </extension>
 */

public class ExampleRefactoringAction extends SelectionDispatchAction {

	private IMethod fMethod;
	private JavaEditor fEditor;

	public ExampleRefactoringAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public ExampleRefactoringAction(IWorkbenchSite site) {
		super(site);
	}

	@Override
	public void run(IStructuredSelection selection) {
			try {
				fMethod= getSingleSelectedMethod(selection);
				ExampleRefactoring refactoring= new ExampleRefactoring(fMethod);
				ExampleRefactoringWizard refactoringWizard= new ExampleRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
				Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				RefactoringWizardOpenOperation op= new RefactoringWizardOpenOperation(refactoringWizard);
				op.run(shell, "Example refactoring");
			} catch (InterruptedException e) {
				// refactoring got cancelled
			}
		}

	@Override
	public void run(ITextSelection selection) {
			try {
				fMethod= getSingleSelectedMethod(selection);
				ExampleRefactoring refactoring= new ExampleRefactoring(fMethod);
				ExampleRefactoringWizard refactoringWizard= new ExampleRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
				Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				RefactoringWizardOpenOperation op= new RefactoringWizardOpenOperation(refactoringWizard);
				op.run(shell, "Example refactoring");
			} catch (InterruptedException | JavaModelException e) {
				// refactoring got cancelled
			}
		}


	//Need to implement check if Refactoring is available --> see ModifyParameterAction
	@Override
	public void selectionChanged(IStructuredSelection selection) {
	}

	//Need to implement check if Refactoring is available --> see ModifyParameterAction
	@Override
	public void selectionChanged(ITextSelection selection) {
	}

	//Need to implement check if Refactoring is available --> see ModifyParameterAction
	@Override
	public void selectionChanged(JavaTextSelection selection) {
	}

	private static IMethod getSingleSelectedMethod(IStructuredSelection selection){
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		if (selection.getFirstElement() instanceof IMethod)
			return (IMethod)selection.getFirstElement();
		return null;
	}

	private IMethod getSingleSelectedMethod(ITextSelection selection) throws JavaModelException{
		//- when caret/selection on method name (call or declaration) -> that method
		//- otherwise: caret position's enclosing method declaration
		//  - when caret inside argument list of method declaration -> enclosing method declaration
		//  - when caret inside argument list of method call -> enclosing method declaration (and NOT method call)
		IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
		if (elements.length > 1)
			return null;
		if (elements.length == 1 && elements[0] instanceof IMethod)
			return (IMethod)elements[0];
		IJavaElement elementAt= SelectionConverter.getInputAsCompilationUnit(fEditor).getElementAt(selection.getOffset());
		if (elementAt instanceof IMethod)
			return (IMethod)elementAt;
		return null;
	}

}
