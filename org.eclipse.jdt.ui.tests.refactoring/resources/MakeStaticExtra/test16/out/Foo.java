public class Foo {
	void anotherMethod(String s);

	String field;

	/**
	 * @param anObject
	 * @param field
	 */
	static void method(String field, Foo foo) {
		foo.anotherMethod(field);
	}
}
