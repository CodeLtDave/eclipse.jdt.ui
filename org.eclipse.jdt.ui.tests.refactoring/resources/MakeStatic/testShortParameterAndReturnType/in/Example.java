package package1;

public class Example {

	public short toRefactor(short ending) {
		short i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		short j = instance.toRefactor(Short.MAX_VALUE);
	}
}