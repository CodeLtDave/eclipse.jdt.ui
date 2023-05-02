package package1;

public class Example {

	int j;
	
	public float greet(float ending) {
		float i = ending;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		float j = instance.greet(Float.MAX_VALUE);
		System.out.println(j);
	}
}