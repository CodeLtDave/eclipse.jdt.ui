package p;

public class Input {
	private int counter;

	private InnerClass inner;

	public static void toRefactor(Input input) {
		input.counter= input.counter + 1;
		input.instanceMethod();
		input.printHello();
	}

	public void instanceMethod() {
		Input.toRefactor(this);
	}

	public static void staticMethod() {
		Input foo= new Input();
		Input.toRefactor(foo);
	}

	private class InnerClass {
		public void printHello() {
			System.out.println("Hello from InnerClass!");
		}
	}
}
