/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

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

	public ASTNode getOrComputeSelectionASTNode() {
		if (fSelectionASTNode==null) {
			calculateSelectionASTNode();
		}
		return fSelectionASTNode;
	}

	public IMethodBinding getOrComputeTargetIMethodBinding() {
		if(fTargetIMethodBinding==null) {
			calculateTargetIMethodBinding();
		}
		return fTargetIMethodBinding;
	}

	public IMethod getOrComputeTargetIMethod() {
		if(fTargetIMethod==null) {
			calculateTargetIMethod();
		}
		return fTargetIMethod;
	}

	public ICompilationUnit getOrComputeTargetICompilationUnit() {
		if(fTargetICompilationUnit==null) {
			calculateTargetICompilationUnit();
		}
		return fTargetICompilationUnit;
	}

	public MethodDeclaration getOrComputeTargetMethodDeclaration() throws JavaModelException {
		if (fTargetMethodDeclaration==null) {
			calculateMethodDeclaration();
		}
		return fTargetMethodDeclaration;
	}

	/**
	 * This method calculates the selected ASTNode {@link #fSelectionASTNode}. It finds the Node
	 * that is inside the {@link #fSelectionICompilationUnit} CompilationUnit at the
	 * {@link #fSelectionEditorText} Selection.
	 */
	private void calculateSelectionASTNode() {
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
	private void calculateTargetIMethodBinding() {
		if (getOrComputeSelectionASTNode() instanceof MethodInvocation selectionMethodInvocation) {
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
	private void calculateTargetIMethod() {
		fTargetIMethod= (IMethod) getOrComputeTargetIMethodBinding().getJavaElement();
	}

	/**
	 * Calculates the target ICompilationUnit from the target IMethod. This method retrieves the
	 * declaring type of the {@link #fTargetIMethod}, then gets its associated compilation unit, and
	 * assigns it to {@link #fTargetICompilationUnit}.
	 */
	private void calculateTargetICompilationUnit() {
		fTargetICompilationUnit= getOrComputeTargetIMethod().getDeclaringType().getCompilationUnit();
	}

	/**
	 * Converts the target {@link ICompilationUnit} to a {@link CompilationUnit} and assigns it to
	 * {@link #fTargetCompilationUnit}.
	 */
	private void calculateTargetCompilationUnit() {
		fTargetCompilationUnit= convertICompilationUnitToCompilationUnit(getOrComputeTargetICompilationUnit());
	}

	/**
	 * Resolves the method declaration and binding for the target method.
	 * @throws JavaModelException
	 */
	private void calculateMethodDeclaration() throws JavaModelException {
		calculateTargetCompilationUnit();
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

	private MethodDeclaration getMethodDeclarationFromIMethod(IMethod iMethod, CompilationUnit compilationUnit) throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, compilationUnit);
	}
}
