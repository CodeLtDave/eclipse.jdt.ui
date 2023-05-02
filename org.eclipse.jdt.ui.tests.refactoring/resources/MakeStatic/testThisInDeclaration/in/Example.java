package package1;

public class Example {

	String j = "";
	
	public String greet(String ending) {
		String i = this.j + ending;
		return i;
	}
	
	public void method() {
		String j = this.greet("David");
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = instance.greet("David");
	}
}