class Foo {

	public static void foo(Foo foo) {
		foo.bar();
	}

	void bar() {
	}

	class A {
		void blah() {
			foo(Foo.this);
		}
	}
}
