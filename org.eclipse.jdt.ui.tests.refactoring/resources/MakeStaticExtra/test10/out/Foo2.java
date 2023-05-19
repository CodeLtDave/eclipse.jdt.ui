public class Foo2 {
	public int myData;

	static int method(int i) {
		return myData + i;
	}
}

class Bar {
	public Foo2 myFoo;

	int a(int b) {
		return Foo2.method(b * 2);
	}
}
