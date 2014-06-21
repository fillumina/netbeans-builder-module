# A Netbeans module that adds _fluent setters_ and _builder_ to code generators.

![image of the builder](https://raw.githubusercontent.com/fillumina/netbeans_builder_module/master/full_image.png "Image of the builder")

## Fluent Setters

Fluent setters are field setters that return _this_ and so can be appended one
to another:

```java
    public NamedBean name(final String value) {
        this.name = value;
        return this;
    }
```

The previous setter can be used like this:

```java
    NamedBean nb = new NamedBean().name("Some Name");
```

This methods allows for a better understanding of the class initialization
parameters but it cannot be used with immutable classes.

## Builder

A builder is a separate class uses a fluent interface to generate another class
(can be used with immutable classes as well).

This is an example of a builder as created by the plugin:

```java
public class MyBean {
    private static String pippo = "";

    private final int a = 1;
    private String name;
    private final int age;

    public static class Builder {

        private String name;
        private int age;

        private Builder() {
        }

        public Builder name(final String value) {
            this.name = value;
            return this;
        }

        public Builder age(final int value) {
            this.age = value;
            return this;
        }

        public MyBean build() {
            return new javaapplication1.MyBean(name, age);
        }
    }

    public static MyBean.Builder builder() {
        return new MyBean.Builder();
    }

    private MyBean(final String name, final int age) {
        this.name = name;
        this.age = age;
    }

}
```

Note that the initialized _final field_ is not considered in the builder.
The generators can also be used while refactoring because they remove
automatically the old artifacts and replace them with the new version.