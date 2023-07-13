package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;

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
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.code.TargetProvider;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;

public class ChangeCalculator {

	/**
	 * Indicates whether there is access to instance variables or instance methods within the body
	 * of the method.
	 */
	private boolean fTargetMethodhasInstanceUsage;

	private RefactoringStatus fStatus;

	private MethodDeclaration fTargetMethodDeclaration;

	private IMethod fTargetMethod;

	/**
	 * Manages all changes to the source code that will be performed at the end of the refactoring.
	 */
	private TextEditBasedChangeManager fChangeManager;

	private TargetProvider fTargetProvider;

	private IMethodBinding fTargetMethodBinding;

	private AST fTargetMethodDeclarationAST;

	private ASTRewrite fTargetMethodDeclarationASTRewrite;

	private String fparameterName;

	public ChangeCalculator(MethodDeclaration targetMethodDeclaration, IMethod targetMethod, TargetProvider targetprovider, IMethodBinding targetMethodBinding) {
		fTargetMethodDeclaration= targetMethodDeclaration;
		fTargetMethod= targetMethod;
		fTargetProvider= targetprovider;
		fTargetMethodBinding= targetMethodBinding;
		fTargetMethodDeclarationAST= fTargetMethodDeclaration.getAST();
		fTargetMethodDeclarationASTRewrite= ASTRewrite.create(fTargetMethodDeclarationAST);
		fparameterName= generateUniqueParameterName();
		fChangeManager= new TextEditBasedChangeManager();
		fStatus= new RefactoringStatus();
	}

	public TextEditBasedChange[] getChanges() {
		return fChangeManager.getAllChanges();
	}

	public boolean getTargetMethodhasInstanceUsage() {
		return fTargetMethodhasInstanceUsage;
	}

	public RefactoringStatus modifyMethodDeclaration() throws JavaModelException {


		//Changes can't be applied to directly to AST, edits are saved in fChangeManager
		TextEdit methodDeclarationEdit= fTargetMethodDeclarationASTRewrite.rewriteAST();
		addEditToChangeManager(methodDeclarationEdit, fTargetMethod.getCompilationUnit());

		return fStatus;
	}

	public void rewriteInstanceUsages() {
		//Change instance Usages ("this" and "super") to paramName and set fTargetMethodhasInstanceUsage flag
		InstanceUsageRewriter instanceUsageRewriter= new InstanceUsageRewriter(fparameterName, fTargetMethodDeclarationASTRewrite, fTargetMethodDeclarationAST, fStatus, fTargetMethodDeclaration);
		fTargetMethodDeclaration.getBody().accept(instanceUsageRewriter);
		fTargetMethodhasInstanceUsage= instanceUsageRewriter.fTargetMethodhasInstanceUsage;
	}

	public void addStaticModifierToTargetMethod() {
		ModifierRewrite modRewrite= ModifierRewrite.create(fTargetMethodDeclarationASTRewrite, fTargetMethodDeclaration);
		modRewrite.setModifiers(fTargetMethodDeclaration.getModifiers() | Modifier.STATIC, null);
	}

