package package1;

public class Example {
	public static void bar(final Example example) {
		new Thread() {
			public void run() {
				System.out.println(example); // Outer class instance
			}
		}.start();
	}
}
