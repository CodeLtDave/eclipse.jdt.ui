public class Foo {

	public int f(int i) {
		return 0;
	}

	public static int method(Foo foo, int i) {
		return foo.f(i);
	}
}