	private String generateUniqueParameterName() {
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		List<SingleVariableDeclaration> parameters= fTargetMethodDeclaration.parameters();
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

	public void addInstanceAsParamIfUsed() throws JavaModelException {
		//Add new parameter to method declaration arguments
		ListRewrite lrw= fTargetMethodDeclarationASTRewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		lrw.insertFirst(generateNewParameter(), null);
		//Changes to fTargetMethodDeclaration's signature need to be adjusted in JavaDocs too
		updateJavaDocs();
	}

	private void updateJavaDocs() {
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();
		if (javadoc != null) {
			TagElement newParameterTag= fTargetMethodDeclarationAST.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(fTargetMethodDeclarationAST.newSimpleName(fparameterName));
			ListRewrite tagsRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertFirst(newParameterTag, null);
		}
	}

	private SingleVariableDeclaration generateNewParameter() throws JavaModelException {
		String className= ((TypeDeclaration) fTargetMethodDeclaration.getParent()).getName().toString();
		IType parentType= fTargetMethod.getDeclaringType();
		ITypeParameter[] classTypeParameters= parentType.getTypeParameters();
		SingleVariableDeclaration newParam= fTargetMethodDeclarationAST.newSingleVariableDeclaration();

		//If generic TypeParameters exist in class the newParam type needs to be parameterized
		if (classTypeParameters.length != 0) {
			SimpleType simpleType= fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newName(className));
			ParameterizedType parameterizedType= fTargetMethodDeclarationAST.newParameterizedType(simpleType);
			for (int i= 0; i < classTypeParameters.length; i++) {
				SimpleType typeParameter= fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newSimpleName(classTypeParameters[i].getElementName()));
				parameterizedType.typeArguments().add(typeParameter);
			}
			newParam.setType(parameterizedType);
		} else {
			newParam.setType(fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newName(className)));
		}
		newParam.setName(fTargetMethodDeclarationAST.newSimpleName(fparameterName));
		return newParam;
	}

	public RefactoringStatus updateMethodTypeParamList() throws JavaModelException {
		IType parentType= fTargetMethod.getDeclaringType();
		ITypeParameter[] classTypeParameters= parentType.getTypeParameters();
		ListRewrite typeParamsRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.TYPE_PARAMETERS_PROPERTY);
		Javadoc javadoc= fTargetMethodDeclaration.getJavadoc();
		List<String> methodParameterTypes= getMethodParameterTypes();
		List<String> methodTypeParametersNames= getTypeParameterNames();

		if (classTypeParameters.length != 0) {
			for (int i= 0; i < classTypeParameters.length; i++) {
				TypeParameter typeParameter= generateTypeParameter(classTypeParameters, i);

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
						addNewTypeParamsToJavaDoc(javadoc, typeParameter);
					}
				}
			}
		}
		return fStatus;
	}

	private TypeParameter generateTypeParameter(ITypeParameter[] classTypeParameters, int i) throws JavaModelException {
		String[] bounds= classTypeParameters[i].getBounds();
		TypeParameter typeParameter= fTargetMethodDeclarationAST.newTypeParameter();
		typeParameter.setName(fTargetMethodDeclarationAST.newSimpleName(classTypeParameters[i].getElementName()));
		for (String bound : bounds) {
			//WildCardTypes are not allowed as bounds
			fStatus.merge(FinalConditionsChecker.checkBoundNotContainingWildCardType(bound));
			if (!fStatus.hasError()) {
				SimpleType boundType= fTargetMethodDeclarationAST.newSimpleType(fTargetMethodDeclarationAST.newSimpleName(bound));
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

	private void addNewTypeParamsToJavaDoc(Javadoc javadoc, TypeParameter typeParameter) {
		if (javadoc != null) {
			//add new type params to javaDoc
			TextElement textElement= fTargetMethodDeclarationAST.newTextElement();
			textElement.setText("<" + typeParameter.getName().getIdentifier() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			TagElement newParameterTag= fTargetMethodDeclarationAST.newTagElement();
			newParameterTag.setTagName(TagElement.TAG_PARAM);
			newParameterTag.fragments().add(textElement);
			ListRewrite tagsRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			tagsRewrite.insertLast(newParameterTag, null);
		}
	}

	public void deleteOverrideAnnotation() {
		ListRewrite listRewrite= fTargetMethodDeclarationASTRewrite.getListRewrite(fTargetMethodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		for (Object obj : fTargetMethodDeclaration.modifiers()) {
			if (obj instanceof org.eclipse.jdt.core.dom.MarkerAnnotation markerAnnotation) {
				if (markerAnnotation.getTypeName().getFullyQualifiedName().equals("Override")) { //$NON-NLS-1$
					listRewrite.remove(markerAnnotation, null);
				}
			}
		}
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

	public RefactoringStatus handleMethodInvocations(IProgressMonitor progressMonitor) throws CoreException {
		ICompilationUnit[] affectedICompilationUnits= fTargetProvider.getAffectedCompilationUnits(fStatus, new ReferencesInBinaryContext(""), progressMonitor); //$NON-NLS-1$
		for (ICompilationUnit affectedICompilationUnit : affectedICompilationUnits) {

			//Check if MethodReferences use selected method -> cancel refactoring
			CompilationUnit affectedCompilationUnit= convertICUtoCU(affectedICompilationUnit);
			affectedCompilationUnit.accept(new MethodReferenceFinder(fStatus, fTargetMethodBinding));

			if (fStatus.hasFatalError()) {
				return fStatus;
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
				return fStatus;
			}

			addEditToChangeManager(multiTextEdit, affectedICompilationUnit);
		}
		return fStatus;
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
}
