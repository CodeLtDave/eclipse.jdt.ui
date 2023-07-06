/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *s
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
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
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;

/**
 *
 * The {@code MakeStaticRefactoring} class represents a refactoring operation to convert a method
 * into a static method. It provides the capability to transform an instance method into a static
 * method by modifying the method declaration, updating method invocations, and handling related
 * changes.
 *
 * @since 3.29
 *
 */
public class MakeStaticRefactoring extends Refactoring {

	/**
	 * The {@code IMethod} object representing the selected method on which the refactoring should
	 * be performed.
	 */
	private IMethod fTargetMethod;


	/**
	 * Manages all changes to the source code that will be performed at the end of the refactoring.
	 */
	private TextEditBasedChangeManager fChangeManager;


	/**
	 * Provides all invocations of the refactored method in the workspace.
	 */
	private TargetProvider fTargetProvider;

	/**
	 * The {@code MethodDeclaration} object representing the selected method on which the
	 * refactoring should be performed. This field is used to analyze and modify the method's
	 * declaration during the refactoring process.
	 */
	private MethodDeclaration fTargetMethodDeclaration;

	/**
	 * Indicates whether there is access to instance variables or instance methods within the body
	 * of the method.
	 */
	private boolean fTargetMethodhasInstanceUsage;

	/**
	 * Represents the status of a refactoring operation.
	 */
	private RefactoringStatus fStatus;

	/**
	 * The {@code IMethodBinding} object representing the binding of the refactored method.
	 */
	private IMethodBinding fTargetMethodBinding;

	private ContextCalculator fContextCalculator;

	public MakeStaticRefactoring(ICompilationUnit inputAsICompilationUnit, int selectionStart, int selectionLength) {
		Selection targetSelection= Selection.createFromStartLength(selectionStart, selectionLength);
		fContextCalculator= new ContextCalculator(inputAsICompilationUnit, targetSelection);
		fChangeManager= new TextEditBasedChangeManager();
	}

	/**
	 * Constructs a new {@code MakeStaticRefactoring} object. This constructor is called when
	 * performing the refactoring on a method in the outline menu.
	 *
	 * @param method The target method the refactoring should be performed on.
	 */
	public MakeStaticRefactoring(IMethod method) {
		fContextCalculator= new ContextCalculator(method);
		fChangeManager= new TextEditBasedChangeManager();
	}

