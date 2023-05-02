package org.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;

/**
 *
 * @since 3.29
 *
 */
public class MakeStaticRefactoring extends Refactoring {

	private IMethod fMethod;

	private ICompilationUnit fCUnit;

	private TextEditBasedChangeManager fChangeManager;

	private TargetProvider fTargetProvider;

	protected MethodDeclaration fMethodDeclaration;

	public MakeStaticRefactoring(IMethod method, ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		fMethod= method;
		fCUnit= inputAsCompilationUnit;
		fChangeManager= new TextEditBasedChangeManager();
	}

	@Override
	public String getName() {
		return "Make static"; //$NON-NLS-1$
	}

	private void findMethodDeclaration() {
		ICompilationUnit compilationUnit= fMethod.getCompilationUnit();

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
				if (resolvedMethod.equals(fMethod)) {
					fMethodDeclaration= node;
					return false;
				}
				return super.visit(node);
			}
		});
	}

	private void modifyMethodDeclaration() throws JavaModelException {
		AST ast= fMethodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, fMethodDeclaration);
		modRewrite.setModifiers(fMethodDeclaration.getModifiers() | Modifier.STATIC, null);
		TextEdit methodDeclarationEdit= rewrite.rewriteAST();

		addEditToChangeManager(methodDeclarationEdit, fCUnit);
	}

	private void findMethodInvocations(ICompilationUnit[] affectedCUs) throws JavaModelException {
		for (ICompilationUnit affectedCU : affectedCUs) {
			BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(affectedCU, null);
			MultiTextEdit multiTextEdit= new MultiTextEdit();
			for (BodyDeclaration body : bodies) {
				ASTNode[] invocations= fTargetProvider.getInvocations(body, null);
				for (ASTNode invocation : invocations) {
					modifyMethodInvocation(multiTextEdit, invocation);
				}
			}
			addEditToChangeManager(multiTextEdit, affectedCU);
		}
	}

	private void modifyMethodInvocation(MultiTextEdit multiTextEdit, ASTNode invocation) throws JavaModelException {
		AST ast= invocation.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		MethodInvocation staticMethodInvocation= ast.newMethodInvocation();
		staticMethodInvocation.setName(ast.newSimpleName(fMethodDeclaration.getName().toString()));
		staticMethodInvocation.setExpression(ast.newSimpleName(((TypeDeclaration) fMethodDeclaration.getParent()).getName().toString()));

		for (Object argument : ((MethodInvocation) invocation).arguments()) {
			staticMethodInvocation.arguments().add(ASTNode.copySubtree(ast, (ASTNode) argument));
		}

		rewrite.replace(invocation, staticMethodInvocation, null);
		TextEdit methodInvocationEdit= rewrite.rewriteAST();
		multiTextEdit.addChild(methodInvocationEdit);
	}

	private void addEditToChangeManager(TextEdit editToAdd, ICompilationUnit iCompilationUnit) {
		//get CompilationUnitChange from ChangeManager, otherwise create one
		CompilationUnitChange compilationUnitChange= (CompilationUnitChange) fChangeManager.get(iCompilationUnit);

		//get all Edits from compilationUnitChange, otherwise create a MultiTextEdit
		MultiTextEdit allTextEdits= (MultiTextEdit) compilationUnitChange.getEdit();
		if (allTextEdits == null)
			allTextEdits= new MultiTextEdit();

		allTextEdits.addChild(editToAdd);
		String changeName= "Change in " + iCompilationUnit.getElementName(); //$NON-NLS-1$
		CompilationUnitChange newCompilationUnitChange= new CompilationUnitChange(changeName, iCompilationUnit);
		newCompilationUnitChange.setEdit(allTextEdits);
		fChangeManager.manage(iCompilationUnit, newCompilationUnitChange);
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		if(fMethod.isConstructor())
			status.addFatalError("Constructor cannot be refactored to static."); //$NON-NLS-1$
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		//Find and Modify MethodDeclaration
		findMethodDeclaration();
		modifyMethodDeclaration();

		//Find and Modify MethodInvocations
		fTargetProvider= TargetProvider.create(fMethodDeclaration);
		fTargetProvider.initialize();

		ICompilationUnit[] affectedCUs= fTargetProvider.getAffectedCompilationUnits(null, new ReferencesInBinaryContext(""), pm); //$NON-NLS-1$
		findMethodInvocations(affectedCUs);


		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange multiChange = new CompositeChange("Make Static", fChangeManager.getAllChanges()); //$NON-NLS-1$
		return multiChange;
	}
}
