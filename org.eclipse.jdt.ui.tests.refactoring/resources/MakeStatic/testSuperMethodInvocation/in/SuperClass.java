package package1;

public class SuperClass {

	public String toRefactor(String bar) {
		String i = bar;
		return i;
	}
	
	public static void staticMethod() {
		SuperClass instance = new SuperClass();
		String j = instance.toRefactor("bar");
	}
}