/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import org.eclipse.jdt.internal.corext.refactoring.code.MakeStaticRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.MakeStaticWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Extracts the code selected inside a compilation unit editor into a new method.
 * Necessary arguments, exceptions and returns values are computed and an
 * appropriate method signature is generated.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 3.29
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MakeStaticAction extends SelectionDispatchAction {

	private final JavaEditor fEditor;
	private IFile fFile;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 *
	 * @param editor the java editor
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public MakeStaticAction(JavaEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.MakeStaticAction_label);
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MAKE_STATIC_ACTION);
	}

	@Override
	public void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() == 0 ? false : fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param selection the Java text selection (internal type)
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public void selectionChanged(JavaTextSelection selection) {
		fFile= null;
			Object object= ((IStructuredSelection) selection).getFirstElement();
			if (object instanceof IFile) {
				fFile= (IFile) object;
			}

	}

	@Override
	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		MakeStaticRefactoring refactoring= new MakeStaticRefactoring(fFile);
		MakeStaticWizard refactoringWizard= new MakeStaticWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		RefactoringWizardOpenOperation op= new RefactoringWizardOpenOperation(refactoringWizard);
		try {
			op.run(shell, "Example refactoring"); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// refactoring got cancelled
		}
	}
}
