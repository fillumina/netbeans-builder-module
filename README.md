# A Netbeans module that adds _constants_, _fluent setters_ and _builder_ to code generators.

![image of the builder](https://raw.githubusercontent.com/fillumina/netbeans_builder_module/master/full_image.png "Image of the builder")

## Versions

3.1 2020-09-23 fixed java.lang.LinkageError (conflict with nb-javac plugin)

3.0 2020-07-28 Added:

    - fluent setters using withName()
    - copy constructor
    - updated to latest API

2.0 2014-07-22 (was externally named 1.0)

1.0 First version

## Download

The plugin can be downloaded from the 
[Netbeans Plugin Center](http://plugins.netbeans.org/plugin/55184/?show=true).


## Constants

Accessing fields via reflection is risky because string names cannot be
enforced to match the fields they refer to. To mitigate this problem an
automatic procedure can be used to auto-generate them.
The constants start with an underscore so to allow the automatic removal of
removed or changed fields. The underscore can also be useful to implicitly
specify that the constant refers to a field name.

```java
    public static final String _FIELD_NAME = "fieldName";

    private int fieldName;
```

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

A different naming for fluent setters use the 'with' prefix so that the 
previous name(String) method would be called withName(String). This is
useful with IDE that allows browsing available methods to quickly find
fluent setters by just typing the prefixed 'with' (from version 3.0+).

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