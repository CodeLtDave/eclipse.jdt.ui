import java.util.function.Function;

class Foo {
  Bar frobnitz(Function<Foo, Bar> f) {
    return f.apply(this);
  }
}

class Bar {
  Bar frob(Foo foo) {
    return this;
  }
}

class Baz {
  public static void main(String[] args) {
    Bar bar = new Bar();
    new Foo().frobnitz(bar::frob);
  }
}
