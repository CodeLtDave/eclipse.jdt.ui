public class Foo {
	public int myData;

	static int method(int i, Foo foo) {
        return foo.myData + i;
    }
}

public class Bar extends Foo {
	int a(int b) {
		return method(this, b * 2);
	}
}
