class Foo {

	public static void foo(Foo foo) {
		foo.bar();
	}

	void bar() {
	}

	class A {
		void blah() {
			Foo.foo(Foo.this);
		}
	}
}
