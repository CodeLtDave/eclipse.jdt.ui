package package1;

public class Example {
	
	public String[] greet(String[] ending) {
		String[] j = new String[] {ending[0], ending[1]};
		return j;
	}
	
	public static void main(String[] args) {
		Example instance = new Example();
		String[] test = new String[] {"hallo", "test"};
		String[] j = instance.greet(test);
	}
}