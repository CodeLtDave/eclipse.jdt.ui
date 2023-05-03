package package1;

public class Example {

	public String toRefactor(String ending) {
		String i = "bar" + ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		String j = instance.toRefactor("foo");
	}
}