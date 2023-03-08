# TypeInfo
Java Reflection based type analysis (including generics resolution)

## Example 1

give it

```java
class C__ <T extends Number> {
    public List<HashMap<T[][], Pair<Integer[], C__<Integer>>>[]> foo;
}
```

```java
try {
    TypeInfo.getFieldRecursive(C__.class, "foo").printDetailed();
} catch (NoSuchFieldException e) {
    throw new RuntimeException(e);
}
```

and it outputs
```
TypeInfo {
  field: public java.util.List<java.util.HashMap<java.lang.Number[][], smallville7123.reflectui.utils.Pair<java.lang.Integer[], smallville7123.Main.C__<java.lang.Integer>>>[]> smallville7123.Main.C__#foo
  field type: 
    TypeInfo {
      type: java.util.List<java.util.HashMap<java.lang.Number[][], smallville7123.reflectui.utils.Pair<java.lang.Integer[], smallville7123.Main.C__<java.lang.Integer>>>[]>
      isGeneric: true
      genericParameters: 1
        TypeInfo {
          type: java.util.HashMap<java.lang.Number[][], smallville7123.reflectui.utils.Pair<java.lang.Integer[], smallville7123.Main.C__<java.lang.Integer>>>[]
          isGeneric: true
          typeRank: 1
          genericParameters: 2
            TypeInfo {
              type: java.lang.Number[][]
              typeRank: 2
            }
            TypeInfo {
              type: smallville7123.reflectui.utils.Pair<java.lang.Integer[], smallville7123.Main.C__<java.lang.Integer>>
              isGeneric: true
              genericParameters: 2
                TypeInfo {
                  type: java.lang.Integer[]
                  typeRank: 1
                }
                TypeInfo {
                  type: smallville7123.Main.C__<java.lang.Integer>
                  isGeneric: true
                  genericParameters: 1
                    TypeInfo {
                      type: java.lang.Integer
                    }
                }
            }
        }
    }
}
```

## Example 2

give it

```java
class C__ <T extends java.io.Serializable> {
    public List<HashMap<T[][], Pair<T[], C__<StringBuilder>>>[]> foo;
}
```
```java
try {
    // get the field of new C__<Number>().foo.get(0)[0].values().iterator().next().second
    TypeInfo foo = TypeInfo.getFieldRecursive(C__.class, "foo");
    for (TypeInfo foo2 : TypeInfo.getMethodsRecursive(foo.getReturnType(), "get")) {
        for (TypeInfo foo3 : TypeInfo.getMethodsRecursive(foo2.getReturnType(), "values")) {
            for (TypeInfo foo4 : TypeInfo.getMethodsRecursive(foo3.getReturnType(), "iterator")) {
                for (TypeInfo foo5 : TypeInfo.getMethodsRecursive(foo4.getReturnType(), "next")) {
                    TypeInfo foo6 = TypeInfo.getFieldRecursive(foo5.getReturnType(), "second");
                    // get the field of new C__<Integer>().foo.get(0)[0].values().iterator().next().first
                    TypeInfo foo7 = TypeInfo.getFieldRecursive(foo6.getReturnType(), "foo");
                    foo7.printDetailed();
                    for (TypeInfo foo8 : TypeInfo.getMethodsRecursive(foo7.getReturnType(), "get")) {
                        for (TypeInfo foo9 : TypeInfo.getMethodsRecursive(foo8.getReturnType(), "values")) {
                            for (TypeInfo foo10 : TypeInfo.getMethodsRecursive(foo9.getReturnType(), "iterator")) {
                                for (TypeInfo foo11 : TypeInfo.getMethodsRecursive(foo10.getReturnType(), "next")) {
                                    TypeInfo foo12 = TypeInfo.getFieldRecursive(foo11.getReturnType(), "first");
                                    foo12.printDetailed();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} catch (NoSuchMethodException | NoSuchFieldException e) {
    throw new RuntimeException(e);
}
```

and it outputs
```
TypeInfo {
  field: public java.util.List<java.util.HashMap<java.lang.StringBuilder[][], smallville7123.reflectui.utils.Pair<java.lang.StringBuilder[], smallville7123.Main.C__<java.lang.StringBuilder>>>[]> smallville7123.Main.C__#foo
  field type: 
    TypeInfo {
      type: java.util.List<java.util.HashMap<java.lang.StringBuilder[][], smallville7123.reflectui.utils.Pair<java.lang.StringBuilder[], smallville7123.Main.C__<java.lang.StringBuilder>>>[]>
      isGeneric: true
      genericParameters: 1
        TypeInfo {
          type: java.util.HashMap<java.lang.StringBuilder[][], smallville7123.reflectui.utils.Pair<java.lang.StringBuilder[], smallville7123.Main.C__<java.lang.StringBuilder>>>[]
          isGeneric: true
          typeRank: 1
          genericParameters: 2
            TypeInfo {
              type: java.lang.StringBuilder[][]
              typeRank: 2
            }
            TypeInfo {
              type: smallville7123.reflectui.utils.Pair<java.lang.StringBuilder[], smallville7123.Main.C__<java.lang.StringBuilder>>
              isGeneric: true
              genericParameters: 2
                TypeInfo {
                  type: java.lang.StringBuilder[]
                  typeRank: 1
                }
                TypeInfo {
                  type: smallville7123.Main.C__<java.lang.StringBuilder>
                  isGeneric: true
                  genericParameters: 1
                    TypeInfo {
                      type: java.lang.StringBuilder
                    }
                }
            }
        }
    }
}

TypeInfo {
  field: public java.lang.StringBuilder smallville7123.reflectui.utils.Pair#first
  field type: 
    TypeInfo {
      type: java.lang.StringBuilder
    }
}
```
