package package1;

public class Example {

	public String toRefactor(String ending) {
		String i = "hallo" + ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		String j = instance.toRefactor("bar");
	}
}