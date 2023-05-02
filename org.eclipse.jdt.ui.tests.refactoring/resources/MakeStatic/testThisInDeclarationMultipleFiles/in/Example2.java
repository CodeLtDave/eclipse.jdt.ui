package package2;

import package1.Example;

public class Example2 {

	public void method() {
		Example instance = new Example();
		String j = instance.greet("David");
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = instance.greet("David");
	}
}