	/**
	 * {@inheritDoc}
	 *
	 * Returns the name of the refactoring operation.
	 *
	 * @return The name of the refactoring operation.
	 */
	@Override
	public String getName() {
		return RefactoringCoreMessages.MakeStaticRefactoring_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		fStatus= new RefactoringStatus();

		SelectionInputType selectionInputType= fContextCalculator.getSelectionInputType();

		if (selectionInputType == SelectionInputType.TEXT_SELECTION) {
			fStatus.merge(InitialConditionsChecker.checkTextSelectionStart(fContextCalculator.getSelectionEditorText()));
			fStatus.merge(InitialConditionsChecker.checkValidICompilationUnit(fContextCalculator.getSelectionICompilationUnit()));
			if (fStatus.hasError()) {
				return fStatus;
			}

			fContextCalculator.calculateSelectionASTNode();

			fStatus.merge(InitialConditionsChecker.checkASTNodeIsValidMethod(fContextCalculator.getSelectionASTNode()));
			if (fStatus.hasError()) {
				return fStatus;
			}

			fContextCalculator.calculateTargetIMethodBinding();
			fContextCalculator.calculateTargetIMethod();
		} else {
			//SelectionInputType is IMethod (for example when performing the refactoring form the outline menu)
			fContextCalculator.calculateTargetIMethodBinding();
		}

		fStatus.merge(InitialConditionsChecker.checkSourceAvailable(fContextCalculator.getTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkIMethodIsValid(fContextCalculator.getTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fContextCalculator.calculateTargetICompilationUnit();

		fStatus.merge(InitialConditionsChecker.checkValidICompilationUnit(fContextCalculator.getSelectionICompilationUnit()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodIsNotConstructor(fContextCalculator.getTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodNotInLocalOrAnonymousClass(fContextCalculator.getTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodNotStatic(fContextCalculator.getTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fStatus.merge(InitialConditionsChecker.checkMethodNotOverridden(fContextCalculator.getTargetIMethod()));
		if (fStatus.hasError()) {
			return fStatus;
		}

		fContextCalculator.calculateTargetCompilationUnit();
		fContextCalculator.calculateMethodDeclaration();

		fTargetMethod= fContextCalculator.getTargetIMethod(); //TODO remove those which dont have to necessarily be fields
		fTargetMethodDeclaration= fContextCalculator.getTargetMethodDeclaration();
		fTargetMethodBinding= fContextCalculator.getTargetIMethodBinding();

		return fStatus;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException {
		modifyMethodDeclaration();

		if (fStatus.hasError()) {
			return fStatus;
		}

		//Find and Modify MethodInvocations
		fTargetProvider= TargetProvider.create(fTargetMethodDeclaration);
		fTargetProvider.initialize();

		handleMethodInvocations(progressMonitor);

		return fStatus;
	}

	private void modifyMethodDeclaration() throws JavaModelException {
		AST ast= fTargetMethodDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		addStaticModifierToTargetMethod(rewrite);

		//If fMethodDeclaraion hasInstanceUsages, adding a new parameter will be necessary
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		String paramName= generateUniqueParameterName(className);

		//Change instance Usages ("this" and "super") to paramName and set fHasInstanceUsage flag
		InstanceUsageRewriter instanceUsageRewriter= new InstanceUsageRewriter(paramName, rewrite, ast, fStatus, fTargetMethodDeclaration);
		fTargetMethodDeclaration.getBody().accept(instanceUsageRewriter);
		fTargetMethodhasInstanceUsage= instanceUsageRewriter.fTargetMethodhasInstanceUsage;


		//check if method would unintentionally hide method of parent class
		fStatus.merge(FinalConditionsChecker.checkWouldHideMethodOfParentType(fTargetMethodhasInstanceUsage, fTargetMethod));

		IType parentType= fTargetMethod.getDeclaringType();
		ITypeParameter[] classTypeParameters= parentType.getTypeParameters();

		//Adding an instance parameter to the newly static method to ensure it can still access class-level state and behavior.
		if (fTargetMethodhasInstanceUsage) {
			addInstanceAsParamIfUsed(ast, rewrite, className, paramName, classTypeParameters);
		}

		//Updates typeParamList of MethodDeclaration and inserts new typeParams to JavaDoc
		updateMethodTypeParamList(ast, rewrite, classTypeParameters);

		//A static method can't have override annotations
		deleteOverrideAnnotation(rewrite);

		//Changes can't be applied to directly to AST, edits are saved in fChangeManager
		TextEdit methodDeclarationEdit= rewrite.rewriteAST();
		addEditToChangeManager(methodDeclarationEdit, fTargetMethod.getCompilationUnit());
	}

	private void addStaticModifierToTargetMethod(ASTRewrite rewrite) {
		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, fTargetMethodDeclaration);
		modRewrite.setModifiers(fTargetMethodDeclaration.getModifiers() | Modifier.STATIC, null);
	}

	private String generateUniqueParameterName(String className) {
		List<SingleVariableDeclaration> parameters= fContextCalculator.getTargetMethodInputParameters();
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

	private void addInstanceAsParamIfUsed(AST ast, ASTRewrite rewrite, String className, String paramName,
			ITypeParameter[] classTypeParameters) throws JavaModelException {
		SingleVariableDeclaration newParameter= generateNewParameter(ast, className, paramName, classTypeParameters);
		//While refactoring the method signature might change; ensure the revised method doesn't unintentionally override an existing one.
		fStatus.merge(FinalConditionsChecker.checkDuplicateMethod(fTargetMethodDeclaration, fTargetMethod));

		//Add new parameter to method declaration arguments
		ListRewrite lrw= rewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		lrw.insertFirst(newParameter, null);

		//Changes to fTargetMethodDeclaration's signature need to be adjusted in JavaDocs too
		updateJavaDocs(ast, rewrite, paramName);
	}

	private void updateJavaDocs(AST ast, ASTRewrite rewrite, String paramName) {
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();
		if (javadoc != null) {
			TagElement newParameterTag= ast.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(ast.newSimpleName(paramName));
			ListRewrite tagsRewrite= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertFirst(newParameterTag, null);
		}
	}

	private SingleVariableDeclaration generateNewParameter(AST ast, String className, String paramName, ITypeParameter[] classTypeParameters) {
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
		return newParam;
	}

	private RefactoringStatus updateMethodTypeParamList(AST ast, ASTRewrite rewrite, ITypeParameter[] classTypeParameters) throws JavaModelException {
		ListRewrite typeParamsRewrite= rewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.TYPE_PARAMETERS_PROPERTY);
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();

		List<String> methodParameterTypes= getMethodParameterTypes();
		List<String> methodTypeParametersNames= getTypeParameterNames();

		if (classTypeParameters.length != 0) {
			for (int i= 0; i < classTypeParameters.length; i++) {
				TypeParameter typeParameter= generateTypeParameter(ast, classTypeParameters, i);

				if (fStatus.hasError()) {
					return fStatus;
				}
				//Check if method needs this TypeParameter (only if one or more methodParams are of this type OR method has instance usage OR an instance of parent class is used as methodParam)
				String typeParamName= typeParameter.getName().getIdentifier();
				String typeParamNameAsArray= typeParamName + "[]"; //$NON-NLS-1$
				boolean paramIsNeeded= methodParameterTypes.contains(typeParamName) || methodParameterTypes.contains(typeParamNameAsArray);
				if (fTargetMethodhasInstanceUsage || paramIsNeeded) {
					//only insert if typeParam not already existing
					if (!methodTypeParametersNames.contains(typeParameter.getName().getIdentifier())) {
						typeParamsRewrite.insertLast(typeParameter, null);
						addNewTypeParamsToJavaDoc(ast, rewrite, javadoc, typeParameter);
					}
				}
			}
		}
		return fStatus;
	}

	private TypeParameter generateTypeParameter(AST ast, ITypeParameter[] classTypeParameters, int i) throws JavaModelException {
		String[] bounds= classTypeParameters[i].getBounds();
		TypeParameter typeParameter= ast.newTypeParameter();
		typeParameter.setName(ast.newSimpleName(classTypeParameters[i].getElementName()));
		for (String bound : bounds) {
			//WildCardTypes are not allowed as bounds
			fStatus.merge(FinalConditionsChecker.checkBoundContainsWildCardType(bound));
			if (!fStatus.hasError()) {
				SimpleType boundType= ast.newSimpleType(ast.newSimpleName(bound));
				typeParameter.typeBounds().add(boundType);
			}
		}
		return typeParameter;
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

	private void handleMethodInvocations(IProgressMonitor progressMonitor) throws CoreException {
		ICompilationUnit[] affectedICompilationUnits= fTargetProvider.getAffectedCompilationUnits(fStatus, new ReferencesInBinaryContext(""), progressMonitor); //$NON-NLS-1$
		for (ICompilationUnit affectedICompilationUnit : affectedICompilationUnits) {

			//Check if MethodReferences use selected method -> cancel refactoring
			CompilationUnit affectedCompilationUnit= convertICUtoCU(affectedICompilationUnit);
			affectedCompilationUnit.accept(new MethodReferenceFinder(fStatus, fTargetMethodBinding));

			if (fStatus.hasFatalError()) {
				return;
			}

			BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(affectedICompilationUnit, null);
			MultiTextEdit multiTextEdit= new MultiTextEdit();
			for (BodyDeclaration body : bodies) {
				ASTNode[] invocations= fTargetProvider.getInvocations(body, null);
				for (ASTNode invocationASTNode : invocations) {
					MethodInvocation invocation= (MethodInvocation) invocationASTNode;
					modifyMethodInvocation(multiTextEdit, invocation);
				}
			}

			if (fStatus.hasFatalError()) {
				return;
			}

			addEditToChangeManager(multiTextEdit, affectedICompilationUnit);
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

	private void modifyMethodInvocation(MultiTextEdit multiTextEdit, MethodInvocation invocation) throws JavaModelException {
		AST ast= invocation.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		if (fTargetMethodhasInstanceUsage) {
			ASTNode newArg;
			if (invocation.getExpression() != null) {
				newArg= ASTNode.copySubtree(ast, invocation.getExpression()); // copy the expression
			} else {
				// We need to find the class that this invocation is inside
				ASTNode parent= findParentClass(invocation);
				boolean isMember= isMember(parent);

				// If the current class is a member of another class, we need to qualify this
				if (isMember) {
					newArg= qualifyThisExpression(invocation, ast);
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

	private ASTNode qualifyThisExpression(MethodInvocation invocation, AST ast) {
		ASTNode newArg;
		ThisExpression thisExpression= ast.newThisExpression();

		// Find the outer class
		IMethodBinding invocationBinding= invocation.resolveMethodBinding();
		ITypeBinding outerClassBinding= invocationBinding.getDeclaringClass();
		String outerClassName= outerClassBinding.getName();

		// Qualify this with the name of the outer class
		thisExpression.setQualifier(ast.newSimpleName(outerClassName));
		newArg= thisExpression;
		return newArg;
	}

	private boolean isMember(ASTNode parent) {
		boolean isMember= false;
		if (parent instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration currentClass= (AbstractTypeDeclaration) parent;
			if (currentClass.isMemberTypeDeclaration()) {
				isMember= true;
			}
		} else if (parent instanceof AnonymousClassDeclaration) {
			isMember= true;
		}
		return isMember;
	}

	private ASTNode findParentClass(MethodInvocation invocation) {
		ASTNode parent= invocation;
		while ((!(parent instanceof AbstractTypeDeclaration)) && (!(parent instanceof AnonymousClassDeclaration))) {
			parent= parent.getParent();
		}
		return parent;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Creates a {@code Change} object representing the refactoring changes to be performed. This
	 * method constructs a composite change that encapsulates all the individual changes managed by
	 * the {@code fChangeManager}.
	 *
	 * @param progressMonitor The progress monitor used to track the progress of the operation.
	 * @return A {@code Change} object representing all refactoring changes.
	 * @throws CoreException If an error occurs during the creation of the change.
	 * @throws OperationCanceledException If the operation is canceled by the user.
	 */
	@Override
	public Change createChange(IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException {
		CompositeChange multiChange= new CompositeChange(RefactoringCoreMessages.MakeStaticRefactoring_name, fChangeManager.getAllChanges());
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
