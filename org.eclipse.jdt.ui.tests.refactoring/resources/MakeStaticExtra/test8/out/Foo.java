public class Foo {
	public int myData;

	static int method(Foo foo, int i) {
		new Runnable() {
			void f() {
			};

			public void run() {
				this.f();
			}
		};
		return foo.myData + foo.myData;
	}
}
