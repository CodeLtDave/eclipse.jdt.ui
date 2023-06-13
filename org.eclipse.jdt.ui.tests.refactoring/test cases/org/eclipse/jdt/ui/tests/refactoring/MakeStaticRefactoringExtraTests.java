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
	public void test22() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 17, 3, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testDeep() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 18, 7, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testMethodReference() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 10, 6, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
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
	public void testThisMethodReference() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 8, 10, 8, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
	}
}
