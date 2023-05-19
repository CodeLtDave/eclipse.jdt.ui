package org.eclipse.refactoring;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.code.TargetProvider;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;

/**
 *
 * @since 3.29
 *
 */
public class MakeStaticRefactoring extends Refactoring {

	private IMethod fTargetMethod;

	private ICompilationUnit fSelectionCompilationUnit;

	private TextEditBasedChangeManager fChangeManager;

	private TargetProvider fTargetProvider;

	private MethodDeclaration fMethodDeclaration;

	private boolean fHasInstanceUsages;

	private int fSelectionStart;

	private int fSelectionLength;

	private IMethodBinding fTargetMethodBinding;

	public MakeStaticRefactoring(ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		fSelectionStart= offset;
		fSelectionLength= length;
		fSelectionCompilationUnit= inputAsCompilationUnit;
	}

	public MakeStaticRefactoring(IMethod method) {
		fTargetMethod= method;
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.MakeStaticRefactoring_name;
	}

	private void modifyMethodDeclaration(RefactoringStatus status) throws JavaModelException {
		AST ast= fMethodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		//Add static modifier to methodDeclaration
		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, fMethodDeclaration);
		modRewrite.setModifiers(fMethodDeclaration.getModifiers() | Modifier.STATIC, null);

		// Find unused name for potential new parameter
		List<SingleVariableDeclaration> alreadyUsedParameters= fMethodDeclaration.parameters();
		String className= ((TypeDeclaration) fMethodDeclaration.getParent()).getName().toString();
		String paramName= generateUniqueParameterName(className, alreadyUsedParameters);

		//Change instance Usages ("this" and "super" to paramName and set fHasInstanceUsage flag
		fMethodDeclaration.getBody().accept(new ChangeInstanceUsagesInMethodBody(paramName, rewrite, ast));

