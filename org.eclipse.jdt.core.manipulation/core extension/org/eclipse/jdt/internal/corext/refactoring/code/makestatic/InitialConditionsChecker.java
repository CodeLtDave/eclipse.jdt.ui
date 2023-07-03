package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

class InitialConditionsChecker {
	private ICompilationUnit fSelectionICompilationUnit;

	private CompilationUnit fSelectionCompilationUnit;

	private Selection fSelectionEditorText;

	private IMethod fSelectionIMethod;

	private boolean fSelectionIsEditorTextNotIMethod;

	public InitialConditionsChecker() {
	}

	public RefactoringStatus checkTextSelectionStart(Selection selection) {
		RefactoringStatus status= new RefactoringStatus();
		if (selection.getOffset() < 0 || selection.getLength() < 0) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return status;
	}

	public RefactoringStatus checkValidICompilationUnit(ICompilationUnit iCompilationUnit) {
		RefactoringStatus status= new RefactoringStatus();
		if (iCompilationUnit == null) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return status;
	}

	public RefactoringStatus checkNodeIsValidMethod(ASTNode selectedNode) {
		RefactoringStatus status= new RefactoringStatus();
		if (selectedNode == null) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		} else if (selectedNode instanceof SuperMethodInvocation) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations));
		}
		while (selectedNode != null) {
			if (!(selectedNode instanceof MethodDeclaration || selectedNode instanceof MethodInvocation)) {
				selectedNode= selectedNode.getParent();
				break;
			}
		}
		if (!(selectedNode instanceof MethodDeclaration || selectedNode instanceof MethodInvocation)) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
		}
		return status;
	}

	public RefactoringStatus checkIMethodIsValid(IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		if (iMethod == null) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
		}

		try {
			if (iMethod.getDeclaringType().isAnnotation()) {
				status.addFatalError(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_annotation);
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	public RefactoringStatus check2(ICompilationUnit iCompilationUnit) {
		RefactoringStatus status= new RefactoringStatus();

		return status;
	}



	public RefactoringStatus check(ICompilationUnit iCompilationUnit) {
		RefactoringStatus status= new RefactoringStatus();

		return status;
	}


}
