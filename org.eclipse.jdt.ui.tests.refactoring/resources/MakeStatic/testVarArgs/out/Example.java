package package1;

public class Example {

	int j;

	public static void bar(Example example, String... items) {
		example.j= 0;
	}

	public void baz() {
		Example example= new Example();
		Example.bar(example, "A", "B", "C");
	}
}
