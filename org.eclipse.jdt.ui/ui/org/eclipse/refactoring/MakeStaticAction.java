package org.eclipse.refactoring;

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

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;

import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;

/**
 * Extracts an expression into a constant field and replaces all occurrences of
 * the expression with the new constant.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MakeStaticAction extends SelectionDispatchAction {

	private final JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public MakeStaticAction(JavaEditor editor) {
		super(editor.getEditorSite());
		setText("Make Static..."); //$NON-NLS-1$
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "org.eclipse.refactoring.make_static_action"); //$NON-NLS-1$
	}

	@Override
	public void selectionChanged(ITextSelection selection) {
		setEnabled((fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null));
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param selection the Java text selection (internal type)
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public void selectionChanged(JavaTextSelection selection) {
	}

	@Override
	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		MakeStaticRefactoring refactoring= new MakeStaticRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection.getOffset(), selection.getLength());
		new RefactoringStarter().activate(new MakeStaticWizard(refactoring), getShell(), "Make static", RefactoringSaveHelper.SAVE_NOTHING); //$NON-NLS-1$
	}
}
