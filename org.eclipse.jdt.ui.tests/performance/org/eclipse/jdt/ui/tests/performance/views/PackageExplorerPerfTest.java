/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.performance.views;

import java.io.File;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public class PackageExplorerPerfTest extends JdtPerformanceTestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";
		
		public static IJavaProject fJProject1;
		public static IPackageFragmentRoot fJunitSrcRoot;
		
		public MyTestSetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("Testing", "bin");
			assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			fJunitSrcRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}
		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite("PackageExplorerPerfTest");
		suite.addTest(new PackageExplorerPerfTest("testOpen"));
		suite.addTest(new PackageExplorerPerfTest("testSelect"));
		suite.addTest(new PackageExplorerPerfTest("testExpand"));
		return new MyTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}
	
	public PackageExplorerPerfTest(String name) {
		super(name);
	}
	
	public void testOpen() throws Exception {
		tagAsGlobalSummary("Open Package Explorer", Dimension.CPU_TIME);
		joinBackgroudActivities();
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		startMeasuring();
		page.showView(JavaUI.ID_PACKAGES);
		finishMeasurements();
	}
	
	public void testSelect() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		StructuredSelection selection= new StructuredSelection(MyTestSetup.fJProject1);
		startMeasuring();
		viewer.setSelection(selection);
		finishMeasurements();
	}
	
	public void testExpand() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		startMeasuring();
		viewer.expandToLevel(MyTestSetup.fJProject1, 1);
		finishMeasurements();
	}
	
	private TreeViewer getViewer() {
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return ((PackageExplorerPart)page.findView(JavaUI.ID_PACKAGES)).getTreeViewer();
	}
}
