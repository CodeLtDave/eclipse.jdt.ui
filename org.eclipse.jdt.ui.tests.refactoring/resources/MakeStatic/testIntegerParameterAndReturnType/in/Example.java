package package1;

public class Example {

	public int toRefactor(int ending) {
		int i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		int i = instance.toRefactor(2);
	}
}