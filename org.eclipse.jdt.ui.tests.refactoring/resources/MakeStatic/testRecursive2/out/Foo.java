public class Foo {
	Foo method() {
		return new Foo();
	}

	static void bar(Foo foo) {
		Foo.bar(foo.method());
	}
}
