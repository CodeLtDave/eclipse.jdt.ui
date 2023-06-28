/*******************************************************************************
 * Copyright (c) 2023 David Erdös and Michael Bangas.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Erdös - initial API and implementation
 *     Michael Bangas - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

class MakeStaticInputPage extends UserInputWizardPage {

	public MakeStaticInputPage() {
		super(RefactoringCoreMessages.MakeStaticRefactoring_name);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gridData);
		composite.setFont(parent.getFont());


		Label label1= new Label(composite, SWT.NONE);
		label1.setText(RefactoringMessages.MakeStaticAction_description);
		label1.setLayoutData(new GridData());
		setControl(composite);

		Dialog.applyDialogFont(composite);
	}
}
