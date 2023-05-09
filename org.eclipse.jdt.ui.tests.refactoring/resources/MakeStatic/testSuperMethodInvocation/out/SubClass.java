package package1;

public class SubClass extends SuperClass {

	public static String toRefactor(String bar, SubClass subclass) {
		String i = subclass.toRefactor(bar);
		return i;
	}

	public static void staticMethod() {
		SubClass instance = new SubClass();
		String j = SubClass.toRefactor("bar", instance);
	}
}