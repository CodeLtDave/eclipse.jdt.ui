package package1;

public class SubClass extends SuperClass {
	private int counter;

	private InnerClass inner;

	private SubClass instanceObject;

	private static InnerClass staticInner;

	private static int staticInt= 0;

	public static void toRefactor(SubClass subClass) {
		subClass.intParent++;
		subClass.instanceParent(1);
		subClass.instanceObject.counter++;
		subClass.instanceObject.counter++;
		subClass.intParent= staticInt + 1;
		int localInt= 0;
		subClass.counter++;
		subClass.counter++;
		subClass.counter= subClass.counter + 1;
		subClass.instanceMethod(1);
		subClass.instanceMethod(1);
		staticMethod();
		subClass.getInstance().getInstance().instanceMethod(1);
		subClass.inner.printHello();
	}

	public void instanceMethod(int i) {
		SubClass.toRefactor(this);
	}

	public static void staticMethod() {
		SubClass foo= new SubClass();
		SubClass.toRefactor(foo);
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
