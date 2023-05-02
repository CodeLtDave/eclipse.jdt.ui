package package1;

public class Example {

	int j;
	
	public static char greet(char ending) {
		char i = ending;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		char j = Example.greet('E');
		System.out.println(j);
	}
}