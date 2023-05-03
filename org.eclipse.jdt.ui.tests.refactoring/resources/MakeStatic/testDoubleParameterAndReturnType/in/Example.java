package package1;

public class Example {

	public double toRefactor(double ending) {
		double i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		double j = instance.toRefactor(Double.MAX_VALUE);
	}
}