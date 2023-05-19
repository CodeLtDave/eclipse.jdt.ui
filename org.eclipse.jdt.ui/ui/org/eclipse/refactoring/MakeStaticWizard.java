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

package org.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 *
 * @since 3.29
 *
 */
public class MakeStaticWizard extends RefactoringWizard {

	/* package */ static final String DIALOG_SETTING_SECTION= "MakeStaticWizard"; //$NON-NLS-1$

	public MakeStaticWizard(MakeStaticRefactoring ref){
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Make method static"); //$NON-NLS-1$
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	public Change createChange(){
		// creating the change is cheap. So we don't need to show progress.
		try {
			return getRefactoring().createChange(new NullProgressMonitor());
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	@Override
	protected void addUserInputPages(){
		addPage(new MakeStaticInputPage(null));
	}
}
