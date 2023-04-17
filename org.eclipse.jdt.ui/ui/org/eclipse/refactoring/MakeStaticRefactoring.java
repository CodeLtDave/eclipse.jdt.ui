package org.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;


public class MakeStaticRefactoring extends Refactoring {

	private IMethod fMethod;

	private Change fChange;

	private CompilationUnitRewrite fBaseCuRewrite;;

	private ICompilationUnit fCUnit;
	private Object fBodyUpdater;

	private CompilationUnitRewrite cuRewrite;

	private TextChangeManager fChangeManager;

	public MakeStaticRefactoring(IMethod method, ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		fMethod = method;
		fCUnit= inputAsCompilationUnit;
	}

	private ICompilationUnit getCu() {
		return fMethod.getCompilationUnit();
	}

	@Override
	public String getName() {
		return "Make static"; //$NON-NLS-1$
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {


		if (fBaseCuRewrite == null || !fBaseCuRewrite.getCu().equals(getCu())) {
			fBaseCuRewrite= new CompilationUnitRewrite(getCu());
			fBaseCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
		}
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		ICompilationUnit compilationUnit= fMethod.getCompilationUnit();
		ASTParser parser= ASTParser.newParser(AST.JLS17);
		parser.setSource(compilationUnit);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
		MethodDeclaration fMethDecl= (MethodDeclaration) astRoot.findDeclaringNode(fMethod.getKey());

		System.out.println(fMethod.getKey());

		fChangeManager= new TextChangeManager();
		cuRewrite= new CompilationUnitRewrite(compilationUnit);
		cuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());

		TextChange change= cuRewrite.createChange(true);
		if (change != null)
			fChangeManager.manage(compilationUnit, change);

		ModifierRewrite modRewrite= ModifierRewrite.create(fBaseCuRewrite.getASTRewrite(), fMethDecl);
		modRewrite.setModifiers(Modifier.STATIC, null);

		/*if (fBodyUpdater != null)
		fBodyUpdater.updateBody(fMethDecl, fCuRewrite, fResult);*/


		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

}
