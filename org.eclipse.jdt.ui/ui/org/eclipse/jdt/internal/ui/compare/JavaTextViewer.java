/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.compare.IStreamContentAccessor;


public class JavaTextViewer extends Viewer {
		
	private SourceViewer fSourceViewer;
	private Object fInput;
	
	
	JavaTextViewer(Composite parent) {
		fSourceViewer= new SourceViewer(parent, null, SWT.H_SCROLL + SWT.V_SCROLL);
		JavaTextTools tools= JavaCompareUtilities.getJavaTextTools();
		if (tools != null)
			fSourceViewer.configure(new JavaSourceViewerConfiguration(tools, null));

		fSourceViewer.setEditable(false);
	}
		
	public Control getControl() {
		return fSourceViewer.getControl();
	}
	
	public void setInput(Object input) {
		
		if (input instanceof IStreamContentAccessor) {
			Document document= new Document(getString(input));
			JavaCompareUtilities.setupDocument(document);
			fSourceViewer.setDocument(document);
		}
		fInput= input;
	}
	
	public Object getInput() {
		return fInput;
	}
	
	public ISelection getSelection() {
		return null;
	}
	
	public void setSelection(ISelection s, boolean reveal) {
	}
	
	public void refresh() {
	}
	
	/**
	 * A helper method to retrieve the contents of the given object
	 * if it implements the IStreamContentAccessor interface.
	 */
	private static String getString(Object input) {
		
		if (input instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) input;
			try {
				return JavaCompareUtilities.readString(sca.getContents());
			} catch (CoreException ex) {
				JavaPlugin.log(ex);
			}
		}
		return ""; //$NON-NLS-1$
	}
}
