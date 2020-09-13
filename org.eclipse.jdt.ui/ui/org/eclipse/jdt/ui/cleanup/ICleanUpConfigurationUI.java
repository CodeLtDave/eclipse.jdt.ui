/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.cleanup;

import org.eclipse.swt.widgets.Composite;


/**
 * Provides the UI to configure a clean up.
 *
 * @since 3.5
 */
public interface ICleanUpConfigurationUI {

	/**
	 * The options to modify in this section.
	 * <p>
	 * <strong>Note:</strong> If an option gets changed in the UI then this must immediately update
	 * the corresponding option in the here given clean up options.
	 * </p>
	 *
	 * @param options the options to modify
	 */
	void setOptions(CleanUpOptions options);

	/**
	 * Creates the contents for this clean up configuration UI.
	 *
	 * @param parent the parent composite
	 * @return created content control
	 */
	Composite createContents(Composite parent);

	/**
	 * Returns the number of clean ups that can be configured.
	 *
	 * @return the number of clean ups that can be configured
	 */
	int getCleanUpCount();

	/**
	 * Returns the number of selected clean ups.
	 *
	 * @return the number of selected clean ups at the moment
	 */
	int getSelectedCleanUpCount();

	/**
	 * A code snippet which complies to the current settings.
	 *
	 * @return a code snippet
	 */
	String getPreview();
}