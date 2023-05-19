
package org.eclipse.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

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
    	  // Create the SWT controls for your InputPage
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());

        // Add the necessary widgets to collect user input

        // Create a composite for the buttons
        Composite buttonComposite = new Composite(composite, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, false));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Label label1= new Label(composite, SWT.NONE);
		label1.setText("Make selected method static"); //$NON-NLS-1$

		setControl(composite);

    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            // Adjust the size of the shell
            Shell shell = getShell();
            shell.setSize(500, 250); // Set the desired width and height
        }
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