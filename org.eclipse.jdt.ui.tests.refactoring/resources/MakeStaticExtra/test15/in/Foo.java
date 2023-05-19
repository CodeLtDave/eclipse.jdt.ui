public class Foo {
	Foo getAnotherFoo() {
	}

	void tryMakeMeStatic(boolean b) {
		if (b) {
			getAnotherFoo().tryMakeMeStatic(!b);
		}
	}
}
