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
package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.code.MakeStaticRefactoring;

public class MakeStaticInputPage extends UserInputWizardPage {

	private final MakeStaticRefactoring fRefactoring;
	private Text fOldText;
	private Text fNewText;

	public MakeStaticInputPage(MakeStaticRefactoring refactoring) {
		super("MyExampleRefactoringInputPage"); //$NON-NLS-1$
		fRefactoring= refactoring;
    }

    @Override
	public void createControl(Composite parent) {
    	Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setFont(parent.getFont());

		Label label1= new Label(composite, SWT.NONE);
		label1.setText("&Find:"); //$NON-NLS-1$
		label1.setLayoutData(new GridData());

		fOldText= new Text(composite, SWT.BORDER);
		fOldText.setText("A"); //$NON-NLS-1$
		fOldText.selectAll();
		fOldText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		Label label2= new Label(composite, SWT.NONE);
		label2.setText("&Replace with:"); //$NON-NLS-1$
		label2.setLayoutData(new GridData());

		fNewText= new Text(composite, SWT.BORDER);
		fNewText.setText("B"); //$NON-NLS-1$
		fNewText.selectAll();
		fNewText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		setControl(composite);

		Dialog.applyDialogFont(composite);
    }

    @Override
	protected boolean performFinish() {
		initializeRefactoring();
		storeSettings();
		return super.performFinish();
	}

	@Override
	public IWizardPage getNextPage() {
		initializeRefactoring();
		storeSettings();
		return super.getNextPage();
	}

	private void storeSettings() {
    }

	private void initializeRefactoring() {
		fRefactoring.setOldText(fOldText.getText());
		fRefactoring.setNewText(fNewText.getText());
    }
}