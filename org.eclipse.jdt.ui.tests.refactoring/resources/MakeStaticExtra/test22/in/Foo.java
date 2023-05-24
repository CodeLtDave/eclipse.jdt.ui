class Foo {

	public void foo() {
		bar();
	}

	void bar() {
	}

	class A {
		void blah() {
			foo();
		}
	}
}
