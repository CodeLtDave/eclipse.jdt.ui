public class Foo {

	public int f(int i) {
		return 0;
	}

	public static int method(int i, Foo foo) {
		return foo.f(i);
	}
}