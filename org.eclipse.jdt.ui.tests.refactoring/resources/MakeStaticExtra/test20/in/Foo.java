final class Foo
		extends JFrame {
	public Foo() {
		foo();
	}

	public void foo() {
		addWindowListener(new MyWindowListener());
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
