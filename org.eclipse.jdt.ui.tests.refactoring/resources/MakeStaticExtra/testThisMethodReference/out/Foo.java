class Foo {
	void test() {
		Runnable f= () -> yyy(this);
	}

	String myField= "";

	static void yyy(Foo foo) {
		System.out.println(foo.myField);
	}
}
