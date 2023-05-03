package package1;

public class Example {

	public char toRefactor(char ending) {
		char i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		char j = instance.toRefactor('E');
	}
}