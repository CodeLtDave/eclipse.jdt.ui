class Foo {
    void test() {
        Runnable f = this::yyy;
    }
    
    String myField = "";
    void yyy() {
        System.out.println(myField);
    }
}