package package1;

public class Example {

	String j = "";
	
	public String toRefactor(String foo) {
		String i = this.j;
		return i;
	}
	
	public static String toRefactor(String foo, Example example) {
		String i = example.j;
		return i;
	}
}