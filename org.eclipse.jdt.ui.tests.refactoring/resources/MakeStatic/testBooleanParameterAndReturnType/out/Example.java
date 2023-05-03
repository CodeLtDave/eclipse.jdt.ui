package package1;

public class Example {

	public static boolean toRefactor(boolean boo) {
		boolean i = boo;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		boolean j = Example.toRefactor(true);
	}
}