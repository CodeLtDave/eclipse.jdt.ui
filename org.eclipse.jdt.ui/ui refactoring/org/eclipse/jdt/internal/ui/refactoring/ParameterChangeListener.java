/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;

public interface ParameterChangeListener {

	/**
	 * Gets fired when the given parameter has changed	 * @param parameter the paramter that has changed.	 */
	public void paramterChanged(ParameterInfo parameter);

	/**
	 * Gets fired if the paramters got reordered.	 */
	public void parameterReordered();
}
