package package1;

public class Example {

	class Inner {
	}

	public static void bar(Example example) {
		Inner inner= example.new Inner(); // Instance of non-static inner class
	}
}
