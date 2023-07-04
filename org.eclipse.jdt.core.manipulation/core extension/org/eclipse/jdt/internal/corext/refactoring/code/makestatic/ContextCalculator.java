package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

/**
 * This class calculates and provides context for a static method refactoring process. It processes
 * the given input (SelectionFields), which can be an ICompilationUnit or an IMethod, and calculates
 * the corresponding TargetFields, which are used in the actual refactoring process.
 */
class ContextCalculator {

	private CompilationUnit fTargetCompilationUnit;

	private IMethod fTargetIMethod;

	private IMethodBinding fTargetIMethodBinding;

	private MethodDeclaration fTargetMethodDeclaration;

	private ICompilationUnit fSelectionICompilationUnit;

	private CompilationUnit fSelectionCompilationUnit;

	private Selection fSelectionEditorText;

	private ASTNode fSelectionMethodNode;

	private IMethod fSelectionIMethod;

	private SelectionInputType fSelectionInputType;

	protected enum SelectionInputType {
		IMETHOD, TEXT_SELECTION
	}


	/**
	 * Constructs a ContextCalculator using a CompilationUnit and a text Selection. These parameters
	 * serve as SelectionFields, from which the TargetFields will be calculated.
	 *
	 * @param inputAsICompilationUnit the CompilationUnit to be processed.
	 * @param targetSelection the Selection specifying the part of the CompilationUnit to be
	 *            processed.
	 */
	public ContextCalculator(ICompilationUnit inputAsICompilationUnit, Selection targetSelection) {
		this.fSelectionEditorText= targetSelection;
		this.fSelectionICompilationUnit= inputAsICompilationUnit;
		this.fSelectionInputType= SelectionInputType.TEXT_SELECTION;
	}

	/**
	 * Constructs a ContextCalculator using an IMethod. This parameter serves as a SelectionField,
	 * from which the TargetFields will be calculated.
	 *
	 * @param method the IMethod to be processed.
	 */
	public ContextCalculator(IMethod method) {
		this.fSelectionIMethod= method;
		this.fSelectionInputType= SelectionInputType.IMETHOD;
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

	public SelectionInputType getSelectionInputType() {
		return fSelectionInputType;
	}

	public ICompilationUnit getSelectionICompilationUnit() {
		return fSelectionICompilationUnit;
	}

	public IMethod getSelectionIMethod() {
		return fSelectionIMethod;
	}

	public ASTNode getSelectionMethodNode() {
		return fSelectionMethodNode;
	}

	public IMethodBinding getTargetIMethodBinding() {
		return fTargetIMethodBinding;
	}

	public void calculateSelectionMethodNode() {
		fSelectionCompilationUnit= convertICompilationUnitToCompilationUnit(fSelectionICompilationUnit);
		fSelectionMethodNode= NodeFinder.perform(fSelectionCompilationUnit, fSelectionEditorText.getOffset(), fSelectionEditorText.getLength());

		if (fSelectionMethodNode == null) {
			return;
		}

		if (fSelectionMethodNode.getNodeType() == ASTNode.SIMPLE_NAME) {
			fSelectionMethodNode= fSelectionMethodNode.getParent();
		} else if (fSelectionMethodNode.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			fSelectionMethodNode= ((ExpressionStatement) fSelectionMethodNode).getExpression();
		}
	}

	public void calculateMethodDeclarationFromSelectionMethodNode() {
		if (fSelectionMethodNode instanceof MethodInvocation selectionMethodInvocation) {
			fTargetIMethodBinding= selectionMethodInvocation.resolveMethodBinding();
			fTargetMethodDeclaration= getMethodDeclarationFromIMethod(fSelectionIMethod, fSelectionCompilationUnit);
		} else {
			fTargetMethodDeclaration= (MethodDeclaration) fSelectionMethodNode;
			fTargetIMethodBinding= fTargetMethodDeclaration.resolveBinding();
		}
	}

	public void calculateICompilationUnitFromIMethod() {
		fSelectionICompilationUnit= fSelectionIMethod.getCompilationUnit();
	}

	public void calculateMethodDeclarationFromIMethod() {
		fSelectionCompilationUnit= convertICompilationUnitToCompilationUnit(fSelectionICompilationUnit);
		fTargetMethodDeclaration= getMethodDeclarationFromIMethod(fSelectionIMethod, fSelectionCompilationUnit);
		fTargetIMethodBinding= fTargetMethodDeclaration.resolveBinding();
	}

	public void calculateTargetIMethod() {
		fTargetIMethod= (IMethod) fTargetIMethodBinding.getJavaElement();
	}

	/**
	 * Converts an ICompilationUnit to a CompilationUnit. This method is used in the process of
	 * calculating TargetFields from the SelectionFields.
	 *
	 * @param compilationUnit the ICompilationUnit to convert.
	 * @return the converted CompilationUnit.
	 */
	private static CompilationUnit convertICompilationUnitToCompilationUnit(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	private MethodDeclaration getMethodDeclarationFromIMethod(IMethod iMethod, CompilationUnit compilationUnit) {
		try {
			return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, compilationUnit);
		} catch (JavaModelException e) {
			System.err.println("Failed to get the source range of the method: " + iMethod.getElementName()); //$NON-NLS-1$
			System.err.println("Status: " + e.getJavaModelStatus()); //$NON-NLS-1$
			e.printStackTrace();
		}
		return null;
	}
}
