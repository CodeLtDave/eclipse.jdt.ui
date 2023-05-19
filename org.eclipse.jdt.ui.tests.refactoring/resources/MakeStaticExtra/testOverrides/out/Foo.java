class PP {
	void foo() {
	}
}

class Foo extends PP {
	void foo() {
		foo();
		bazz(0);
	}

	private static void bazz(int k) {
		bazz(k);
		bazz(k);
	}
}
