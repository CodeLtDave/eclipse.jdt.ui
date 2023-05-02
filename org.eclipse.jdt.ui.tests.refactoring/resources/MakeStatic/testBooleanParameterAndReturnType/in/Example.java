package package1;

public class Example {

	int j;
	
	public boolean greet(boolean boo) {
		boolean i = boo;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		boolean j = instance.greet(true);
		System.out.println(j);
	}
}