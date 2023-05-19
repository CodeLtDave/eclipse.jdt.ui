public class Foo {
	static Foo method(final Foo foo) {
		final Foo[] result= new int[1];
		new Runnable() {
			public void run() {
				result[0]= foo;
			}
		}.run();
		return result[0];
	}
}
