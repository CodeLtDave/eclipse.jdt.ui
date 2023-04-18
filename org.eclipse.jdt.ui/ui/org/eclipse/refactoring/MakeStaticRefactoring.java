package org.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;


public class MakeStaticRefactoring extends Refactoring {

	private IMethod fMethod;

	private CompilationUnitChange fChange;

	private CompilationUnitRewrite fBaseCuRewrite;

	private ICompilationUnit fCUnit;

	private Object fBodyUpdater;

	private CompilationUnitRewrite cuRewrite;

	private TextChangeManager fChangeManager;

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
		ICompilationUnit compilationUnit= fMethod.getCompilationUnit();

		MethodDeclaration methodDeclaration = findMethodDeclaration(fMethod);

		fBaseCuRewrite= new CompilationUnitRewrite(compilationUnit);

		AST ast = methodDeclaration.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, methodDeclaration);
		modRewrite.setModifiers(Modifier.STATIC, null);

		TextEdit textEdit= rewrite.rewriteAST();

		fChange = new CompilationUnitChange("Test",compilationUnit); //$NON-NLS-1$
	    fChange.setEdit(textEdit);

		//compilationUnit.applyTextEdit(textEdit, null);

		/*if (fBodyUpdater != null)
		fBodyUpdater.updateBody(fMethDecl, fCuRewrite, fResult);*/


		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return fChange;
	}

	private MethodDeclaration findMethodDeclaration(IMethod method) {
		ICompilationUnit compilationUnit= method.getCompilationUnit();

		// Create AST parser with Java 17 support
		ASTParser parser= ASTParser.newParser(AST.JLS17);
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
					// Found the MethodDeclaration for the given IMethod
					return true;
				}
				return super.visit(node);
			}
		});

		// Get the resolved MethodDeclaration
		MethodDeclaration methodDeclaration= (MethodDeclaration) astRoot.findDeclaringNode(method.getKey());

		return methodDeclaration;
	}

}
