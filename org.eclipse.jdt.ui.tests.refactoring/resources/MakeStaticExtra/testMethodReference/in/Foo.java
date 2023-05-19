class Foo {
	void test() {
		Foo2<Foo> f= Foo::yyy;
	}

	void yyy() {
	}
}

interface Foo2<T> {
	void bar(T j);
}
