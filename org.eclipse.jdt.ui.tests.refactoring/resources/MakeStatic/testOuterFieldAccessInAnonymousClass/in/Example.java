package package1;

public class Example {
	public void bar() {
		new Thread() {
			public void run() {
				System.out.println(Example.this); // Outer class instance
			}
		}.start();
	}
}
