package package1;

public class Example {

	int j;
	
	public short greet(short ending) {
		short i = ending;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		short j = instance.greet(Short.MAX_VALUE);
		System.out.println(j);
	}
}