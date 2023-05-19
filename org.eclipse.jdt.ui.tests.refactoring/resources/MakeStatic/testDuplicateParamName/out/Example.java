package package1;

public class Example {

	String j = "";
	
	public static String toRefactor(String example, Example example2) {
		String i = example2.j;
		return i;
	}
	
	public void method() {
		String j = Example.toRefactor("bar", this);
	}
}