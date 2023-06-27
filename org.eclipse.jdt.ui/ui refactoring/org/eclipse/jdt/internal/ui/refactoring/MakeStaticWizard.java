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

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.code.MakeStaticRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 *
 * @since 3.29
 *
 */
public class MakeStaticWizard extends RefactoringWizard {

	public MakeStaticWizard(MakeStaticRefactoring ref, String pagetitle){
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(pagetitle);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}



	@Override
	protected void addUserInputPages(){
		addPage(new MakeStaticInputPage(null));
	}
}
