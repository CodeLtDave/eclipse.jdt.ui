package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

class ContextCalculator {

	private ICompilationUnit fTargetICompilationUnit;

	private CompilationUnit fTargetCompilationUnit;

	private IMethod fTargetIMethod;

	private MethodDeclaration fTargetMethodDeclaration;

	private ICompilationUnit fSelectionICompilationUnit;

	private CompilationUnit fSelectionCompilationUnit;

	private Selection fSelectionEditorText;

	private IMethod fSelectionIMethod;

	private boolean fSelectionIsEditorTextNotIMethod;


	public ContextCalculator(ICompilationUnit inputAsCompilationUnit, Selection targetSelection) {
		this.fSelectionEditorText= targetSelection;
		this.fSelectionICompilationUnit= inputAsCompilationUnit;
		this.fSelectionIsEditorTextNotIMethod= true;
	}

	public ContextCalculator(IMethod method) {
		this.fSelectionIMethod= method;
		this.fSelectionIsEditorTextNotIMethod= false;
	}

	public ICompilationUnit getSelectionCompilationUnit() {
		return this.fSelectionICompilationUnit;
	}

	public MethodDeclaration getTargetMethod() {
		return this.fTargetMethodDeclaration;
	}

	public void calculateCompleteContext() throws JavaModelException {
		IMethodBinding targetIMethodBinding;
		if (fSelectionIsEditorTextNotIMethod) {
			fSelectionCompilationUnit= convertICompilationUnitToCompilationUnit(fSelectionICompilationUnit);
			ASTNode selectionMethodNode= NodeFinder.perform(fSelectionCompilationUnit, fSelectionEditorText.getOffset(), fSelectionEditorText.getLength(), fSelectionICompilationUnit);
			if (selectionMethodNode instanceof MethodInvocation selectionMethodInvocation) {
				targetIMethodBinding= selectionMethodInvocation.resolveMethodBinding();
				fSelectionIMethod= (IMethod) targetIMethodBinding.getMethodDeclaration().getJavaElement();
				fTargetMethodDeclaration= getMethodDeclarationFromIMethod(fSelectionIMethod, fSelectionCompilationUnit);
			} else {
				fTargetMethodDeclaration= (MethodDeclaration) selectionMethodNode;
				targetIMethodBinding= fTargetMethodDeclaration.resolveBinding();
			}
		}

		else {
			fSelectionICompilationUnit= fSelectionIMethod.getCompilationUnit();
			fSelectionCompilationUnit= convertICompilationUnitToCompilationUnit(fSelectionICompilationUnit);
			fTargetMethodDeclaration= getMethodDeclarationFromIMethod(fSelectionIMethod, fSelectionCompilationUnit);
			targetIMethodBinding= fTargetMethodDeclaration.resolveBinding();
		}
		fTargetIMethod= (IMethod) targetIMethodBinding.getJavaElement();
		fTargetICompilationUnit= fTargetIMethod.getCompilationUnit();
	}

	public CompilationUnit convertICompilationUnitToCompilationUnit(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	private MethodDeclaration getMethodDeclarationFromIMethod(IMethod iMethod, CompilationUnit compilationUnit) throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, compilationUnit);
	}


}
