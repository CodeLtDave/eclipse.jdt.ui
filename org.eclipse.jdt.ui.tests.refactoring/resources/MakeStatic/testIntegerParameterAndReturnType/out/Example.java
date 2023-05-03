package package1;

public class Example {

	public static int toRefactor(int ending) {
		int i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		int i = Example.toRefactor(2);
	}
}