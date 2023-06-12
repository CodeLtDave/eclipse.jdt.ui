public class Foo {
	Foo foo;

	public static void bar(Foo foo) {
		foo.foo.foo.foo.method();
		foo.getInstance().getInstance().method();
		foo.foo.getInstance().foo.getInstance().foo.method();
		foo.getInstance().foo.getInstance().foo.getInstance().method();
	}


	public Foo getInstance() {
		return this;
	}

	public void method() {
	}
}
