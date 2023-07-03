package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.Selection;

class InitialConditionsChecker {
	private ICompilationUnit fSelectionICompilationUnit;

	private CompilationUnit fSelectionCompilationUnit;

	private Selection fSelectionEditorText;

	private IMethod fSelectionIMethod;

	private boolean fSelectionIsEditorTextNotIMethod;

	public InitialConditionsChecker(Selection selectionEditorText) {
		fSelectionEditorText= selectionEditorText;
	}


}
