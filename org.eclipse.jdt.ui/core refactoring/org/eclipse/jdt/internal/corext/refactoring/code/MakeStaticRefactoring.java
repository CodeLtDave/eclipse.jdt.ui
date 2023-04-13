/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Allow inlining of local variable initialized to null. - https://bugs.eclipse.org/93850
 *     Microsoft Corporation - read preferences from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

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

public class MakeStaticRefactoring extends Refactoring {

	private IFile fFile;
	private String fOldText;
	private String fNewText;

	private Change fChange;

	public MakeStaticRefactoring(IFile file) {
		fFile= file;
		fChange= null;
		fOldText= null;
		fNewText= null;
	}

	public MakeStaticRefactoring(ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Make replaces"; //$NON-NLS-1$
	}

	public void setNewText(String text) {
		fNewText= text;
	}

	public void setOldText(String text) {
		fOldText= text;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (fFile == null || !fFile.exists()) {
			return RefactoringStatus.createFatalErrorStatus("File does not exist"); //$NON-NLS-1$
		}
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (fOldText == null || fOldText.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Old text must be set and not empty"); //$NON-NLS-1$
		}
		if (fNewText == null || fNewText.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("New text must be set and not empty");//$NON-NLS-1$
		}

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
					change.addTextEditGroup(new TextEditGroup("Change " + count++, replaceEdit));//$NON-NLS-1$
					i= offset + fOldText.length();
				} else {
					break;
				}
			}
			if (count == 1) {
				fChange= new NullChange(getName());
				return RefactoringStatus.createErrorStatus("No matches found for '" + fOldText +"'");//$NON-NLS-1$ //$NON-NLS-2$
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
