public class Foo {
	Foo method() {
		final Foo[] result= new int[1];
		new Runnable() {
			public void run() {
				result[0]= Foo.this;
			}
		}.run();
		return result[0];
	}
}
