class Foo {
	{
    new Runnable() {
      private void print() {}
      @Override
      public void run() {
        Runnable r = this::print;
      }
    }
  }
}
