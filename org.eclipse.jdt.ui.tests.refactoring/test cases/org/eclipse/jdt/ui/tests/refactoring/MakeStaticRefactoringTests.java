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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

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

	private RefactoringStatus helper(String[] topLevelName, int startLine, int startColumn, int endLine, int endColumn)
			throws Exception, JavaModelException, CoreException, IOException {

		ICompilationUnit[] cu= new ICompilationUnit[topLevelName.length];
		for (int i= 0; i < topLevelName.length; i++) {
			String packName= topLevelName[i].substring(0, topLevelName[i].indexOf('.'));
			String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
			IPackageFragment cPackage= getRoot().createPackageFragment(packName, true, null);
			cu[i]= createCUfromTestFile(cPackage, className);
		}

		ISourceRange selection= TextRangeUtil.getSelection(cu[0], startLine, startColumn, endLine, endColumn);

		MakeStaticRefactoring ref= new MakeStaticRefactoring(cu[0], selection.getOffset(), selection.getLength());
		RefactoringStatus status= performRefactoringWithStatus(ref);

		if (status.hasEntries()) {
			return status;
		} else {
			matchFiles(topLevelName, cu);
			matchASTs(topLevelName, cu);
		}

		for (int i= 0; i < topLevelName.length; i++)
			JavaProjectHelper.delete(cu[i]);

		return status;
	}

	private void matchFiles(String[] topLevelName, ICompilationUnit[] cu) throws IOException, JavaModelException {
		for (int i= 0; i < topLevelName.length; i++) {
			String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
			assertEqualLines("invalid output.", getFileContents(getOutputTestFileName(className)), cu[i].getSource()); //$NON-NLS-1$
		}
	}

	private void matchASTs(String[] topLevelName, ICompilationUnit[] cu) throws IOException {
		for (int i= 0; i < topLevelName.length; i++) {

			String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
			String content= getFileContents(getOutputTestFileName(className));

			ASTParser parser= ASTParser.newParser(AST.JLS20);
			parser.setSource(content.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setResolveBindings(true);
			parser.setBindingsRecovery(true);

			CompilationUnit outputCU= (CompilationUnit) parser.createAST(null);
			parser.setSource(cu[i]);
			CompilationUnit refactoredCU= (CompilationUnit) parser.createAST(null);

			assertTrue(outputCU.subtreeMatch(new ASTMatcher(), refactoredCU));
		}
	}

	public void assertHasNoCommonErrors(RefactoringStatus status) {
		assertFalse("Failed but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.FATAL), status.hasFatalError());
		assertFalse("Had errors but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.ERROR), status.hasError());
		assertFalse("Had warnings but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.WARNING), status.hasWarning());
	}

	@Test
	public void testSimpleFile() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testStringParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of String type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testIntegerParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Integer type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testArrayParameterAndReturnType() throws Exception {
		//Refactor method with String-Array as Parameter and return type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testBooleanParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Boolean type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testLongParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Long type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testFloatParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Float type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testDoubleParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Double type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testCharParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Char type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testShortParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Char type
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodNotFound() throws Exception {
		//Method cannot be found -> NullPointerException
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 0, 5, 5);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
	}

	@Test
	public void testIsConstructor() throws Exception {
		//Check if Constructor
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 8, 5, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors));
	}

	@Test
	public void testThisInDeclaration() throws Exception {
		//MethodDeclaration uses "this"-Keyword for instance variables
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 7, 10, 7, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testThisInDeclarationMultipleFiles() throws Exception {
		//MethodDeclaration uses "this"-Keyword for instance variables && MethodInvocations are in different packages within the same project
		RefactoringStatus status= helper(new String[] { "package1.Example", "package1.Example2" }, 7, 10, 7, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testThisInDeclarationInnerClass() throws Exception {
		//MethodDeclaration uses "this"-Keyword for instance variables && InnerClass is referenced with "this"
		RefactoringStatus status= helper(new String[] { "p.Input" }, 8, 10, 8, 20);
		assertHasNoCommonErrors(status);
	}


	@Test
	public void testMultipleFilesInSameProject() throws Exception {
		//MethodInvocations are in different packages within the same project
		RefactoringStatus status= helper(new String[] { "package1.Example", "package2.Example2" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testRecursive() throws Exception {
		//MethodInvocation in MethodDeclaration with object of the same Class in parameter
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testSuperMethodInvocation() throws Exception {
		//Refactor of method that overrides method of supertype (Selection is set to MethodDeclaration)
		RefactoringStatus status= helper(new String[] { "package1.SubClass", "package1.SuperClass" }, 6, 14, 6, 24);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testSuperMethodInvocation2() throws Exception {
		//Refactor of method in super type that has child type with override of the method -> should fail
		RefactoringStatus status= helper(new String[] { "package1.SuperClass", "package1.SubClass", }, 5, 14, 5, 24);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations));
	}

	@Test
	public void testDuplicateParamName() throws Exception {
		//Method has instance usage and already parameter with name "example"
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 7, 10, 7, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(Messages.format(RefactoringCoreMessages.MakeStaticRefactoring_parameter_name_already_used, BasicElementLabels.getJavaElementName("example"))));
	}

	@Test
	public void testDuplicateMethod() throws Exception {
		//Selected method has instance usage and there is an existing method that is equal to the selected method after being refactored
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 7, 10, 7, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_duplicate_method_signature));
	}

	@Test
	public void testMethodAlreadyStatic() throws Exception {
		//Selected method is already static
		RefactoringStatus status= helper(new String[] { "package1.Example" }, 5, 22, 5, 32);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static));
	}
}
