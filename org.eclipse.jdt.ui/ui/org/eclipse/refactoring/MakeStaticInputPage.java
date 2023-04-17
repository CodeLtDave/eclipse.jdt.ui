
package org.eclipse.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

class MakeStaticInputPage extends UserInputWizardPage {

	private final MakeStaticRefactoring fRefactoring;

	public MakeStaticInputPage(MakeStaticRefactoring refactoring) {
		super("MyMakeStaticRefactoringInputPage"); //$NON-NLS-1$
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


		Label label2= new Label(composite, SWT.NONE);
		label2.setText("&Replace with:"); //$NON-NLS-1$
		label2.setLayoutData(new GridData());


		setControl(composite);

		Dialog.applyDialogFont(composite);
    }

    @Override
	protected boolean performFinish() {
		return super.performFinish();
	}

	@Override
	public IWizardPage getNextPage() {
		return super.getNextPage();
	}

}