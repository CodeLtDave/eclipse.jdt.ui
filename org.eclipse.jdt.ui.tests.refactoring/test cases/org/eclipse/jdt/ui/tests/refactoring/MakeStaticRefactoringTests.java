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

		if (status.hasFatalError() || status.hasError()) {
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
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 10, 2, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testObjectParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of String type
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 10, 3, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testPrimitiveParameterAndReturnType() throws Exception {
		//Refactor method with parameter and return type of Integer type
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 16, 3, 19);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testArrayParameterAndReturnType() throws Exception {
		//Refactor method with String-Array as Parameter and return type
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 21, 3, 24);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodNotFound() throws Exception {
		//Method cannot be found
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 0, 2, 1);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection));
	}

	@Test
	public void testIsConstructor() throws Exception {
		//Check if Constructor
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 12, 2, 15);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors));
	}

	@Test
	public void testThisInDeclaration() throws Exception {
		//MethodDeclaration uses "this"-Keyword for instance variables
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 19, 5, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testThisInDeclarationMultipleFiles() throws Exception {
		//MethodDeclaration uses "this"-Keyword for instance variables && MethodInvocations are in different packages within the same project
		RefactoringStatus status= helper(new String[] { "p.Foo", "p.Foo2" }, 7, 19, 7, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testThisInDeclarationInnerClass() throws Exception {
		//MethodDeclaration uses "this"-Keyword for instance variables && InnerClass is referenced with "this"
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 8, 17, 8, 20);
		assertHasNoCommonErrors(status);
	}


	@Test
	public void testMultipleFilesInSameProject() throws Exception {
		//MethodInvocations are in different packages within the same project
		RefactoringStatus status= helper(new String[] { "p1.Foo", "p2.Foo2" }, 5, 19, 5, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testRecursive() throws Exception {
		//MethodInvocation in MethodDeclaration with object of the same Class in parameter
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 10, 3, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testRecursive2() throws Exception {
		//recursive invocation after invoking a method that returns a new instance of the same class
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 10, 6, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testRecursive3() throws Exception {
		//simple recursive invocation of instance method
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testInheritance() throws Exception {
		//Refactor of method that overrides method of supertype (Selection is set to MethodDeclaration)
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 4, 19, 4, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_explicit_super_method_invocation));
	}

	@Test
	public void testInheritance2() throws Exception {
		//Refactor of method in super type that has child type overriding the method -> should fail
		RefactoringStatus status= helper(new String[] { "p.SuperClass", "p.SubClass" }, 3, 19, 3, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_method_is_overridden_in_subtype));
	}

	@Test
	public void testInheritance3() throws Exception {
		//Selecting SuperMethodInvocation -> should fail
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 5, 26, 5, 29);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations));
	}

	@Test
	public void testInheritance4() throws Exception {
		//Refactor method without parameters on the lowest hierarchy level ->
		//After refactoring it is static but has the same signature as parent type method -> should fail
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 4, 19, 4, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
	}

	@Test
	public void testInheritance5() throws Exception {
		//Inheritance with Recursion
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 4, 10, 4, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage().equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testDuplicateParamName() throws Exception {
		//Method has instance usage and already parameter with name "example"
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 19, 5, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testDuplicateMethod() throws Exception {
		//Selected method has instance usage and there is an existing method that is equal to the selected method after being refactored
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 19, 5, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_duplicate_method_signature));
	}

	@Test
	public void testDuplicateMethod2() throws Exception {
		//like testDuplicateMethod but parameter positions are switches. No error should be thrown
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 19, 5, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodAlreadyStatic() throws Exception {
		//Selected method is already static
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 24, 2, 27);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static));
	}

	@Test
	public void testNonStaticInnerClass() throws Exception {
		//instance of Inner Class is used in refactored method
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 17, 5, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration() throws Exception {
		//class has one generic type that is used for field and for parameter of selected method
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 17, 4, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration2() throws Exception {
		//class has more than one generic type
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 17, 6, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration3() throws Exception {
		//class has more than one generic type and an instance of the class is used in selected method
		//selected method is hiding two generic types -> refactoring should fail
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 24, 6, 27);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration4() throws Exception {
		//duplicate generic paramType in methodDeclaration
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration5() throws Exception {
		//three generic types in methodDeclaration
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}


	@Test
	public void testGenericDeclaration6() throws Exception {
		//generic array as param
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration7() throws Exception {
		//class type param number is higher than method param number
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 17, 3, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration8() throws Exception {
		//different bounds on typeParameters
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 17, 7, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testGenericDeclaration9() throws Exception {
		//check for wildcardTypes as bounds (T extends List<?>)
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 17, 7, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_wildCardTypes_as_bound));
	}

	@Test
	public void testGenericDeclaration10() throws Exception {
		//check for wildcardTypes as bounds (T extends Map<? extends Runnable, ? extends Throwable>)
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 17, 7, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_wildCardTypes_as_bound));
	}

	@Test
	public void testNoAdditionalParameter() throws Exception {
		//An instance of the class is already in use as parameter with name example in the selected method for field access.
		//Only static keyword needs to be set and not additional Parameter for field access.
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 19, 5, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testNoAdditionalParameter2() throws Exception {
		//An instance of the class is already in use as parameter with name foo in the selected method for field access.
		//Only static keyword needs to be set and not additional Parameter for field access.
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 19, 5, 22);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testOuterFieldAccessInAnonymousClass() throws Exception {
		//Anonymous class uses a field of outer class
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testOuterFieldAccessInLambda() throws Exception {
		//Lambda uses a field of outer class
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testInnerFieldAccessInAnonymousClass() throws Exception {
		//Method of anonymous class invokes another method of anonymous class -> Refactoring should ignore this invocation
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 10, 5, 13);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testVariousInstanceCases() throws Exception {
		//Various cases of instance access in many different forms
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 14, 17, 14, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_selected_method_uses_super_field_access));
	}

	@Test
	public void testInheritanceWithoutOverride() throws Exception {
		//subtype implements a method that is not overriding parent type method -> Refactoring should work
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testSelectionOfInvocationInDifferentClass() throws Exception {
		//Selection of MethodInvocation that is in different class than MethodDeclaration
		RefactoringStatus status= helper(new String[] { "p.Foo2", "p.Foo" }, 6, 13, 6, 19);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testVarArgs() throws Exception {
		//MethodDeclaration uses varargs
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 17, 5, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testPassingInstanceReference() throws Exception {
		//Passing instance reference to another method
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 17, 5, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testSuperMethodReference() throws Exception {
		//Selected method is used in SuperMethodReference -> Should throw error
		RefactoringStatus status= helper(new String[] { "p.SuperClass", "p.SubClass" }, 4, 19, 4, 22);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
	}

	@Test
	public void testReturnThis() throws Exception {
		//Only this keyword in return statement
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 16, 2, 19);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testReturnField() throws Exception {
		//Selected method return field
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 16, 5, 19);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testExplicitSuperMethodInvocation() throws Exception {
		//MethodDeclaration uses explcit SuperMethodInvocation to call method of parent type -> semantic change not allowed
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 3, 17, 3, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_explicit_super_method_invocation));
	}

	@Test
	public void testImplicitSuperMethodInvocation() throws Exception {
		//MethodDeclaration uses implicit SuperMethodInvocation to call method of parent type -> no semantic change
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 3, 17, 3, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testSuperFieldAccess() throws Exception {
		//MethodDeclaration uses SuperFieldAccess -> throws warning but is possible
		RefactoringStatus status= helper(new String[] { "p.SubClass", "p.SuperClass" }, 6, 17, 6, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_selected_method_uses_super_field_access));
	}

	@Test
	public void testConcatenatedFieldAccessAndQualifiedNames() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 6, 17, 6, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testSourceNotAvailable() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 3, 20, 3, 27);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_source_not_available_for_selected_method));
	}

	@Test
	public void testInstanceFieldAccessInOtherClass() throws Exception {
		//Access to instance method with field as member in another class
		RefactoringStatus status= helper(new String[] { "p.Foo", "p.Foo2" }, 6, 12, 6, 15);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testClassInstanceCreation() throws Exception {
		//new keyword needs to be called on instance after refactoring
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testConvertMethodReferenceToLambda() throws Exception {
		//MethodReference needs to be co0nverted to lambda because refactored method accepts two parameters
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 10, 10, 10, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
	}

	@Test
	public void testNested() throws Exception {
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 7, 17, 7, 28);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testMethodReference() throws Exception {
		//TypeMethodReference
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 8, 10, 8, 13);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
	}

	@Test
	public void testMethodReference2() throws Exception {
		//ExpressionMethodReference in anonymous class -> Refactoring not allowed in anonymous class and method references also not allowed
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 4, 26, 4, 29);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types));
	}

	@Test
	public void testMethodReference3() throws Exception {
		//ExpressionMethodReference with recursion
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_recursive_methods));
	}

	@Test
	public void testMethodReference4() throws Exception {
		//ExpressionMethodReference
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 8, 17, 8, 20);
		assertTrue(status.getEntryWithHighestSeverity().getMessage()
				.equals(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
	}

	@Test
	public void testAlignment() throws Exception {
		//Alignment of parameters should not be changed
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 2, 17, 2, 20);
		assertHasNoCommonErrors(status);
	}

	@Test
	public void testInstanceAccessInInnerClass() throws Exception {
		//Instance method is called in Inner class and instance parameter is added to selected method
		RefactoringStatus status= helper(new String[] { "p.Foo" }, 5, 17, 5, 20);
		assertHasNoCommonErrors(status);
	}
}
