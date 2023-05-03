package package1;

public class Example {

	public static double toRefactor(double ending) {
		double i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		double j = Example.toRefactor(Double.MAX_VALUE);
	}
}