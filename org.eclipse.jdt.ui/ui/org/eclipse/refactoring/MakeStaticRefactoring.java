package org.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.code.TargetProvider;


public class MakeStaticRefactoring extends Refactoring {

	private IMethod fMethod;

	private ICompilationUnit fCUnit;

	private CompositeChange fChange;

	private TargetProvider fTargetProvider;

	boolean fMultiFlag= false;

	protected MethodDeclaration methodDeclaration;

	public MakeStaticRefactoring(IMethod method, ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		fMethod= method;
		fCUnit= inputAsCompilationUnit;
	}

	@Override
	public String getName() {
		return "Make static"; //$NON-NLS-1$
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		findMethodDeclaration(fMethod);
		TextEdit methodInvocationEdit= null;
		fTargetProvider= TargetProvider.create(methodDeclaration);
		fTargetProvider.initialize();

		fChange= new CompositeChange("AllChanges"); //$NON-NLS-1$

		//Find and Modify MethodInvocations
		ReferencesInBinaryContext binaryRefs= new ReferencesInBinaryContext(""); //$NON-NLS-1$
		ICompilationUnit[] affectedCUs= fTargetProvider.getAffectedCompilationUnits(null, binaryRefs, new SubProgressMonitor(pm, 1));

		for (ICompilationUnit affectedCU : affectedCUs) {
			BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(affectedCU, null);
			MultiTextEdit fMultiTextEdit= new MultiTextEdit();
			for (BodyDeclaration body : bodies) {
				ASTNode[] invocations= fTargetProvider.getInvocations(body, null);
				for (ASTNode invocation : invocations) {
					AST ast= invocation.getAST();
					ASTRewrite rewrite= ASTRewrite.create(ast);
					MethodInvocation staticMethodInvocation= ast.newMethodInvocation();
					staticMethodInvocation.setName(ast.newSimpleName(methodDeclaration.getName().toString()));
					staticMethodInvocation.setExpression(ast.newSimpleName(((TypeDeclaration) methodDeclaration.getParent()).getName().toString()));
					rewrite.replace(invocation, staticMethodInvocation, null);
					methodInvocationEdit= rewrite.rewriteAST();
					fMultiTextEdit.addChild(methodInvocationEdit);
				}
			}
			CompilationUnitChange methodInvocationChange= new CompilationUnitChange("ChangeInMethodInvocation", affectedCU); //$NON-NLS-1$
			methodInvocationChange.setEdit(fMultiTextEdit);
			fChange.add(methodInvocationChange);
		}

		//Modify MethodDeclaration
		AST ast= methodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, methodDeclaration);
		modRewrite.setModifiers(methodDeclaration.getModifiers() | Modifier.STATIC, null);

		TextEdit methodDeclarationEdit= rewrite.rewriteAST();

		CompilationUnitChange methodDeclarationChange= new CompilationUnitChange("ChangeInMethodDeclaration", fCUnit); //$NON-NLS-1$
		methodDeclarationChange.setEdit(methodDeclarationEdit);
		fChange.add(methodDeclarationChange);

		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return fChange;
	}

	private void findMethodDeclaration(IMethod method) {
		ICompilationUnit compilationUnit= method.getCompilationUnit();

		// Create AST parser with Java 17 support
		ASTParser parser= ASTParser.newParser(AST.JLS20);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

		// Configure parser with classpath and project options
		parser.setEnvironment(null, null, null, true);
		parser.setUnitName(compilationUnit.getElementName());

		// Create CompilationUnit AST root
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		// Resolve bindings
		astRoot.recordModifications();
		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				IMethod resolvedMethod= (IMethod) node.resolveBinding().getJavaElement();
				if (resolvedMethod.equals(method)) {
					methodDeclaration= node;
					return false;
				}
				return super.visit(node);
			}
		});
	}
}