		// check if method is overriding in hierarchy and has no parameters
		//-> after refactoring method would be static and have same signature as parent method
		if (!fHasInstanceUsages && isOverriding(fTargetMethod.getDeclaringType(), fTargetMethod.getElementName())) {
			status.merge(RefactoringStatus
					.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
			return;
		}



		if (fHasInstanceUsages) {
			//Create new parameter
			SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();
			newParam.setType(ast.newSimpleType(ast.newName(className)));
			newParam.setName(ast.newSimpleName(paramName));

			//Check if duplicate method exists after refactoring
			int parameterAmount= alreadyUsedParameters.size() + 1;
			checkDuplicateMethod(status, parameterAmount);
			if (status.hasFatalError())
				return;

			//Add new parameter to method declaration arguments
			ListRewrite lrw= rewrite.getListRewrite(fMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			lrw.insertLast(newParam, null);
		}

		// Delete @Override annotation
		ListRewrite listRewrite= rewrite.getListRewrite(fMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		for (Object obj : fMethodDeclaration.modifiers()) {
			if (obj instanceof org.eclipse.jdt.core.dom.MarkerAnnotation) {
				org.eclipse.jdt.core.dom.MarkerAnnotation markerAnnotation= (org.eclipse.jdt.core.dom.MarkerAnnotation) obj;
				if (markerAnnotation.getTypeName().getFullyQualifiedName().equals("Override")) { //$NON-NLS-1$
					listRewrite.remove(markerAnnotation, null);
				}
			}
		}

		TextEdit methodDeclarationEdit= rewrite.rewriteAST();
		addEditToChangeManager(methodDeclarationEdit, fSelectionCompilationUnit);
	}

	private final class ChangeInstanceUsagesInMethodBody extends ASTVisitor {
		private final String fParamName;

		private final ASTRewrite fRewrite;

		private final AST fAst;

		private ChangeInstanceUsagesInMethodBody(String paramName, ASTRewrite rewrite, AST ast) {
			fParamName= paramName;
			fRewrite= rewrite;
			fAst= ast;
		}

		@Override
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding variableBinding= (IVariableBinding) binding;
				if (variableBinding.isField() && !Modifier.isStatic(variableBinding.getModifiers())) {
					fHasInstanceUsages= true;
					ASTNode parent= node.getParent();

					if (parent instanceof FieldAccess) {
						Expression toReplace= ((FieldAccess) parent).getExpression();
						if (!(toReplace instanceof FieldAccess)) {
							SimpleName replacement= fAst.newSimpleName(fParamName);
							fRewrite.replace(toReplace, replacement, null);
						}
					} else if (parent instanceof SuperFieldAccess) {
						SuperFieldAccess toReplace= (SuperFieldAccess) parent;
						FieldAccess replacement= fAst.newFieldAccess();
						replacement.setExpression(fAst.newSimpleName(fParamName));
						replacement.setName(fAst.newSimpleName(node.getIdentifier()));
						fRewrite.replace(toReplace, replacement, null);
					} else {
						SimpleName toReplace= node;
						FieldAccess replacement= fAst.newFieldAccess();
						replacement.setExpression(fAst.newSimpleName(fParamName));
						replacement.setName(fAst.newSimpleName(node.getIdentifier()));
						fRewrite.replace(toReplace, replacement, null);
					}
				}
			} else if (binding instanceof IMethodBinding) {
				IMethodBinding methodBinding= (IMethodBinding) binding;
				if (!Modifier.isStatic(methodBinding.getModifiers())) {
					fHasInstanceUsages= true;
					ASTNode parent= node.getParent();
					SimpleName replacementExpression= fAst.newSimpleName(fParamName);
					if (parent instanceof MethodInvocation) {
						MethodInvocation methodInvocation= (MethodInvocation) parent;
						Expression optionalExpression= methodInvocation.getExpression();

						if (optionalExpression instanceof SimpleName) {
							fRewrite.replace(optionalExpression, replacementExpression, null);
						} else if (optionalExpression instanceof ThisExpression) {
							fRewrite.replace(optionalExpression, replacementExpression, null);
						} else if (optionalExpression == null) {
							MethodInvocation replacementMethodInvocation= fAst.newMethodInvocation();
							replacementMethodInvocation.setExpression(replacementExpression);
							replacementMethodInvocation.setName(fAst.newSimpleName(methodInvocation.getName().toString()));
							List<Expression> args= methodInvocation.arguments();
							for (Expression arg : args) {
								replacementMethodInvocation.arguments().add(ASTNode.copySubtree(fAst, arg));
							}
							fRewrite.replace(methodInvocation, replacementMethodInvocation, null);
						}
					} else if (parent instanceof SuperMethodInvocation) {
						SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) parent;
						MethodInvocation replacementSuperInvocation= fAst.newMethodInvocation();
						replacementSuperInvocation.setExpression(replacementExpression);
						SimpleName copiedName= fAst.newSimpleName(superMethodInvocation.getName().getIdentifier());
						replacementSuperInvocation.setName(copiedName);
						List<Expression> args= superMethodInvocation.arguments();
						for (Expression arg : args) {
							replacementSuperInvocation.arguments().add(ASTNode.copySubtree(fAst, arg));
						}
						fRewrite.replace(superMethodInvocation, replacementSuperInvocation, null);
					}

				}
			}
			return super.visit(node);
		}
	}

	private String generateUniqueParameterName(String className, List<SingleVariableDeclaration> parameters) {
		String classNameFirstLowerCase= Character.toLowerCase(className.charAt(0)) + className.substring(1); //makes first char lower to match name conventions
		if (parameters == null || parameters.isEmpty())
			return classNameFirstLowerCase;

		boolean duplicateExists= false;
		String combinedName= classNameFirstLowerCase;
		int counter= 2;
		while (true) {
			for (SingleVariableDeclaration param : parameters) {
				String paramString= param.getName().getIdentifier();
				if (!(combinedName.equals(paramString))) {
					duplicateExists= false;
				} else {
					duplicateExists= true;
					break;
				}
			}
			if (duplicateExists) {
				combinedName= classNameFirstLowerCase + counter++;
			} else {
				return combinedName;
			}
		}
	}

	private void checkDuplicateMethod(RefactoringStatus status, int parameterAmount) throws JavaModelException {
		String methodName= fMethodDeclaration.getName().getIdentifier();
		IMethodBinding methodBinding= fMethodDeclaration.resolveBinding();
		ITypeBinding typeBinding= methodBinding.getDeclaringClass();
		IType type= (IType) typeBinding.getJavaElement();

		IMethod method= org.eclipse.jdt.internal.corext.refactoring.Checks.findMethod(methodName, parameterAmount, false, type);

		if (method != null) {

			//check if parameter types match (also compare new parameter that is added by refactoring)
			String className= ((TypeDeclaration) fMethodDeclaration.getParent()).getName().toString();
			String extendedClassName= "Q" + className + ";"; //$NON-NLS-1$ //$NON-NLS-2$
			boolean contains;
			String[] paramTypesOfFoundMethod= method.getParameterTypes();
			String[] paramTypesOfSelectedMethodExtended= new String[parameterAmount];
			paramTypesOfSelectedMethodExtended[parameterAmount - 1]= extendedClassName;
			String[] paramTypesOfSelectedMethod= fTargetMethod.getParameterTypes();

			for (int i= 0; i < paramTypesOfSelectedMethod.length; i++) {
				paramTypesOfSelectedMethodExtended[i]= paramTypesOfSelectedMethod[i];
			}

			for (int i= 0; i < paramTypesOfFoundMethod.length; i++) {
				contains= paramTypesOfSelectedMethodExtended[i].equals(paramTypesOfFoundMethod[i]);
				if (!contains) {
					return;
				}
			}

			status.merge(RefactoringStatus
					.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_duplicate_method_signature));
			return;
		}
	}

	private void findMethodInvocations(RefactoringStatus status, ICompilationUnit[] affectedCUs) throws JavaModelException {
		for (ICompilationUnit affectedCU : affectedCUs) {
			BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(affectedCU, null);
			MultiTextEdit multiTextEdit= new MultiTextEdit();
			for (BodyDeclaration body : bodies) {
				ASTNode[] invocations= fTargetProvider.getInvocations(body, null);
				for (ASTNode invocationASTNode : invocations) {
					MethodInvocation invocation= (MethodInvocation) invocationASTNode;
					modifyMethodInvocation(multiTextEdit, invocation);
				}
			}

			if (status.hasFatalError()) {
				return;
			}

			addEditToChangeManager(multiTextEdit, affectedCU);
		}
	}

	private void modifyMethodInvocation(MultiTextEdit multiTextEdit, MethodInvocation invocation) throws JavaModelException {
		AST ast= invocation.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		MethodInvocation staticMethodInvocation= ast.newMethodInvocation();

		//copy contents in new staticMethodInvocation
		staticMethodInvocation.setName(ast.newSimpleName(fMethodDeclaration.getName().toString()));
		staticMethodInvocation.setExpression(ast.newSimpleName(((TypeDeclaration) fMethodDeclaration.getParent()).getName().toString()));
		for (Object argument : invocation.arguments()) {
			staticMethodInvocation.arguments().add(ASTNode.copySubtree(ast, (ASTNode) argument));
		}

		if (fHasInstanceUsages) {
			//find the variable that needs to be passed as an argument
			ASTNode expression= (invocation.getExpression() != null) ? invocation.getExpression() : ast.newThisExpression();
			staticMethodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
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
		try {
			pm.beginTask(RefactoringCoreMessages.MakeStaticRefactoring_checking_activation, 1);

			// This refactoring has been invoked on
			// (1) a TextSelection inside an ICompilationUnit, or
			// (2) an IMethod inside a ICompilationUnit

			if (fTargetMethod == null) {
				// (1) invoked on a text selection

				if (fSelectionStart == 0)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);

				// if a text selection exists, source is available.
				CompilationUnit selectionCURoot= null;
				ASTNode selectionNode= null;
				if (fSelectionCompilationUnit != null) {
					selectionCURoot= new CompilationUnitRewrite(fSelectionCompilationUnit).getRoot();
					selectionNode= getSelectedNode(fSelectionCompilationUnit, selectionCURoot, fSelectionStart, fSelectionLength);
				}

				if (selectionNode == null)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);

				IMethodBinding targetMethodBinding= null;

				int nodeType= selectionNode.getNodeType();

				if (nodeType == ASTNode.METHOD_INVOCATION) {
					targetMethodBinding= ((MethodInvocation) selectionNode).resolveMethodBinding();
				} else if (nodeType == ASTNode.METHOD_DECLARATION) {
					targetMethodBinding= ((MethodDeclaration) selectionNode).resolveBinding();
				} else if (nodeType == ASTNode.SUPER_METHOD_INVOCATION) {
					//Invoke refactoring on SuperMethodInvocation is not allowed
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations);
				}

				if (targetMethodBinding != null) {
					fTargetMethodBinding= targetMethodBinding.getMethodDeclaration(); // resolve generics
					if (fTargetMethodBinding != null) {
						fTargetMethod= (IMethod) fTargetMethodBinding.getJavaElement();
						fMethodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(fTargetMethod, selectionCURoot);
					}
				}
				if (targetMethodBinding == null || fTargetMethodBinding == null) {
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
				}
			} else {
				// (2) invoked on an IMethod: Source may not be available
				fSelectionCompilationUnit= fTargetMethod.getCompilationUnit();
				if (fTargetMethod.getDeclaringType().isAnnotation())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_annotation);

				if (fTargetMethod.getCompilationUnit() != null) {
					// source method
					CompilationUnit selectionCURoot= new CompilationUnitRewrite(fTargetMethod.getCompilationUnit()).getRoot();
					fMethodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(fTargetMethod, selectionCURoot);
					fTargetMethodBinding= fMethodDeclaration.resolveBinding().getMethodDeclaration();
				}
			}

			if (fTargetMethod == null || fTargetMethodBinding == null || (!RefactoringAvailabilityTester.isMakeStaticAvailable(fTargetMethod)))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);

			if (fTargetMethod.getDeclaringType().isLocal() || fTargetMethod.getDeclaringType().isAnonymous())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types);

			if (fTargetMethod.isConstructor())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors);

			// check if method already static
			int flags= fTargetMethod.getFlags();
			if (Modifier.isStatic(flags))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static);

			// check if method is overridden in hierarchy
			if (isOverridden(fTargetMethod.getDeclaringType(), fTargetMethod.getElementName())) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_is_overridden_in_subtype);
			}

			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	public boolean isOverridden(IType type, String methodName) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] subtypes= hierarchy.getAllSubtypes(type);
		for (IType subtype : subtypes) {
			IMethod[] methods= subtype.getMethods();
			//IMethod method= subtype.getMethod(methodName, fTargetMethod.getParameterTypes());
			for (IMethod method : methods) {
				if (method.isSimilar(fTargetMethod)) {
					int flags= method.getFlags();
					if (!Flags.isPrivate(flags) || (!Flags.isStatic(flags))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isOverriding(IType type, String methodName) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] supertypes= hierarchy.getAllSupertypes(type);
		for (IType supertype : supertypes) {
			if (!(supertype.getElementName().equals("Object"))) { //$NON-NLS-1$
				IMethod method= supertype.getMethod(methodName, fTargetMethod.getParameterTypes());
				if (method != null) {
					return true;
				}
			}
		}
		return false;
	}


	private static ASTNode getSelectedNode(ICompilationUnit unit, CompilationUnit root, int offset, int length) {
		ASTNode node= null;
		try {
			if (unit != null)
				node= checkNode(NodeFinder.perform(root, offset, length, unit));
			else
				node= checkNode(NodeFinder.perform(root, offset, length));
		} catch (JavaModelException e) {
			// Do nothing
		}
		if (node != null)
			return node;
		return checkNode(NodeFinder.perform(root, offset, length));
	}

	private static ASTNode checkNode(ASTNode node) {
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement) node).getExpression();
		}
		switch (node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
				return node;
		}
		return null;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextEditBasedChangeManager();
		fHasInstanceUsages= false;

		//Modify MethodDeclaration
		modifyMethodDeclaration(status);

		if (status.hasFatalError()) {
			return status;
		}

		//Find and Modify MethodInvocations
		fTargetProvider= TargetProvider.create(fMethodDeclaration);
		fTargetProvider.initialize();

		ICompilationUnit[] affectedCUs= fTargetProvider.getAffectedCompilationUnits(status, new ReferencesInBinaryContext(""), pm); //$NON-NLS-1$
		findMethodInvocations(status, affectedCUs);


		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange multiChange= new CompositeChange("Make Static", fChangeManager.getAllChanges()); //$NON-NLS-1$
		return multiChange;
	}
}
