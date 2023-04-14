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
package org.eclipse.ltk.ui.refactoring.examples;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;

public class ExampleRefactoring extends Refactoring {

	private IFile fFile;
	private String fOldText;
	private String fNewText;

	private Change fChange;
	private CompilationUnitRewrite fBaseCuRewrite;
	private IMethod fMethod;

	public ExampleRefactoring(IMethod method) {
		fMethod= method;
		fChange= null;
		fOldText= null;
		fNewText= null;
	}


	@Override
	public String getName() {
		return "Make replaces";
	}

	public void setNewText(String text) {
		fNewText= text;
	}

	public void setOldText(String text) {
		fOldText= text;
	}

	private ICompilationUnit getCu() {
		return fMethod.getCompilationUnit();
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (fFile == null || !fFile.exists()) {
			return RefactoringStatus.createFatalErrorStatus("File does not exist");
		}

		if (fBaseCuRewrite == null || !fBaseCuRewrite.getCu().equals(getCu())) {
			fBaseCuRewrite= new CompilationUnitRewrite(getCu());
			fBaseCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
		}
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException
	{
		ICompilationUnit compilationUnit = fMethod.getCompilationUnit();
		ASTParser parser = ASTParser.newParser(AST.JLS17);
		parser.setSource(compilationUnit);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
		MethodDeclaration fMethDecl = (MethodDeclaration) astRoot.findDeclaringNode(fMethod.getKey());

		ModifierRewrite modRewrite= ModifierRewrite.create(fBaseCuRewrite.getASTRewrite(), fMethDecl);
		modRewrite.setModifiers(Modifier.STATIC, null);

		/*if (fBodyUpdater != null)
			fBodyUpdater.updateBody(fMethDecl, fCuRewrite, fResult);

*/
		TextFileChange change= new TextFileChange(getName(), fFile);
		change.setEdit(new MultiTextEdit());

		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		manager.connect(fFile.getFullPath(), LocationKind.IFILE, null);
		try {
			ITextFileBuffer textFileBuffer= manager.getTextFileBuffer(fFile.getFullPath(), LocationKind.IFILE);
			String content= textFileBuffer.getDocument().get();

			int i= 0;
			int count= 1;
			while (i < content.length()) {
				int offset= content.indexOf(fOldText, i);
				if (offset != -1) {
					ReplaceEdit replaceEdit= new ReplaceEdit(offset, fOldText.length(), fNewText);
					change.addEdit(replaceEdit);
					change.addTextEditGroup(new TextEditGroup("Change " + count++, replaceEdit));
					i= offset + fOldText.length();
				} else {
					break;
				}
			}
			if (count == 1) {
				fChange= new NullChange(getName());
				return RefactoringStatus.createErrorStatus("No matches found for '" + fOldText +"'");
			}
			fChange= change;

		} finally {
			manager.disconnect(fFile.getFullPath(), LocationKind.IFILE, null);
		}
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return fChange;
	}
}
