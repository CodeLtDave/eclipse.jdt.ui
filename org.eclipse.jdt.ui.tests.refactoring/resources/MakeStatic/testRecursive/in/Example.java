package package1;

public class Example {

	public void toRefactor(Example bar) {
		bar.toRefactor(this);
	}
	
	public void foo() {
		Example instance = new Example();
		instance.toRefactor(this);
	}
}