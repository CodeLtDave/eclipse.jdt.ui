package package1;

public class Example {

	String j = "";
	
	public static String toRefactor(String ending, Example example) {
		String i = example.j + ending;
		i = example.j + ending;
		return i;
	}
	
	public void method() {
		String j = Example.toRefactor("bar", this);
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = Example.toRefactor("bar", instance);
	}
}