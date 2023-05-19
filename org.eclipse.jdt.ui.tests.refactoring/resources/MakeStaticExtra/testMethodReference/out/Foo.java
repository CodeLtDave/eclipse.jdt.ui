class Foo {
	void test() {
		Foo2<Foo> f= Foo::yyy;
	}

	static void yyy(Foo foo) {
	}
}

interface Foo2<T> {
	void bar(T j);
}
