public class Foo {
	void anotherMethod(String s) {
	};

	String field;

	/**
	 * @param foo
	 */
	static void method(Foo foo) {
		foo.anotherMethod(foo.field);
	}
}
