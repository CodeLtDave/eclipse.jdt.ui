package package1;

public class Example {

	int j;
	
	public static String greet(String ending) {
		String i = "hallo" + ending;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		String j = Example.greet("David");
		System.out.println(j);
	}
}