package package1;

public class Example {

	String j = "";
	
	public static String toRefactor(Example example, String ending) {
		String i = example.j + ending;
		return i;
	}
	
	public void method() {
		String j = Example.toRefactor(this, "bar");
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = Example.toRefactor(instance, "bar");
	}
}