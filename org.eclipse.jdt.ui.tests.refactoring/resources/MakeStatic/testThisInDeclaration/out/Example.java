package package1;

public class Example {

	String j = "";
	
	public static String greet(String ending, Example example) {
		String i = example.j + ending;
		return i;
	}
	
	public void method() {
		String j = Example.greet("David", this);
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = Example.greet("David", instance);
	}
}