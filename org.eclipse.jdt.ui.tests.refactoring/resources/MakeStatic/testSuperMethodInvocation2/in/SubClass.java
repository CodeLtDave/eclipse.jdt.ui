package package1;

public class SubClass extends SuperClass {

	@Override
	public String toRefactor(String bar) {
		String i = super.toRefactor(bar);
		return i;
	}
	
	public static void staticMethod() {
		SubClass instance = new SubClass();
		String j = instance.toRefactor("bar");
	}
}