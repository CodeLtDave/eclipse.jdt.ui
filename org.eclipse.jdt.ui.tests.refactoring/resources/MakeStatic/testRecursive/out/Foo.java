public class Foo {

	public static void bar(Foo foo2, Foo foo) {
		Foo.bar(foo2, foo);
	}

	public void foo() {
		Foo instance= new Foo();
		Foo.bar(this, instance);
	}
}
