package org.eclipse.jdt.ui.tests.astrewrite;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

public class ASTRewritingMoveCodeTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingMoveCodeTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingMoveCodeTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), THIS, args);
	}


	public static Test suite() {
		return new TestSuite(THIS);
//		TestSuite suite= new TestSuite();
//		suite.addTest(new ASTRewritingMoveCodeTest("testMoveDeclDifferentLevel"));
//		return suite;
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	

	
	public void testMoveDeclSameLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);	
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move inner type to the end of the type & move, copy statements from constructor to method
			
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			ASTRewriteAnalyzer.markAsRemoved(innerType);
			ASTNode movedNode= ASTRewriteAnalyzer.createCopyTarget(innerType);
			members.add(movedNode);
			
			Statement toMove;
			Statement toCopy;
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Not expected number of statements", statements.size() == 4);
				
				toMove= (Statement) statements.get(1);
				toCopy= (Statement) statements.get(3);
				
				ASTRewriteAnalyzer.markAsRemoved(toMove);
			}
			{
				MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
				assertTrue("Cannot find gee()", methodDecl != null);
				Block body= methodDecl.getBody();
				assertTrue("No body", body != null);
				List statements= body.statements();
				assertTrue("Has statements", statements.isEmpty());
				
				ASTNode insertNodeForMove= ASTRewriteAnalyzer.createCopyTarget(toMove);
				ASTNode insertNodeForCopy= ASTRewriteAnalyzer.createCopyTarget(toCopy);
				
				statements.add(insertNodeForCopy);
				statements.add(insertNodeForMove);
			}	
		}			
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");		
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");		
		buf.append("        i= 0;\n");
		buf.append("    }\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");			
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");		
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testMoveDeclDifferentLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{
			List members= type.bodyDeclarations();
			assertTrue("Has declarations", !members.isEmpty());
			
			assertTrue("Cannot find inner class", members.get(0) instanceof TypeDeclaration);
			TypeDeclaration innerType= (TypeDeclaration) members.get(0);
			
			List innerMembers= innerType.bodyDeclarations();
			assertTrue("Not expected number of inner members", innerMembers.size() == 1);
			
			{ // move outer as inner of inner.
				TypeDeclaration outerType= findTypeDeclaration(astRoot, "G");
				assertTrue("G not found", outerType != null);
				
				ASTRewriteAnalyzer.markAsRemoved(outerType);
				
				ASTNode insertNodeForCopy= ASTRewriteAnalyzer.createCopyTarget(outerType);
				innerMembers.add(insertNodeForCopy);
			}
			{ // copy method of inner to main type
				MethodDeclaration methodDecl= (MethodDeclaration) innerMembers.get(0);
				ASTNode insertNodeForMove= ASTRewriteAnalyzer.createCopyTarget(methodDecl);
				members.add(insertNodeForMove);
			}
			{ // nest body of constructor in a while statement
				MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
				assertTrue("Cannot find Constructor E", methodDecl != null);

				Block body= methodDecl.getBody();

				WhileStatement whileStatement= ast.newWhileStatement();
				whileStatement.setExpression(ast.newBooleanLiteral(true));
				
				Statement insertNodeForCopy= (Statement) ASTRewriteAnalyzer.createCopyTarget(body);
				
				whileStatement.setBody(insertNodeForCopy); // set existing body

				Block newBody= ast.newBlock();
				List newStatements= newBody.statements();				
				newStatements.add(whileStatement);
				
				ASTRewriteAnalyzer.markAsReplaced(body, newBody);
			}			
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");			
		buf.append("        interface G {\n");
		buf.append("        }\n");				
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        while (true) {\n");		
		buf.append("            super();\n");
		buf.append("            i= 0;\n");
		buf.append("            k= 9;\n");
		buf.append("            if (System.out == null) {\n");
		buf.append("                gee(); // cool\n");
		buf.append("            }\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("    public void xee() {\n");
		buf.append("        /* does nothing */\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testMoveStatements() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        i= 0;\n");
		buf.append("        k= 9;\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // move first statments inside an ifstatement, move second statment inside a new while statement
		   // that is in the ifstatement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "E");
			assertTrue("Cannot find Constructor E", methodDecl != null);

			Block body= methodDecl.getBody();
			List statements= body.statements();
			
			assertTrue("Cannot find if statement", statements.get(3) instanceof IfStatement);
			
			IfStatement ifStatement= (IfStatement) statements.get(3);
			
			Statement insertNodeForCopy1= (Statement) ASTRewriteAnalyzer.createCopyTarget((ASTNode) statements.get(1));
			Statement insertNodeForCopy2= (Statement) ASTRewriteAnalyzer.createCopyTarget((ASTNode) statements.get(2));
			
			Block whileBody= ast.newBlock();
			
			WhileStatement whileStatement= ast.newWhileStatement();
			whileStatement.setExpression(ast.newBooleanLiteral(true));
			whileStatement.setBody(whileBody);
			
			List whileBodyStatements= whileBody.statements();
			whileBodyStatements.add(insertNodeForCopy2);
			
			
			assertTrue("if statement body not a block", ifStatement.getThenStatement() instanceof Block);
			
			List ifBodyStatements= ((Block)ifStatement.getThenStatement()).statements();
			
			ifBodyStatements.add(0, whileStatement);
			ifBodyStatements.add(1, insertNodeForCopy1);
			
			ASTRewriteAnalyzer.markAsInserted(whileStatement);
			
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) statements.get(1));
			ASTRewriteAnalyzer.markAsRemoved((ASTNode) statements.get(2));
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends Exception implements Runnable, Serializable {\n");
		buf.append("    public static class EInner {\n");
		buf.append("        public void xee() {\n");
		buf.append("            /* does nothing */\n");
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    private /* inner comment */ int i;\n");
		buf.append("    private int k;\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            while (true) {\n");
		buf.append("                k= 9;\n");
		buf.append("            }\n");		
		buf.append("            i= 0;\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void gee() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface G {\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void tesCopyFromDeleted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // delete method foo, but copy if statement to goo
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			ASTRewriteAnalyzer.markAsRemoved(methodDecl);
			
			Block body= methodDecl.getBody();
			List statements= body.statements();
			assertTrue("Cannot find if statement", statements.size() == 1);
			
			ASTNode placeHolder= ASTRewriteAnalyzer.createCopyTarget((ASTNode) statements.get(0));
			
			
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			methodGoo.getBody().statements().add(placeHolder);
		}	
					

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee(); // cool\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}
	
	public void testChangesInMove() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee( /* cool */);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("        x= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // replace statement in goo with moved ifStatement from foo. add a node to if statement
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			List fooStatements= methodDecl.getBody().statements();
			assertTrue("Cannot find if statement", fooStatements.size() == 1);
			
			// prepare ifStatement to move
			IfStatement ifStatement= (IfStatement) fooStatements.get(0);
			ASTRewriteAnalyzer.markAsRemoved(ifStatement);
			
			ASTNode placeHolder= ASTRewriteAnalyzer.createCopyTarget(ifStatement);
			
			// add return statement to ifStatement block
			ReturnStatement returnStatement= ast.newReturnStatement();
			ASTRewriteAnalyzer.markAsInserted(returnStatement);

			Block then= (Block) ifStatement.getThenStatement();
			then.statements().add(returnStatement);
	
			// replace statement in goo with moved ifStatement
			MethodDeclaration methodGoo= findMethodDeclaration(type, "goo");
			assertTrue("Cannot find goo", methodGoo != null);
			
			List gooStatements= methodGoo.getBody().statements();
			assertTrue("Cannot find statement in goo", gooStatements.size() == 1);
			
			ASTRewriteAnalyzer.markAsReplaced((ASTNode) gooStatements.get(0), placeHolder);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    public void goo() {\n");
		buf.append("        if (System.out == null) {\n");
		buf.append("            gee( /* cool */);\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}

	public void testSwap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(xoo(/*hello*/), k * 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		assertTrue("Code has errors", (astRoot.getFlags() & astRoot.MALFORMED) == 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		
		{ // swap the two arguments
			MethodDeclaration methodDecl= findMethodDeclaration(type, "foo");
			assertTrue("Cannot find foo", methodDecl != null);
			
			List fooStatements= methodDecl.getBody().statements();
			assertTrue("More statements than expected", fooStatements.size() == 1);
			
			ExpressionStatement statement= (ExpressionStatement) fooStatements.get(0);
			MethodInvocation invocation= (MethodInvocation) statement.getExpression();
			
			List arguments= invocation.arguments();
			assertTrue("More arguments than expected", arguments.size() == 2);
			
			ASTNode arg0= (ASTNode) arguments.get(0);
			ASTNode arg1= (ASTNode) arguments.get(1);
			
			ASTNode placeHolder0= ASTRewriteAnalyzer.createCopyTarget(arg0);
			ASTNode placeHolder1= ASTRewriteAnalyzer.createCopyTarget(arg1);
			
			ASTRewriteAnalyzer.markAsReplaced(arg0, placeHolder1);
			ASTRewriteAnalyzer.markAsReplaced(arg1, placeHolder0);
		}	
					
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, astRoot, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo(k * 2, xoo(/*hello*/));\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(cu.getSource(), buf.toString());
	}	
	
}
