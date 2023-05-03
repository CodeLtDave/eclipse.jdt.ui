package package1;

public class Example {

	public static float toRefactor(float ending) {
		float i = ending;
		return i;
	}
	
	public static void foo() {
		Example instance = new Example();
		float j = Example.toRefactor(Float.MAX_VALUE);
	}
}