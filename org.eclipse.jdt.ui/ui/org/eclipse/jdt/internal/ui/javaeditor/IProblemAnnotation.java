package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Interface of annotations representing problems.
 */
public interface IProblemAnnotation {
	
	String getMessage();
	
	int getId();
	
	String[] getArguments();
	
	boolean isTemporaryProblem();
	
	boolean isWarning();
	
	boolean isError();
	
	boolean isProblem();
}
