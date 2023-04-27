package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;

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

	private RefactoringStatus helper(String[] topLevelName, String methodName, String[] parameters, int startLine, int startColumn, int endLine, int endColumn)
			throws Exception, JavaModelException, CoreException, IOException {
		ICompilationUnit[] cu= new ICompilationUnit[topLevelName.length];
		for (int i= 0; i < topLevelName.length; i++) {
			String packName= topLevelName[i].substring(0, topLevelName[i].indexOf('.'));
			String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
			IPackageFragment cPackage= getRoot().createPackageFragment(packName, true, null);
			cu[i]= createCUfromTestFile(cPackage, className);
		}

		IType type= cu[0].getTypes()[0];
		IMethod method= type.getMethod(methodName, parameters);

		ISourceRange selection= TextRangeUtil.getSelection(cu[0], startLine, startColumn, endLine, endColumn);

		try {
			MakeStaticRefactoring ref= new MakeStaticRefactoring(method, cu[0], selection.getOffset(), selection.getLength());
			RefactoringStatus status= performRefactoringWithStatus(ref);

			for (int i= 0; i < topLevelName.length; i++) {
				String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
				assertEqualLines("invalid output.", getFileContents(getOutputTestFileName(className)), cu[i].getSource()); //$NON-NLS-1$
			}

			return status;
		} finally {
			for (int i= 0; i < topLevelName.length; i++)
				JavaProjectHelper.delete(cu[i]);
		}
	}

	public void assertHasNoCommonErrors(RefactoringStatus status) {
		assertFalse("Failed but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.FATAL), status.hasFatalError());
		assertFalse("Had errors but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.ERROR), status.hasError());
		assertFalse("Had warnings but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.WARNING), status.hasWarning());
	}



	@Test
	public void test01() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, "foo", new String[] {}, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test02() throws Exception {
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "QString;" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test03() throws Exception {
		RefactoringStatus status = helper(new String[] { "p.Foo" }, "foo", new String[] {}, 7, 10, 7, 13);


	}

	@Test
	public void test04() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, "foo", new String[] {}, 7, 10, 7, 13);
	}



}
