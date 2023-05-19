final class Foo
		extends JFrame {
	public Bug() {
        foo(this);
    }

	public static void foo(Foo foo) {
		foo.addWindowListener(foo.new MyWindowListener());
	}

	private class MyWindowListener
			extends WindowAdapter {
		public void windowActivated(int e) {
		}
	}
}

class JFrame {
	public void addWindowListener(WindowAdapter e) {
	}

	static class WindowAdapter {
		public void windowActivated(int e) {
		}
	}
}
