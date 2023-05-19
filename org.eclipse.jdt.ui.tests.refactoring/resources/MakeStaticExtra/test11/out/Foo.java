public class Foo {
	public int myData;

	static int method(int i, Foo foo) {
		return foo.myData + i;
	}
}

public class Bar extends Foo {
	int method(int b) {
		return super.method(this, b * 2);
	}
}
