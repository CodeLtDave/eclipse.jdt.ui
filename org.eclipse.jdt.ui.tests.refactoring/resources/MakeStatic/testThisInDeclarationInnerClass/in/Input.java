package p;

public class Input {
	private int counter;

	private InnerClass inner;

	public void toRefactor() {
		this.counter= this.counter + 1;
		this.instanceMethod();
		this.inner.printHello();
	}

	public void instanceMethod() {
		this.toRefactor();
	}

	public static void staticMethod() {
		Input foo= new Input();
		foo.toRefactor();
	}

	private class InnerClass {
		public void printHello() {
			System.out.println("Hello from InnerClass!");
		}
	}
}
