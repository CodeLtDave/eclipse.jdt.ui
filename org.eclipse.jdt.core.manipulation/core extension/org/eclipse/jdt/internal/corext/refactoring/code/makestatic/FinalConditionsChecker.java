package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

class FinalConditionsChecker {
	public static RefactoringStatus checkDuplicateMethod(MethodDeclaration methodDeclaration, IMethod imethod) throws JavaModelException {
		RefactoringStatus status= new RefactoringStatus();
		int parameterAmount= methodDeclaration.parameters().size() + 1;
		String methodName= methodDeclaration.getName().getIdentifier();
		IMethodBinding methodBinding= methodDeclaration.resolveBinding();
		ITypeBinding typeBinding= methodBinding.getDeclaringClass();
		IType type= (IType) typeBinding.getJavaElement();

		IMethod method= Checks.findMethod(methodName, parameterAmount, false, type);

		if (method == null) {
			return new RefactoringStatus();
		}

		//check if parameter types match (also compare new parameter that is added by refactoring)
		String className= ((TypeDeclaration) methodDeclaration.getParent()).getName().toString();
		String extendedClassName= "Q" + className + ";"; //$NON-NLS-1$ //$NON-NLS-2$
		boolean contains;
		String[] paramTypesOfFoundMethod= method.getParameterTypes();
		String[] paramTypesOfSelectedMethodExtended= new String[parameterAmount];
		paramTypesOfSelectedMethodExtended[0]= extendedClassName;
		String[] paramTypesOfSelectedMethod= imethod.getParameterTypes();

		for (int i= 0; i < paramTypesOfSelectedMethod.length; i++) {
			paramTypesOfSelectedMethodExtended[i + 1]= paramTypesOfSelectedMethod[i];
		}

		for (int i= 0; i < paramTypesOfFoundMethod.length; i++) {
			contains= paramTypesOfSelectedMethodExtended[i].equals(paramTypesOfFoundMethod[i]);
			if (!contains) {
				return status;
			}
		}
		status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_duplicate_method_signature));
		return status;
	}

	public static RefactoringStatus checkWouldHideMethodOfParentType(boolean methodhasInstanceUsage, IMethod iMethod) {
		RefactoringStatus status= new RefactoringStatus();
		try {
			if (!methodhasInstanceUsage && isOverriding(iMethod.getDeclaringType(), iMethod)) {
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_hiding_method_of_parent_type));
			}
		} catch (JavaModelException e) {
			System.out.println("iMethod.getDeclaringType(): " + iMethod.getDeclaringType() + " does not exist or an exception occured while accessing its corresponding resource"); //$NON-NLS-1$//$NON-NLS-2$
			e.printStackTrace();
		}
		return status;
	}

	public static RefactoringStatus checkBoundContainsWildCardType(String bound) {
		RefactoringStatus status= new RefactoringStatus();
		if (bound.contains("?")) { //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MakeStaticRefactoring_not_available_for_wildCardTypes_as_bound));
		}
		return status;
	}

	private static boolean isOverriding(IType type, IMethod iMethod) throws JavaModelException { //TODO duplicate isOverriding()?
		ITypeHierarchy hierarchy= type.newTypeHierarchy(null);
		IType[] supertypes= hierarchy.getAllSupertypes(type);
		for (IType supertype : supertypes) {
			IMethod[] methods= supertype.getMethods();
			for (IMethod method : methods) {
				if (method.isSimilar(iMethod)) {
					return true;
				}
			}
		}
		return false;
	}


}
