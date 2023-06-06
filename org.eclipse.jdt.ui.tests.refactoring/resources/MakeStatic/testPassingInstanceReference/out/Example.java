package package1;

public class Example {

	Example example;

	public static void bar(Example example) {
		example.method(example);
		example.method(example.example);
		example.method(example.example);
	}

	public void method(Example example) {

	}
}
