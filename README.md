# TypeInfo
Java Reflection based type analysis (including generics resolution)


give it

```java
class C__ <T extends Number> {
    public List<HashMap<T[][], Pair<Integer[], C__<Integer>>>[]> foo;
}
```

```java
TypeInfo.GLOBAL_OUTPUT = true;
try {
    TypeInfo.getFieldRecursive(C__.class, "foo").printDetailed();
} catch (NoSuchFieldException e) {
    throw new RuntimeException(e);
}
TypeInfo.GLOBAL_OUTPUT = false;
```

ant it outputs
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
