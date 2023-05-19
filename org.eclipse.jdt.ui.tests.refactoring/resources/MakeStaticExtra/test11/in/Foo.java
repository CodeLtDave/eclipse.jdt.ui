public class Foo {
	public int myData;

	int method(int i) {
		return myData + i;
	}
}

public class Bar extends Foo {
	int method(int b) {
		return super.method(b * 2);
	}
}
