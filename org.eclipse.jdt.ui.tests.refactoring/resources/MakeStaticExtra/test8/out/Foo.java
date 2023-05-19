public class Foo {
	public int myData;

	static int method(int i, final Foo foo) {
        new Runnable () {
            void f() {};
            public void run() {
                this.f(foo.myData);    
            }
        }
        return foo.myData + foo.myData;
    }
}
