package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.refactoring.MakeStaticRefactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;


public class MakeStaticRefactoringTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "MakeStatic/";

	public MakeStaticRefactoringTests() {
		rts= new RefactoringTestSetup();
	}

	protected MakeStaticRefactoringTests(RefactoringTestSetup rts) {
		super(rts);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper(String[] topLevelName, int startLine, int startColumn, int endLine, int endColumn, boolean shouldWarn,
			boolean shouldError, boolean shouldFail) throws Exception, JavaModelException, CoreException, IOException {
		ICompilationUnit[] cu= new ICompilationUnit[topLevelName.length];
		for (int i= 0; i < topLevelName.length; i++) {
			String packName= topLevelName[i].substring(0, topLevelName[i].indexOf('.'));
			String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
			IPackageFragment cPackage= getRoot().createPackageFragment(packName, true, null);
			cu[i]= createCUfromTestFile(cPackage, className);
		}

		IType type = cu[0].getTypes()[0];
	    IMethod method = type.getMethod("foo", new String[] {});

		ISourceRange selection= TextRangeUtil.getSelection(cu[0], startLine, startColumn, endLine, endColumn);
		cu[0].getSource();
		try {
			MakeStaticRefactoring ref= new MakeStaticRefactoring(method, cu[0], selection.getOffset(), selection.getLength());

			boolean failed= false;
			RefactoringStatus status= performRefactoringWithStatus(ref);
			if (status.hasFatalError()) {
				assertTrue("Failed but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.FATAL), shouldFail); //$NON-NLS-1$
				failed= true;
			} else
				assertFalse("Didn't fail although expected", shouldFail); //$NON-NLS-1$

			if (!failed) {

				if (status.hasError())
					assertTrue("Had errors but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.ERROR), shouldError); //$NON-NLS-1$
				else
					assertFalse("No error although expected", shouldError); //$NON-NLS-1$

				if (status.hasWarning())
					assertTrue("Had warnings but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.WARNING), shouldWarn); //$NON-NLS-1$
				else
					assertFalse("No warning although expected", shouldWarn); //$NON-NLS-1$

				for (int i= 0; i < topLevelName.length; i++) {
					String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
					assertEqualLines("invalid output.", getFileContents(getOutputTestFileName(className)), cu[i].getSource()); //$NON-NLS-1$
				}
			}
		} finally {
			for (int i= 0; i < topLevelName.length; i++)
				JavaProjectHelper.delete(cu[i]);
		}
	}

	protected void helperPass(String[] topLevelName, String newName, String target, int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		helper(topLevelName, startLine, startColumn, endLine, endColumn, false, false, false);
	}


	@Test
	public void test01() throws Exception {
		// very simple test
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 7, 10, 7, 13);
	}



}
