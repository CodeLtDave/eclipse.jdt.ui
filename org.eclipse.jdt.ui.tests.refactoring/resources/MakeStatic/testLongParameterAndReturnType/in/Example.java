package package1;

public class Example {

	int j;
	
	public long greet(long ending) {
		long i = ending;
		return i;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		long j = instance.greet(Long.MAX_VALUE);
		System.out.println(j);
	}
}