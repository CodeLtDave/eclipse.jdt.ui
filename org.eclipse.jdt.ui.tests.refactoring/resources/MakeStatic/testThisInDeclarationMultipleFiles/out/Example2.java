package package2;

import package1.Example;

public class Example2 {
	
	public void method() {
		Example instance = new Example();
		String j = Example.toRefactor("bar", instance);
	}
	
	public static void staticMethod() {
		Example instance = new Example();
		String j = Example.toRefactor("bar", instance);
	}
}