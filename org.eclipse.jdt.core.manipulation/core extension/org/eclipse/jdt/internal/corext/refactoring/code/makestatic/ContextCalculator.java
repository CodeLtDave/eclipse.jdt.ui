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

/**
 * This class calculates and provides context for a static method refactoring
 * process. It processes the given input (SelectionFields), which can be an
 * ICompilationUnit or an IMethod, and calculates the corresponding TargetFields,
 * which are used in the actual refactoring process.
 */
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


	/**
     * Constructs a ContextCalculator using a CompilationUnit and a text Selection.
     * These parameters serve as SelectionFields, from which the TargetFields will be calculated.
     *
     * @param inputAsCompilationUnit the CompilationUnit to be processed.
     * @param targetSelection the Selection specifying the part of the CompilationUnit to be processed.
     */
	public ContextCalculator(ICompilationUnit inputAsCompilationUnit, Selection targetSelection) {
		this.fSelectionEditorText= targetSelection;
		this.fSelectionICompilationUnit= inputAsCompilationUnit;
		this.fSelectionIsEditorTextNotIMethod= true;
	}

	/**
     * Constructs a ContextCalculator using an IMethod.
     * This parameter serves as a SelectionField, from which the TargetFields will be calculated.
     *
     * @param method the IMethod to be processed.
     */
	public ContextCalculator(IMethod method) {
		this.fSelectionIMethod= method;
		this.fSelectionIsEditorTextNotIMethod= false;
	}



	public ICompilationUnit getTargetICompilationUnit() {
		return fTargetICompilationUnit;
	}

	public CompilationUnit getTargetCompilationUnit() {
		return fTargetCompilationUnit;
	}

	public IMethod getTargetIMethod() {
		return fTargetIMethod;
	}

	public MethodDeclaration getTargetMethodDeclaration() {
		return fTargetMethodDeclaration;
	}

	/**
     * Calculates the complete context for the refactoring process. This includes determining
     * the TargetFields: CompilationUnit, IMethod, and other related details from the SelectionFields.
     *
     * @throws JavaModelException if a problem occurs while resolving bindings or accessing elements.
     */
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

	/**
     * Converts an ICompilationUnit to a CompilationUnit.
     * This method is used in the process of calculating TargetFields from the SelectionFields.
     *
     * @param compilationUnit the ICompilationUnit to convert.
     * @return the converted CompilationUnit.
     */
	public static CompilationUnit convertICompilationUnitToCompilationUnit(ICompilationUnit compilationUnit) {
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
