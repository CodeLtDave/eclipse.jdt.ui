package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import java.util.List;

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
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

/**
 * This class calculates and provides context for a static method refactoring process. It processes
 * the given input (SelectionFields), which can be an ICompilationUnit or an IMethod, and calculates
 * the corresponding TargetFields, which are used in the actual refactoring process.
 */
class ContextCalculator {

	private SelectionInputType fSelectionInputType;

	private Selection fSelectionEditorText;

	private ICompilationUnit fSelectionICompilationUnit;

	private CompilationUnit fSelectionCompilationUnit;

	private ASTNode fSelectionASTNode;

	private IMethodBinding fTargetIMethodBinding;

	private IMethod fTargetIMethod;

	private ICompilationUnit fTargetICompilationUnit;

	private CompilationUnit fTargetCompilationUnit;

	private MethodDeclaration fTargetMethodDeclaration;

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
		this.fSelectionInputType= SelectionInputType.TEXT_SELECTION;
		this.fSelectionEditorText= targetSelection;
		this.fSelectionICompilationUnit= inputAsICompilationUnit;
	}

	/**
	 * Constructs a ContextCalculator using an IMethod. This parameter serves as a SelectionField,
	 * from which the TargetFields will be calculated.
	 *
	 * @param method the IMethod to be processed.
	 */
	public ContextCalculator(IMethod method) {
		this.fSelectionInputType= SelectionInputType.IMETHOD;
		this.fTargetIMethod= method;
	}

	public SelectionInputType getSelectionInputType() {
		return fSelectionInputType;
	}

	public Selection getSelectionEditorText() {
		return fSelectionEditorText;
	}

	public ICompilationUnit getSelectionICompilationUnit() {
		return fSelectionICompilationUnit;
	}

	public ASTNode getSelectionASTNode() {
		return fSelectionASTNode;
	}

	public IMethodBinding getTargetIMethodBinding() {
		return fTargetIMethodBinding;
	}

	public IMethod getTargetIMethod() {
		return fTargetIMethod;
	}

	public MethodDeclaration getTargetMethodDeclaration() {
		return fTargetMethodDeclaration;
	}

	public List<SingleVariableDeclaration> getTargetMethodInputParameters() {
		return fTargetMethodDeclaration.parameters();
	}

	/**
	 * This method calculates the selected ASTNode {@link #fSelectionASTNode}. It finds the Node
	 * that is inside the {@link #fSelectionICompilationUnit} CompilationUnit at the
	 * {@link #fSelectionEditorText} Selection.
	 */
	public void calculateSelectionASTNode() {
		fSelectionCompilationUnit= convertICompilationUnitToCompilationUnit(fSelectionICompilationUnit);
		fSelectionASTNode= NodeFinder.perform(fSelectionCompilationUnit, fSelectionEditorText.getOffset(), fSelectionEditorText.getLength());

		if (fSelectionASTNode == null) {
			return;
		}

		if (fSelectionASTNode.getNodeType() == ASTNode.SIMPLE_NAME) {
			fSelectionASTNode= fSelectionASTNode.getParent();
		} else if (fSelectionASTNode.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			fSelectionASTNode= ((ExpressionStatement) fSelectionASTNode).getExpression();
		}
	}

	/**
	 * This method calculates the {@link #fTargetIMethodBinding}. If the {@link #fSelectionASTNode}
	 * is an instance of {@link MethodInvocation} or {@link MethodDeclaration}, it resolves the
	 * corresponding {@link IMethodBinding} from that instance.
	 */
	public void calculateTargetIMethodBinding() {
		if (fSelectionASTNode instanceof MethodInvocation selectionMethodInvocation) {
			fTargetIMethodBinding= selectionMethodInvocation.resolveMethodBinding();
		} else if (fSelectionASTNode instanceof MethodDeclaration selectionMethodDeclaration) {
			fTargetIMethodBinding= selectionMethodDeclaration.resolveBinding();
		}
	}

	/**
	 * Calculates the target IMethod from the target IMethodBinding. This method retrieves the
	 * IMethod represented by the {@link #fTargetIMethodBinding} and assigns it to
	 * {@link #fTargetIMethod}
	 */
	public void calculateTargetIMethod() {
		fTargetIMethod= (IMethod) fTargetIMethodBinding.getJavaElement();
	}

	/**
	 * Calculates the target ICompilationUnit from the target IMethod. This method retrieves the
	 * declaring type of the {@link #fTargetIMethod}, then gets its associated compilation unit, and
	 * assigns it to {@link #fTargetICompilationUnit}.
	 */
	public void calculateTargetICompilationUnit() {
		fTargetICompilationUnit= fTargetIMethod.getDeclaringType().getCompilationUnit();
	}

	/**
	 * Converts the target {@link ICompilationUnit} to a {@link CompilationUnit} and assigns it to
	 * {@link #fTargetCompilationUnit}.
	 */
	public void calculateTargetCompilationUnit() {
		fTargetCompilationUnit= convertICompilationUnitToCompilationUnit(fTargetICompilationUnit);
	}

	/**
	 * Resolves the method declaration and binding for the target method.
	 */
	public void calculateMethodDeclaration() {
		fTargetMethodDeclaration= getMethodDeclarationFromIMethod(fTargetIMethod, fTargetCompilationUnit);
		fTargetIMethodBinding= fTargetMethodDeclaration.resolveBinding();
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
