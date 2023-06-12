public class Foo {
	static Foo method(Foo foo) {
		final Foo[] result= new Foo[1];
		new Runnable() {
			public void run() {
				result[0]= foo;
			}
		}.run();
		return result[0];
	}
}
