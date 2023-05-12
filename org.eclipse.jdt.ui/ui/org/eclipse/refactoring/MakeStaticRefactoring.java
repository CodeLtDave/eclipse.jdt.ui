package org.eclipse.refactoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.code.TargetProvider;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;

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

	/**
	 * CompilationUnitRewrites for all affected cus
	 */
	private Map<ICompilationUnit, CompilationUnitRewrite> fRewrites;

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

	private void findMethodDeclaration() {
		ICompilationUnit compilationUnit= fTargetMethod.getCompilationUnit();

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
				if (resolvedMethod.equals(fTargetMethod)) {
					fMethodDeclaration= node;

					// Set the hasInstanceUsages flag
					checkForInstanceUsages(node);
					return false;
				}
				return super.visit(node);
			}

			private void checkForInstanceUsages(MethodDeclaration node) {
				Block methodBody= node.getBody();
				methodBody.accept(new ASTVisitor() {
					@Override
					public boolean visit(SimpleName simpleName) {
						IBinding binding= simpleName.resolveBinding();
						if (binding instanceof IVariableBinding) {
							IVariableBinding variableBinding= (IVariableBinding) binding;
							if (variableBinding.isField() && !Modifier.isStatic(variableBinding.getModifiers())) {
								fHasInstanceUsages= true;
								return false;
							}
						} else if (binding instanceof IMethodBinding) {
							IMethodBinding methodBinding= (IMethodBinding) binding;
							if (!Modifier.isStatic(methodBinding.getModifiers())) {
								fHasInstanceUsages= true;
								return false;
							}
						} else if (binding instanceof ITypeBinding) {
							ITypeBinding typeBinding= (ITypeBinding) binding;
							if (typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers())) {
								fHasInstanceUsages= true;
								return false;
							}
						}
						return super.visit(simpleName);
					}
				});
			}
		});
	}

	private void modifyMethodDeclaration(RefactoringStatus status) throws JavaModelException {
		AST ast= fMethodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		ListRewrite listRewrite= rewrite.getListRewrite(fMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		for (Object obj : fMethodDeclaration.modifiers()) {
			if (obj instanceof org.eclipse.jdt.core.dom.MarkerAnnotation) {
				org.eclipse.jdt.core.dom.MarkerAnnotation markerAnnotation= (org.eclipse.jdt.core.dom.MarkerAnnotation) obj;
				if (markerAnnotation.getTypeName().getFullyQualifiedName().equals("Override")) { //$NON-NLS-1$
					listRewrite.remove(markerAnnotation, null);
				}
			}
		}

		if (fHasInstanceUsages) {
			// Create a new parameter for the method declaration
			SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();
			String className= ((TypeDeclaration) fMethodDeclaration.getParent()).getName().toString();
			newParam.setType(ast.newSimpleType(ast.newName(className)));
			newParam.setName(ast.newSimpleName(className.toLowerCase()));

			//Check if parameter name already used
			List<SingleVariableDeclaration> parameters= fMethodDeclaration.parameters();
			checkDuplicateParamName(status, newParam, parameters);

			if (status.hasFatalError()) {
				return;
			}

			//Check if duplicate method exists after refactoring
			int parameterAmount= parameters.size() + 1;
			checkDuplicateMethod(status, parameterAmount);

			if (status.hasFatalError()) {
				return;
			}

			ListRewrite lrw= rewrite.getListRewrite(fMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
			lrw.insertLast(newParam, null);

			fMethodDeclaration.getBody().accept(new ASTVisitor() {
				@Override
				public boolean visit(SimpleName node) {
					IBinding binding= node.resolveBinding();
					if (binding instanceof IVariableBinding) {
						IVariableBinding variableBinding= (IVariableBinding) binding;
						if (variableBinding.isField() && !Modifier.isStatic(variableBinding.getModifiers())) {
							// Replace instance variable with object.parameter
							FieldAccess fieldAccess= ast.newFieldAccess();
							fieldAccess.setExpression(ast.newSimpleName(newParam.getName().toString()));
							fieldAccess.setName(ast.newSimpleName(node.getIdentifier()));
							rewrite.replace(node.getParent(), fieldAccess, null);
						}
					} else if (binding instanceof IMethodBinding) {
						IMethodBinding methodBinding= (IMethodBinding) binding;
						if (!Modifier.isStatic(methodBinding.getModifiers())) {
							// Replace instance method with object.method
							MethodInvocation methodInvocation= ast.newMethodInvocation();
							methodInvocation.setExpression(ast.newSimpleName(newParam.getName().toString()));
							methodInvocation.setName(ast.newSimpleName(node.getIdentifier()));

							List<Object> temp= Collections.emptyList();

							if (node.getParent().getClass() == SuperMethodInvocation.class) {
								temp= ((SuperMethodInvocation) node.getParent()).arguments();
							} else {
								temp= ((MethodInvocation) node.getParent()).arguments();
							}

							for (Object argument : temp) {
								methodInvocation.arguments().add(ASTNode.copySubtree(ast, (ASTNode) argument));
							}

							rewrite.replace(node.getParent(), methodInvocation, null);
						}
					}
					return super.visit(node);
				}
			});
		}

		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, fMethodDeclaration);
		modRewrite.setModifiers(fMethodDeclaration.getModifiers() | Modifier.STATIC, null);

		TextEdit methodDeclarationEdit= rewrite.rewriteAST();
		addEditToChangeManager(methodDeclarationEdit, fSelectionCompilationUnit);
	}

	private void checkDuplicateParamName(RefactoringStatus status, SingleVariableDeclaration newParam, List<SingleVariableDeclaration> parameters) {
		String newParamName= newParam.getName().toString();
		for (SingleVariableDeclaration param : parameters) {
			String oldParamName= param.getName().toString();
			if (oldParamName.equals(newParamName)) {
				status.merge(RefactoringStatus
						.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.MakeStaticRefactoring_parameter_name_already_used, BasicElementLabels.getJavaElementName(oldParamName))));
				return;
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

			fChangeManager= new TextEditBasedChangeManager();
			fRewrites= new HashMap<>();
			fHasInstanceUsages= false;

			// This refactoring has been invoked on
			// (1) a TextSelection inside an ICompilationUnit or inside an IClassFile (definitely with source), or
			// (2) an IMethod inside a ICompilationUnit or inside an IClassFile (with or without source)

			if (fTargetMethod == null) {
				// (1) invoked on a text selection

				if (fSelectionStart == 0)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);

				// if a text selection exists, source is available.
				CompilationUnit selectionCURoot;
				ASTNode selectionNode= null;
				if (fSelectionCompilationUnit != null) {
					// compilation unit - could use CuRewrite later on
					selectionCURoot= getCachedCURewrite(fSelectionCompilationUnit).getRoot();
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
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations);
				}

				if (targetMethodBinding != null) {
					fTargetMethodBinding= targetMethodBinding.getMethodDeclaration(); // resolve generics
					if (fTargetMethodBinding != null) {
						fTargetMethod= (IMethod) fTargetMethodBinding.getJavaElement();
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
					CompilationUnit selectionCURoot= getCachedCURewrite(fTargetMethod.getCompilationUnit()).getRoot();
					MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fTargetMethod, selectionCURoot);
					fTargetMethodBinding= declaration.resolveBinding().getMethodDeclaration();
				}
			}

			if (fTargetMethod == null || fTargetMethodBinding == null || (!RefactoringAvailabilityTester.isMakeStaticAvailable(fTargetMethod)))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);

			if (fTargetMethod.getDeclaringType().isLocal() || fTargetMethod.getDeclaringType().isAnonymous())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types);

			if (fTargetMethod.isConstructor())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors);

			int flags= fTargetMethod.getFlags();
			if (Modifier.isStatic(flags))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static);

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
			IMethod method= subtype.getMethod(methodName, fTargetMethod.getParameterTypes());
			if (method != null) {
				int flags= method.getFlags();
				if (!Flags.isPrivate(flags)) {
					return true;
				}
			}
		}
		return false;
	}

	private CompilationUnitRewrite getCachedCURewrite(ICompilationUnit unit) {
		CompilationUnitRewrite rewrite= fRewrites.get(unit);
		if (rewrite == null && fMethodDeclaration != null) {
			CompilationUnit cuNode= ASTResolving.findParentCompilationUnit(fMethodDeclaration);
			if (cuNode != null && cuNode.getJavaElement().equals(unit)) {
				rewrite= new CompilationUnitRewrite(unit, cuNode);
				fRewrites.put(unit, rewrite);
			}
		}
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(unit);
			fRewrites.put(unit, rewrite);
		}
		return rewrite;
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

		//Find and Modify MethodDeclaration
		findMethodDeclaration();
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
