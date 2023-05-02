package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

	@Rule
	public ExpectedException exceptionRule= ExpectedException.none();

	@Test
	public void testSimpleFile() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, "foo", new String[] {}, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testStringParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of String type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "QString;" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testIntegerParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Integer type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "I" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testArrayParameterAndReturnType() throws Exception {
		//Refactor method with String-Array as Parameter and return type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "[QString;" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testBooleanParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Boolean type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "Z" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testLongParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Long type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "J" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testFloatParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Float type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "F" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testDoubleParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Double type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "D" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testCharParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Char type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "C" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testShortParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Char type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "greet", new String[] { "S" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodNotFound() throws Exception {
		//Method cannot be found -> NullPointerException
		exceptionRule.expect(NullPointerException.class);
		helper(new String[] { "p.Foo" }, "not included", new String[] {}, 7, 10, 7, 13);
		//assertThrows(NullPointerException.class, () -> helper(new String[] { "p.Foo" }, "notIncluded", new String[] {}, 7, 10, 7, 13));
	}

	@Test
	public void testIsConstructor() throws Exception {
		//Check if Constructor
		RefactoringStatus status= helper(new String[] { "package1.Example" }, "Example", new String[] {}, 5, 8, 5, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage() == "Constructor cannot be refactored to static.");
	}
}
