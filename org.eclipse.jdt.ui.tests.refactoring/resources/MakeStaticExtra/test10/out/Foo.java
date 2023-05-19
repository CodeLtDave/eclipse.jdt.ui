public class Foo {
	public int myData;

	static int method(int i, Foo foo) {
		return foo.myData + i;
	}
}

class Bar {
	public Foo myFoo;

	int a(int b) {
		return Foo.method(b * 2, myFoo);
	}
}
