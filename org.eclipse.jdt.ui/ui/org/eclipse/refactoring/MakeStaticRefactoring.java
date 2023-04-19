package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;


public class MakeStaticRefactoring extends Refactoring {

	private IMethod fMethod;

	private CompilationUnitChange fChange;

	private CompilationUnitRewrite fBaseCuRewrite;

	private ICompilationUnit fCUnit;

	private Object fBodyUpdater;

	private CompilationUnitRewrite cuRewrite;

	private TextChangeManager fChangeManager;

	public MakeStaticRefactoring(IMethod method, ICompilationUnit inputAsCompilationUnit, int offset, int length) {
		fMethod= method;
		fCUnit= inputAsCompilationUnit;
	}

	@Override
	public String getName() {
		return "Make static"; //$NON-NLS-1$
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		ICompilationUnit compilationUnit= fMethod.getCompilationUnit();

		MethodDeclaration methodDeclaration = findMethodDeclaration(fMethod);
		List<IJavaElement> invocationList = findMethodInvocations(fMethod);

		fBaseCuRewrite= new CompilationUnitRewrite(compilationUnit);

		AST ast = methodDeclaration.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		ModifierRewrite modRewrite= ModifierRewrite.create(rewrite, methodDeclaration);
		modRewrite.setModifiers(methodDeclaration.getModifiers() | Modifier.STATIC, null);

//		//Ausprobieren
		TextEdit referenceEdit = updateMethodInvocations(fMethod, invocationList);
		TextEdit textEdit= rewrite.rewriteAST();

		MultiTextEdit multiTextEdit = new MultiTextEdit();
		multiTextEdit.addChild(referenceEdit);
		multiTextEdit.addChild(textEdit);

//		TextEdit textEdit= rewrite.rewriteAST();
		fChange = new CompilationUnitChange("Test",compilationUnit); //$NON-NLS-1$
	    fChange.setEdit(multiTextEdit);

		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return fChange;
	}

	private MethodDeclaration findMethodDeclaration(IMethod method) {
		ICompilationUnit compilationUnit= method.getCompilationUnit();

		// Create AST parser with Java 17 support
		ASTParser parser= ASTParser.newParser(AST.JLS17);
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
				if (resolvedMethod.equals(method)) {
					// Found the MethodDeclaration for the given IMethod
					return true;
				}
				return super.visit(node);
			}
		});

		// Get the resolved MethodDeclaration
		MethodDeclaration methodDeclaration= (MethodDeclaration) astRoot.findDeclaringNode(method.getKey());

		return methodDeclaration;
	}

	public static List<IJavaElement> findMethodInvocations(IMethod method) {
        List<IJavaElement> invocationList = new ArrayList<>();
        try {
            // Found the method, now search for method invocations
            SearchPattern pattern = SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES);
            SearchEngine engine = new SearchEngine();
            SearchRequestor requestor = new SearchRequestor() {
                @Override
                public void acceptSearchMatch(SearchMatch match) throws CoreException {
                    // Handle each method invocation match
                    IJavaElement element = (IJavaElement) match.getElement();
                    invocationList.add(element);
                }
            };
            IJavaProject javaProject = method.getJavaProject();
            IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
            engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope,
                    requestor, null);
        } catch (JavaModelException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return invocationList;
    }

	//Ausprobieren
	public static TextEdit updateMethodInvocations(IMethod method, List<IJavaElement> methodInvocationElements) throws JavaModelException, IllegalArgumentException {
		TextEdit textEdit = null;
        for (IJavaElement element : methodInvocationElements) {
            if (element instanceof IMethod) {
                MethodInvocation invocation = createMethodInvocation((IMethod) element);
                if (invocation != null) {
                    // Create AST and ASTRewrite
                    AST ast = invocation.getAST();
                    ASTRewrite rewrite = ASTRewrite.create(ast);

                    // Update the method invocation to call the static method
                    MethodInvocationVisitor visitor = new MethodInvocationVisitor(method.getElementName(), ast);
                    invocation.accept(visitor);
                    MethodInvocation methodInvocation = visitor.getMethodInvocation();
                    if (methodInvocation != null) {
                        rewrite.replace(invocation, methodInvocation, null);
                    }

                    textEdit= rewrite.rewriteAST();


//                    String source = element.getOpenable().getBuffer().getContents();
//                    String updatedSource = applyChanges(source, rewrite);
//                    if (updatedSource != null) {
//                        // Update the source code
//                        // e.g., by setting the updatedSource to the buffer of the IJavaElement
//                    }
                }
            }
        }
        return textEdit;
    }

    private static MethodInvocation createMethodInvocation(IMethod method) {
        try {
            ASTParser parser = ASTParser.newParser(AST.JLS17);
            parser.setSource(method.getCompilationUnit());
            CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
            MethodInvocationVisitor visitor = new MethodInvocationVisitor(method.getElementName(), compilationUnit.getAST());
            compilationUnit.accept(visitor);
            return visitor.getMethodInvocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class MethodInvocationVisitor extends ASTVisitor {
        private String methodName;
        private AST ast;
        private MethodInvocation methodInvocation;

        public MethodInvocationVisitor(String methodName, AST ast) {
            this.methodName = methodName;
            this.ast = ast;
        }

        public MethodInvocation getMethodInvocation() {
            return methodInvocation;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (node.getName().getIdentifier().equals(methodName)) {
                methodInvocation = (MethodInvocation) ASTNode.copySubtree(ast, node);
            }
            return true;
        }
    }
}

