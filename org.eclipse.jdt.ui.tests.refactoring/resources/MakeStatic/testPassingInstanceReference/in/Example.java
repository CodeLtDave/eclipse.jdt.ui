package package1;

public class Example {

	Example example;

	public void bar() {
		method(this);
		method(this.example);
		method(example);
	}

	public void method(Example example) {

	}
}
