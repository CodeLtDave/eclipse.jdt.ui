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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.code.MakeStaticRefactoring;

import org.eclipse.jdt.internal.ui.refactoring.code.MakeStaticInputPage;


public class MakeStaticWizard extends RefactoringWizard {

	public MakeStaticWizard(MakeStaticRefactoring refactoring, int flags) {
		super(refactoring, flags);
		setDefaultPageTitle("My Example Refactoring"); //$NON-NLS-1$
		setWindowTitle("My Example Refactoring"); //$NON-NLS-1$
	}

	@Override
	protected void addUserInputPages() {
		addPage(new MakeStaticInputPage((MakeStaticRefactoring) getRefactoring()));
	}

}
