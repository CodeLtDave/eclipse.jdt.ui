/*******************************************************************************
 * Copyright (c) 2023 David Erdös and Michael Bangas.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Erdös - initial API and implementation
 *     Michael Bangas - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import java.util.ArrayList;
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
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.code.TargetProvider;
import org.eclipse.jdt.internal.corext.refactoring.code.makestatic.ContextCalculator.SelectionInputType;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;

/**
 *
 * @since 3.29
 *
 */
public class MakeStaticRefactoring extends Refactoring {

	private IMethod fTargetMethod; //TODO remove

	private ICompilationUnit fSelectionICompilationUnit; //TODO remove

	private TextEditBasedChangeManager fChangeManager;

	private TargetProvider fTargetProvider;

	private MethodDeclaration fTargetMethodDeclaration;

	private boolean fHasInstanceUsages;

	private Selection fTargetSelection; //TODO remove

	private IMethodBinding fTargetMethodBinding;

	private ContextCalculator fContextCalculator;

	private InitialConditionsChecker fInitialConditionsChecker;

	public MakeStaticRefactoring(ICompilationUnit inputAsCompilationUnit, int selectionStart, int selectionLength) {
		fTargetSelection= Selection.createFromStartLength(selectionStart, selectionLength);
		fSelectionICompilationUnit= inputAsCompilationUnit;
		fContextCalculator= new ContextCalculator(fSelectionICompilationUnit, fTargetSelection);
		fInitialConditionsChecker= new InitialConditionsChecker();
	}

	public MakeStaticRefactoring(IMethod method) {
		fContextCalculator= new ContextCalculator(method);
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.MakeStaticRefactoring_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();

		SelectionInputType selectionInputType= fContextCalculator.getSelectionInputType();

		if (selectionInputType == SelectionInputType.TEXT_SELECTION) {
			status.merge(fInitialConditionsChecker.checkTextSelectionStart(fTargetSelection));
			status.merge(fInitialConditionsChecker.checkValidICompilationUnit(fContextCalculator.getSelectionICompilationUnit()));
			if (status.hasError()) {
				return status;
			}

			fContextCalculator.calculateSelectionMethodNode();
			ASTNode selectionMethodNode= fContextCalculator.getSelectionMethodNode();

			status.merge(fInitialConditionsChecker.checkNodeIsValidMethod(selectionMethodNode));
			if (status.hasError()) {
				return status;
			}

			fContextCalculator.calculateMethodDeclarationFromSelectionMethodNode();
		} else {
			IMethod selectionIMethod= fContextCalculator.getSelectionIMethod();
			status.merge(fInitialConditionsChecker.checkIMethodIsValid(selectionIMethod));
			if (status.hasError()) {
				return status;
			}

			fContextCalculator.calculateICompilationUnitFromIMethod();

			status.merge(fInitialConditionsChecker.checkValidICompilationUnit(fContextCalculator.getSelectionICompilationUnit()));
			if (status.hasError()) {
				return status;
			}
		}

		fContextCalculator.calculateTargetIMethod();

		return status;
	}

	private RefactoringStatus checkInitialConditionsFromTextSelection() throws JavaModelException {

		RefactoringStatus status= new RefactoringStatus();

		if (fTargetSelection.getOffset() == 0)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);

		CompilationUnit selectionCURoot= new CompilationUnitRewrite(fSelectionICompilationUnit).getRoot();
		ASTNode selectionNode= getSelectedNode(fSelectionICompilationUnit, selectionCURoot, fTargetSelection.getOffset(), fTargetSelection.getLength());

