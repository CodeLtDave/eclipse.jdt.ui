package package1;

public class Example {

	String j = "";
	
	public String toRefactor(String example) {
		String i = this.j;
		return i;
	}
	
	public void method() {
		String j = this.toRefactor("bar");
	}
}