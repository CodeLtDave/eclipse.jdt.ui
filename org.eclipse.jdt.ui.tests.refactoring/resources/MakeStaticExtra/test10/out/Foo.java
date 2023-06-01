public class Foo {
	public int myData;

	static int method(Foo foo, int i) {
		return foo.myData + i;
	}
}

class Bar {
	public Foo myFoo;

	int a(int b) {
		return Foo.method(myFoo, b * 2);
	}
}
