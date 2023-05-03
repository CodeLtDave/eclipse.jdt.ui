package package1;

public class Example {

	public static String toRefactor(String ending) {
		String i = "hallo" + ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		String j = Example.toRefactor("bar");
	}
}