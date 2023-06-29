package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SuperMethodReference;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

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
			IMethodBinding methodBinding= node.resolveMethodBinding();
			if (fTargetMethodBinding.isEqualTo(methodBinding)) {
				fstatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
			}
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		// Check if the method reference refers to the selected method
		if (!fstatus.hasFatalError()) {
			IMethodBinding methodReferenceBinding= node.resolveMethodBinding();
			ITypeBinding typeBindingOfMethodReference= methodReferenceBinding.getDeclaringClass();
			ITypeBinding typeBindingOfTargetMethod = fTargetMethodBinding.getDeclaringClass();
			if (fTargetMethodBinding.isEqualTo(methodReferenceBinding) && typeBindingOfTargetMethod.isEqualTo(typeBindingOfMethodReference)) {
					fstatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
			}
		}
		return super.visit(node);
	}
}
