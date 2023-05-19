class Foo {
	int i;

	private void foo() {
		if (true) {
			foo();
		}
		System.out.println(i);
	}

	{
		foo();
	}
}
