class Foo {
	void test() {
		Foo2<Test4> f= Foo::yyy;
	}

	void yyy() {
	}
}

interface Foo2<T> {
	void bar(T j);
}
