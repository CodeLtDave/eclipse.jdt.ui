package package1;

public class SubClass extends SuperClass {
	private int counter;

	private InnerClass inner;

	private SubClass instanceObject;

	private static InnerClass staticInner;

	private static int staticInt= 0;

	public void toRefactor() {
		super.intParent++;
		super.instanceParent(1);
		this.instanceObject.counter++;
		intParent= staticInt + 1;
		int localInt= 0;
		this.counter++;
		counter++;
		this.counter= this.counter + 1;
		this.instanceMethod(1);
		instanceMethod(1);
		staticMethod();
		this.getInstance().getInstance().instanceMethod(1);
		this.inner.printHello();
	}

	public void instanceMethod(int i) {
		this.toRefactor();
	}

	public static void staticMethod() {
		SubClass foo= new SubClass();
		foo.toRefactor();
	}

	public SubClass getInstance() {
		return new SubClass();
	}

	private class InnerClass {
		public void printHello() {
			System.out.println("Hello from InnerClass!");
		}
	}
}
