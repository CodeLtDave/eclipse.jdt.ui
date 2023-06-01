package package1;

public class Example {

	int j;

	public void bar(String... items) {
		this.j= 0;
	}

	public void baz() {
		Example example= new Example();
		example.bar("A", "B", "C");
	}
}
