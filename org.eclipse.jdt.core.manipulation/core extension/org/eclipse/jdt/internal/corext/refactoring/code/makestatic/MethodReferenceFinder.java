/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.SuperMethodReference;

final class MethodReferenceFinder extends ASTVisitor {
	private final RefactoringStatus fstatus;

	private final IMethodBinding fTargetMethodBinding;

	public MethodReferenceFinder(RefactoringStatus status, IMethodBinding targetMethodBinding) {
		fstatus= status;
		fTargetMethodBinding= targetMethodBinding;
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
		// Check if the method reference refers to the selected method
		if (!fstatus.hasFatalError()) {
			fstatus.merge(FinalConditionsChecker.checkMethodReferenceNotReferingToMethod(node, fTargetMethodBinding));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		// Check if the method reference refers to the selected method
		if (!fstatus.hasFatalError()) {
			fstatus.merge(FinalConditionsChecker.checkMethodReferenceNotReferingToMethod(node, fTargetMethodBinding));
		}
		return super.visit(node);
	}
}
