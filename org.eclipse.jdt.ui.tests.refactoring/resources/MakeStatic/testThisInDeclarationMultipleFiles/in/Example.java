package package1;

public class Example {

	String j = "";
	
	public String toRefactor(String ending) {
		String i = this.j + ending;
		return i;
	}
	
	public void method() {
		String j = this.toRefactor("bar");
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = instance.toRefactor("bar");
	}
}