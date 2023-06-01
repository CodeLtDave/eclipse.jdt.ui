public class Foo {
	Foo method() {
		final Foo[] result= new Foo[1];
		new Runnable() {
			public void run() {
				result[0]= Foo.this;
			}
		}.run();
		return result[0];
	}
}
