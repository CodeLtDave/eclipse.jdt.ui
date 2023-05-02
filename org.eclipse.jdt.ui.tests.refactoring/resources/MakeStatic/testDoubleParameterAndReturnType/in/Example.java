package package1;

public class Example {

	int j;
	
	public double greet(double ending) {
		double i = ending;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		double j = instance.greet(Double.MAX_VALUE);
		System.out.println(j);
	}
}