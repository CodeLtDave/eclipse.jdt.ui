package org.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;

public class MakeStaticRefactoring extends Refactoring {

	public MakeStaticRefactoring(ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Make static"; //$NON-NLS-1$
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

}
