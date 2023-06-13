import java.util.function.Function;

class Foo {
	Foo2 method(Function<Foo, Foo2> function) {
		return function.apply(this);
	}
}

class Foo2 {
	static Foo2 bar(Foo2 foo2, Foo foo) {
		return foo2;
	}
}

class Foo3 {
	public void method2() {
		Foo2 bar= new Foo2();
		new Foo().method(foo -> Foo2.bar(bar, foo));
	}
}
