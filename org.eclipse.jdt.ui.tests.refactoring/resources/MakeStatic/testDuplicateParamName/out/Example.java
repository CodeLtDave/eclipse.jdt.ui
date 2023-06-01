package package1;

public class Example {

	String j= "";

	public static String toRefactor(Example example2, String example) {
		String i= example2.j;
		return i;
	}

	public void method() {
		String j= Example.toRefactor(this, "bar");
	}
}
