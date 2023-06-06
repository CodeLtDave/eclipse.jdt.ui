public class Foo {
	public int myData;

	static int method(Foo foo, int i) {
		return foo.myData + i;
	}
}

public class Bar extends Foo {
	int a(int b) {
		return Foo.method(this, b * 2);
	}
}
