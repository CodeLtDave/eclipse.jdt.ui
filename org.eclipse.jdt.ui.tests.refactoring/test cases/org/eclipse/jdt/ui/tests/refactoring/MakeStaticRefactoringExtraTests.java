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

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;


public class MakeStaticRefactoringExtraTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "MakeStaticExtra/";

	public MakeStaticRefactoringExtraTests() {
		rts= new RefactoringTestSetup();
	}

	protected MakeStaticRefactoringExtraTests(RefactoringTestSetup rts) {
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
	public void test1() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 23);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test2() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 16, 4, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test3() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 16, 4, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test4() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 16, 4, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test5() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 16, 4, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test6() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 16, 4, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test7() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 16, 7, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test8() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 9, 4, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test9() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 9, 4, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test10() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 9, 4, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test11() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 9, 4, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_method_is_overridden_in_subtype));
	}

	@Test
	public void test12() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 9, 2, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_selected_method_overrides_parent_type));
	}

	@Test
	public void test13() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 9, 2, 12);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test14() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 9, 2, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test15() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 10, 5, 25);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void test16() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 8, 10, 8, 16);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test17() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 12, 6, 18);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test18() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 10, 2, 16);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test19() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 10, 4, 16);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test20() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 17, 7, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_selected_method_overrides_parent_type));
	}

	@Test
	public void test21() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void test22() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 17, 3, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void test23() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 14, 2, 17);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testClearOverrideAnnotation() throws Exception {
		//IntelliJ fails
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 17, 7, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
	}

	@Test
	public void testDeep() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 18, 7, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testExpandMethodReference() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 10, 7, 10, 11);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodReference() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 10, 6, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodReferenceInAnonymousClass() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 20, 4, 25);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types));
	}

	@Test
	public void testMethodReferenceInTheSameMethod() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 17, 5, 21);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testOverrides() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 12, 18, 12, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testPreserveParametersAlignment() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testPreserveTypeParams() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 25, 4, 41);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testReceiverParameter() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testThisMethodReference() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 10, 7, 13);
		assertHasNoCommonErrors(status);
	}
}
