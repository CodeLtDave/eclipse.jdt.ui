public class Foo {
	Foo getAnotherFoo() {
	}

	static void tryMakeMeStatic(boolean b,Foo foo) {
		if (b) {
			Foo.tryMakeMeStatic(foo.getAnotherFoo(), !b);
		}
	}
}
