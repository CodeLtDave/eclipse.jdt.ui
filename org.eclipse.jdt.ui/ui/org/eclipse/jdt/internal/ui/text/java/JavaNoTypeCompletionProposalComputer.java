/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jdt.core.CompletionProposal;

import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * 
 * @since 3.2
 */
public class JavaNoTypeCompletionProposalComputer extends JavaCompletionProposalComputer {
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer#createCollector(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector= super.createCollector(context);
		collector.setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, false);
		collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, false);
		collector.setIgnored(CompletionProposal.FIELD_REF, false);
		collector.setIgnored(CompletionProposal.KEYWORD, false);
		collector.setIgnored(CompletionProposal.LABEL_REF, false);
		collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
		collector.setIgnored(CompletionProposal.METHOD_DECLARATION, false);
		collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, false);
		collector.setIgnored(CompletionProposal.METHOD_REF, false);
		collector.setIgnored(CompletionProposal.PACKAGE_REF, false);
		collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, false);
		collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, false);
		
		collector.setIgnored(CompletionProposal.JAVADOC_BLOCK_TAG, true);
		collector.setIgnored(CompletionProposal.JAVADOC_FIELD_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_INLINE_TAG, true);
		collector.setIgnored(CompletionProposal.JAVADOC_METHOD_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_PARAM_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_TYPE_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_VALUE_REF, true);
		
		collector.setIgnored(CompletionProposal.TYPE_REF, true);
		return collector;
	}
}
