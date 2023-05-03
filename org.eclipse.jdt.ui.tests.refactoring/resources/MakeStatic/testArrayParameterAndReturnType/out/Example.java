package package1;

public class Example {
	
	public static String[] toRefactor(String[] ending) {
		String[] j = new String[] {ending[0], ending[1]};
		return j;
	}
	
	public static void foo() {
		Example instance = new Example();
		String[] stringArray = new String[] {"bar", "bar"};
		String[] j = Example.toRefactor(stringArray);
	}
}