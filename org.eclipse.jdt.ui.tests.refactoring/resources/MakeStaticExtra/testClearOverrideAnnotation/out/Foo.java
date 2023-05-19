interface A {
	void foo();
}

class Foo implements A {
	@Override
	public void foo() {
		foo(this);
	}

	public static void foo(Foo foo) {
	}
}
