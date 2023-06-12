public class Foo {
	Foo foo;

	public void bar() {
		this.foo.foo.foo.method();
		this.getInstance().getInstance().method();
		foo.getInstance().foo.getInstance().foo.method();
		getInstance().foo.getInstance().foo.getInstance().method();
	}


	public Foo getInstance() {
		return this;
	}

	public void method() {
	}
}