		if (selectionNode == null) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
		}

		fTargetMethodBinding= getMethodBindingFromSelectionNode(selectionNode, status);

		if (status.hasError()) {
			return status;
		}


		if (fTargetMethodBinding != null) {
			fTargetMethod= (IMethod) fTargetMethodBinding.getJavaElement();
			if (fTargetMethod.getCompilationUnit() == null) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_source_not_available_for_selected_method);
			}
			CompilationUnit targetCU= convertICUtoCU(fTargetMethod.getCompilationUnit());
			fTargetMethodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(fTargetMethod, targetCU);
		}

		if (fTargetMethodBinding == null) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
		}

		return status;
	}

	private static ASTNode getSelectedNode(ICompilationUnit unit, CompilationUnit root, int offset, int length) throws JavaModelException { //TODO fix method structure, bad coding style
		ASTNode node= null;
		if (unit != null) {
			node= checkNode(NodeFinder.perform(root, offset, length, unit));
		} else {
			node= checkNode(NodeFinder.perform(root, offset, length));
		}
		if (node != null) {
			return node;
		}
		return checkNode(NodeFinder.perform(root, offset, length));
	}

	private static ASTNode checkNode(ASTNode node) { //TODO uninformative name
		if (node == null) {
			return null;
		}

		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement) node).getExpression();
		}

		int nodeType= node.getNodeType();
		if (nodeType == ASTNode.METHOD_INVOCATION || nodeType == ASTNode.METHOD_DECLARATION || nodeType == ASTNode.SUPER_METHOD_INVOCATION) {
			return node;
		}

		return null;
	}

	private IMethodBinding getMethodBindingFromSelectionNode(ASTNode selectionNode, RefactoringStatus status) {

		int nodeType= selectionNode.getNodeType();

		if (nodeType == ASTNode.METHOD_INVOCATION) {
			return ((MethodInvocation) selectionNode).resolveMethodBinding();
		} else if (nodeType == ASTNode.METHOD_DECLARATION) {
			return ((MethodDeclaration) selectionNode).resolveBinding();
		} else if (nodeType == ASTNode.SUPER_METHOD_INVOCATION) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_super_method_invocations));
			return null;
		}

		return null;
	}

	private RefactoringStatus checkInitialConditionsFromMethod() throws JavaModelException {
		RefactoringStatus status= new RefactoringStatus();

		fSelectionICompilationUnit= fTargetMethod.getCompilationUnit();
		if (fTargetMethod.getDeclaringType().isAnnotation()) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_annotation);
		}
		if (fSelectionICompilationUnit != null) { //TODO extract into findMethodDeclaration
			CompilationUnit selectionCURoot= new CompilationUnitRewrite(fSelectionICompilationUnit).getRoot();
			fTargetMethodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(fTargetMethod, selectionCURoot);
			fTargetMethodBinding= fTargetMethodDeclaration.resolveBinding().getMethodDeclaration();
		} else {
			//TODO what if null
		}

		return status;
	}

	private RefactoringStatus checkGeneralInitialConditions() throws JavaModelException {
		if (fTargetMethod == null || fTargetMethodBinding == null) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_on_this_selection);
		}

		if (fTargetMethod.getDeclaringType().isLocal() || fTargetMethod.getDeclaringType().isAnonymous()) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_local_or_anonymous_types);
		}
		if (fTargetMethod.isConstructor()) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_constructors);
		}
		int flags= fTargetMethod.getFlags();
		if (Modifier.isStatic(flags)) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_already_static);
		}
		if (isOverridden(fTargetMethod.getDeclaringType())) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_method_is_overridden_in_subtype);
		}
		return null;
	}

	private boolean isOverridden(IType type) throws JavaModelException { //TODO duplicate isOverriding()?
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] subtypes= hierarchy.getAllSubtypes(type);
		for (IType subtype : subtypes) {
			IMethod[] methods= subtype.getMethods();
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

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextEditBasedChangeManager();
		fHasInstanceUsages= false;

		//Modify MethodDeclaration
		modifyMethodDeclaration(status);

		if (status.hasError()) {
			return status;
		}

		//Find and Modify MethodInvocations
		fTargetProvider= TargetProvider.create(fTargetMethodDeclaration);
		fTargetProvider.initialize();

		ICompilationUnit[] affectedCUs= fTargetProvider.getAffectedCompilationUnits(status, new ReferencesInBinaryContext(""), pm); //$NON-NLS-1$
		handleMethodInvocations(status, affectedCUs);

		return status;
	}

	private void modifyMethodDeclaration(RefactoringStatus status) throws JavaModelException {
		AST ast= fTargetMethodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		//Add static modifier to methodDeclaration
		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, fTargetMethodDeclaration);
		modRewrite.setModifiers(fTargetMethodDeclaration.getModifiers() | Modifier.STATIC, null);

		//If fMethodDeclaraion hasInstanceUsages, adding a new parameter will be necessary
		List<SingleVariableDeclaration> alreadyUsedParameters= fTargetMethodDeclaration.parameters();
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		String paramName= generateUniqueParameterName(className, alreadyUsedParameters);

		//Change instance Usages ("this" and "super") to paramName and set fHasInstanceUsage flag
		InstanceUsageRewriter visitor= new InstanceUsageRewriter(paramName, rewrite, ast, status, fTargetMethodDeclaration);
		fTargetMethodDeclaration.getBody().accept(visitor);
		fHasInstanceUsages= visitor.fHasInstanceUsages;


		//Refactored method could unintentionally hide method of parent class
		if (!fHasInstanceUsages && isOverriding(fTargetMethod.getDeclaringType())) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
			return;
		}

		IType parentType= fTargetMethod.getDeclaringType();
		ITypeParameter[] classTypeParameters= parentType.getTypeParameters();

		//Adding an instance parameter to the newly static method to ensure it can still access class-level state and behavior.
		if (fHasInstanceUsages) {
			addInstanceAsParamIfUsed(status, ast, rewrite, alreadyUsedParameters, className, paramName, classTypeParameters);
		}

		//Updates typeParamList of MethodDeclaration and inserts new typeParams to JavaDoc
		updateMethodParameterTypes(status, ast, rewrite, classTypeParameters);

		//A static method can't have override annotations
		deleteOverrideAnnotation(rewrite);

		//Changes can't be applied to directly to AST, edits are saved in fChangeManager
		TextEdit methodDeclarationEdit= rewrite.rewriteAST();
		addEditToChangeManager(methodDeclarationEdit, fTargetMethod.getCompilationUnit());
	}

	private String generateUniqueParameterName(String className, List<SingleVariableDeclaration> parameters) {
		String classNameFirstLowerCase= Character.toLowerCase(className.charAt(0)) + className.substring(1); //makes first char lower to match name conventions
		if (parameters == null || parameters.isEmpty()) {
			return classNameFirstLowerCase;
		}
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

	private boolean isOverriding(IType type) throws JavaModelException { //TODO duplicate isOverridden()?
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] supertypes= hierarchy.getAllSupertypes(type);
		for (IType supertype : supertypes) {
			if (!(supertype.getElementName().equals("Object"))) { //$NON-NLS-1$
				IMethod[] method= supertype.findMethods(fTargetMethod);
				if (method != null) {
					return true;
				}
			}
		}
		return false;
	}

	private void addInstanceAsParamIfUsed(RefactoringStatus status, AST ast, ASTRewrite rewrite, List<SingleVariableDeclaration> alreadyUsedParameters, String className, String paramName,
			ITypeParameter[] classTypeParameters) throws JavaModelException {
		SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();

		//If generic TypeParameters exist in class the newParam type needs to be parameterized
		if (classTypeParameters.length != 0) {
			SimpleType simpleType= ast.newSimpleType(ast.newName(className));
			ParameterizedType parameterizedType= ast.newParameterizedType(simpleType);
			for (int i= 0; i < classTypeParameters.length; i++) {
				SimpleType typeParameter= ast.newSimpleType(ast.newSimpleName(classTypeParameters[i].getElementName()));
				parameterizedType.typeArguments().add(typeParameter);
			}
			newParam.setType(parameterizedType);

		} else {
			newParam.setType(ast.newSimpleType(ast.newName(className)));
		}
		newParam.setName(ast.newSimpleName(paramName));

		//While refactoring, the method signature might change; ensure the revised method doesn't unintentionally override an existing one.
		int parameterAmount= alreadyUsedParameters.size() + 1;
		checkDuplicateMethod(status, parameterAmount);

		//Add new parameter to method declaration arguments
		ListRewrite lrw= rewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		lrw.insertFirst(newParam, null);

		//Changes to fTargetMethodDeclaration's signature need to be adjusted in JavaDocs too
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();
		if (javadoc != null) {
			TagElement newParameterTag= ast.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(ast.newSimpleName(paramName));
			ListRewrite tagsRewrite= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertFirst(newParameterTag, null);
		}
	}

	private void checkDuplicateMethod(RefactoringStatus status, int parameterAmount) throws JavaModelException {
		String methodName= fTargetMethodDeclaration.getName().getIdentifier();
		IMethodBinding methodBinding= fTargetMethodDeclaration.resolveBinding();
		ITypeBinding typeBinding= methodBinding.getDeclaringClass();
		IType type= (IType) typeBinding.getJavaElement();

		IMethod method= org.eclipse.jdt.internal.corext.refactoring.Checks.findMethod(methodName, parameterAmount, false, type);

		if (method == null) {
			return;
		}

		//check if parameter types match (also compare new parameter that is added by refactoring)
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		String extendedClassName= "Q" + className + ";"; //$NON-NLS-1$ //$NON-NLS-2$
		boolean contains;
		String[] paramTypesOfFoundMethod= method.getParameterTypes();
		String[] paramTypesOfSelectedMethodExtended= new String[parameterAmount];
		paramTypesOfSelectedMethodExtended[0]= extendedClassName;
		String[] paramTypesOfSelectedMethod= fTargetMethod.getParameterTypes();

		for (int i= 0; i < paramTypesOfSelectedMethod.length; i++) {
			paramTypesOfSelectedMethodExtended[i + 1]= paramTypesOfSelectedMethod[i];
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

	private void updateMethodParameterTypes(RefactoringStatus status, AST ast, ASTRewrite rewrite, ITypeParameter[] classTypeParameters) throws JavaModelException {
		ListRewrite typeParamsRewrite= rewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.TYPE_PARAMETERS_PROPERTY);
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();

		List<String> methodParameterTypes= getMethodParameterTypes();
		List<String> methodTypeParametersNames= getTypeParameterNames();

		addTypeParametersFromClass(status, ast, rewrite, classTypeParameters, typeParamsRewrite, javadoc, methodParameterTypes, methodTypeParametersNames);
	}

	private List<String> getMethodParameterTypes() {
		List<SingleVariableDeclaration> methodParams= fTargetMethodDeclaration.parameters();
		List<String> methodParameterTypes= new ArrayList<>(methodParams.size());
		for (SingleVariableDeclaration methodParam : methodParams) {
			Type type= methodParam.getType();
			//if type is parameterized, then those typeParams need to be included in TypeParamList of method
			if (type.isParameterizedType()) {
				ParameterizedType parameterizedType= (ParameterizedType) type;
				List<Type> typeParamsOfMethodParam= parameterizedType.typeArguments();
				for (Type typeParamOfMethodParam : typeParamsOfMethodParam) {
					methodParameterTypes.add(typeParamOfMethodParam.resolveBinding().getName());
				}
			}
			String typeName= type.toString();
			methodParameterTypes.add(typeName);
		}
		return methodParameterTypes;
	}

	private List<String> getTypeParameterNames() {
		List<TypeParameter> methodTypeParameters= fTargetMethodDeclaration.typeParameters();
		List<String> methodTypeParametersNames= new ArrayList<>(methodTypeParameters.size());
		for (TypeParameter methodTypeParam : methodTypeParameters) {
			methodTypeParametersNames.add(methodTypeParam.getName().getIdentifier());
		}
		return methodTypeParametersNames;
	}

	private void addTypeParametersFromClass(RefactoringStatus status, AST ast, ASTRewrite rewrite, ITypeParameter[] classTypeParameters, ListRewrite typeParamsRewrite, Javadoc javadoc,
			List<String> methodParameterTypes, List<String> methodTypeParametersNames) throws JavaModelException {
		if (classTypeParameters.length != 0) {
			for (int i= 0; i < classTypeParameters.length; i++) {
				String[] bounds= classTypeParameters[i].getBounds();
				TypeParameter typeParameter= ast.newTypeParameter();
				typeParameter.setName(ast.newSimpleName(classTypeParameters[i].getElementName()));
				for (String bound : bounds) {
					if (bound.contains("?")) { //$NON-NLS-1$
						//WildCardTypes are not allowed as bounds
						status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_wildCardTypes_as_bound));
						return;
					} else {
						SimpleType boundType= ast.newSimpleType(ast.newSimpleName(bound));
						typeParameter.typeBounds().add(boundType);
					}
				}
				//Check if method needs this TypeParameter (only if one or more methodParams are of this type OR method has instance usage OR an instance of parent class is used as methodParam)
				String typeParamName= typeParameter.getName().getIdentifier();
				String typeParamNameAsArray= typeParamName + "[]"; //$NON-NLS-1$
				boolean paramIsNeeded= methodParameterTypes.contains(typeParamName) || methodParameterTypes.contains(typeParamNameAsArray);
				if (fHasInstanceUsages || paramIsNeeded) {
					//only insert if typeParam not already existing
					if (!methodTypeParametersNames.contains(typeParameter.getName().getIdentifier())) {
						typeParamsRewrite.insertLast(typeParameter, null);
						addNewTypeParamsToJavaDoc(ast, rewrite, javadoc, typeParameter);
					}
				}
			}
		}
	}

	private void addNewTypeParamsToJavaDoc(AST ast, ASTRewrite rewrite, Javadoc javadoc, TypeParameter typeParameter) {
		if (javadoc != null) {
			//add new type params to javaDoc
			TextElement textElement= ast.newTextElement();
			textElement.setText("<" + typeParameter.getName().getIdentifier() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			TagElement newParameterTag= ast.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(textElement);
			ListRewrite tagsRewrite= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertLast(newParameterTag, null);
		}
	}

	private void deleteOverrideAnnotation(ASTRewrite rewrite) {
		ListRewrite listRewrite= rewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		for (Object obj : fTargetMethodDeclaration.modifiers()) {
			if (obj instanceof org.eclipse.jdt.core.dom.MarkerAnnotation markerAnnotation) {
				if (markerAnnotation.getTypeName().getFullyQualifiedName().equals("Override")) { //$NON-NLS-1$
					listRewrite.remove(markerAnnotation, null);
				}
			}
		}
	}

	private void handleMethodInvocations(RefactoringStatus status, ICompilationUnit[] affectedCUs) throws JavaModelException {
		for (ICompilationUnit affectedCU : affectedCUs) {

			//Check MethodReferences that use selected method -> cancel refactoring
			CompilationUnit cu= convertICUtoCU(affectedCU);
			cu.accept(new MethodReferenceFinder(status));

			if (status.hasFatalError()) {
				return;
			}

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

	//Convert ICompialtionUnit to CompilationUnit
	private CompilationUnit convertICUtoCU(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);

		return (CompilationUnit) parser.createAST(null);
	}

	private final class MethodReferenceFinder extends ASTVisitor {
		private final RefactoringStatus fstatus;

		private MethodReferenceFinder(RefactoringStatus status) {
			fstatus= status;
		}

		@Override
		public boolean visit(ExpressionMethodReference node) {
			// Check if the method reference refers to the selected method
			if (!fstatus.hasFatalError()) {
				ITypeBinding typeBinding= node.getExpression().resolveTypeBinding();
				IMethodBinding methodBinding= node.resolveMethodBinding();
				if (isTargetMethodReference(methodBinding, typeBinding)) {
					fstatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
				}
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(SuperMethodReference node) {
			// Check if the method reference refers to the selected method
			if (!fstatus.hasFatalError()) {
				IMethodBinding methodBinding= node.resolveMethodBinding();
				if (isTargetMethodReference(methodBinding)) {
					ITypeBinding declaringTypeBinding= methodBinding.getDeclaringClass();
					if (fTargetMethodBinding.getDeclaringClass().isEqualTo(declaringTypeBinding)) {
						fstatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_method_references));
					}
				}
			}
			return super.visit(node);
		}

		private boolean isTargetMethodReference(IMethodBinding methodBinding) {
			return fTargetMethodBinding.isEqualTo(methodBinding);
		}

		private boolean isTargetMethodReference(IMethodBinding methodBinding, ITypeBinding typeBinding) {
			return fTargetMethodBinding.isEqualTo(methodBinding) && fTargetMethodBinding.getDeclaringClass().isEqualTo(typeBinding);
		}
	}

	private void modifyMethodInvocation(MultiTextEdit multiTextEdit, MethodInvocation invocation) throws JavaModelException {
		AST ast= invocation.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		if (fHasInstanceUsages) {
			ASTNode newArg;
			if (invocation.getExpression() != null) {
				newArg= ASTNode.copySubtree(ast, invocation.getExpression()); // copy the expression
			} else {
				// We need to find the class that this invocation is inside
				ASTNode parent= invocation;
				while ((!(parent instanceof AbstractTypeDeclaration)) && (!(parent instanceof AnonymousClassDeclaration))) {
					parent= parent.getParent();
				}

				boolean isMember= false;
				if (parent instanceof AbstractTypeDeclaration) {
					AbstractTypeDeclaration currentClass= (AbstractTypeDeclaration) parent;
					if (currentClass.isMemberTypeDeclaration()) {
						isMember= true;
					}
				} else if (parent instanceof AnonymousClassDeclaration) {
					isMember= true;
				}


				// If the current class is a member of another class, we need to qualify this
				if (isMember) {
					ThisExpression thisExpression= ast.newThisExpression();

					// Find the outer class
					IMethodBinding invocationBinding= invocation.resolveMethodBinding();
					ITypeBinding outerClassBinding= invocationBinding.getDeclaringClass();
					String outerClassName= outerClassBinding.getName();

					// Qualify this with the name of the outer class
					thisExpression.setQualifier(ast.newSimpleName(outerClassName));
					newArg= thisExpression;
				} else {
					newArg= ast.newThisExpression();
				}
			}
			ListRewrite listRewrite= rewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
			listRewrite.insertFirst(newArg, null);
		}

		SimpleName optionalExpression= ast.newSimpleName(((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().getIdentifier());
		rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, optionalExpression, null);

		TextEdit methodInvocationEdit= rewrite.rewriteAST();
		multiTextEdit.addChild(methodInvocationEdit);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange multiChange= new CompositeChange("Make Static", fChangeManager.getAllChanges()); //$NON-NLS-1$
		return multiChange;
	}

	private void addEditToChangeManager(TextEdit editToAdd, ICompilationUnit iCompilationUnit) {
		//get CompilationUnitChange from ChangeManager, otherwise create one
		CompilationUnitChange compilationUnitChange= (CompilationUnitChange) fChangeManager.get(iCompilationUnit);

		//get all Edits from compilationUnitChange, otherwise create a MultiTextEdit
		MultiTextEdit allTextEdits= (MultiTextEdit) compilationUnitChange.getEdit();
		if (allTextEdits == null) {
			allTextEdits= new MultiTextEdit();
		}
		allTextEdits.addChild(editToAdd);
		String changeName= "Change in " + iCompilationUnit.getElementName(); //$NON-NLS-1$
		CompilationUnitChange newCompilationUnitChange= new CompilationUnitChange(changeName, iCompilationUnit);
		newCompilationUnitChange.setEdit(allTextEdits);
		fChangeManager.manage(iCompilationUnit, newCompilationUnitChange);
	}
}
