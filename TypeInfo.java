package smallville7123.reflectui;

import static smallville7123.reflectui.utils.PRINTLN.println;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import smallville7123.reflectui.utils.Pair;


public class TypeInfo {
    private static final int MAX_RECUSIVE_DEPTH = 30;
    static class ClassInfo {

        /**
         * responsible for generating a mapping of all generic type information to actual expected types as would be seen during compilation and at runtime
         */
        void map() {
            if (GLOBAL_OUTPUT) {
                println("mapping class: " + clazz);
            }
            typeVariables = clazz.getTypeParameters();
            // typeinfo is never null
            if (typeInfo.genericParameters != null) {
                this.isGeneric = typeInfo.isGeneric();
                if (isGeneric) {
                    if (GLOBAL_OUTPUT) {
                        println("creating a synthetic mapping for " + typeInfo.getType() + " to satisfy TypeVariable's " + Arrays.toString(typeVariables));
                    }
                    mapping = new ArrayList<>(typeVariables.length);
                    ArrayList<TypeInfo> parameters = typeInfo.genericParameters;
                    for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
                        TypeInfo genericParameter = parameters.get(i);
                        testSelfBounds(genericParameter, typeVariables[i]);
                        Object obj = genericParameter.selfBound ? genericParameter : genericParameter.isGeneric() ? genericParameter : genericParameter.getType();
                        mapping.add(new Pair<>(typeVariables[i], obj == null ? Object.class : obj));
                    }
                    if (GLOBAL_OUTPUT) {
                        println("mapping: " + mapping);
                    }
                }
            } else {
                this.isGeneric = typeVariables.length > 0 || parameterizedType != null;
                if (isGeneric) {
                    Object[] actualTypeVariables = null;

                    if (parameterizedType != null) {
                        actualTypeVariables = parameterizedType.getActualTypeArguments();
                    }

                    if (GLOBAL_OUTPUT) {
                        println("    TypeVariable's        " + Arrays.toString(typeVariables));
                        println("    ACTUAL TypeVariable's        " + Arrays.toString(actualTypeVariables));
                        println("mapping type arguments");
                    }

                    mapping = new ArrayList<>();

                    if (actualTypeVariables == null) {
                        for (TypeVariable<?> t : typeVariables) {
                            testSelfBounds(typeInfo, t);
                            mapping.add(new Pair<>(t, typeInfo.selfBound ? typeInfo : resolveBounds(t)));
                        }
                    } else {
                        for (int i = 0, actualTypeVariablesLength = actualTypeVariables.length; i < actualTypeVariablesLength; i++) {
                            Object actualTypeVariable = actualTypeVariables[i];
                            if (actualTypeVariable instanceof TypeVariable<?>) {
                                TypeVariable<?> typeVariable1 = (TypeVariable<?>) actualTypeVariable;
                                if (GLOBAL_OUTPUT) {
                                    println("LOOKING FOR: " + actualTypeVariable + " IN PARENT");
                                }
                                if (parent == null) {
                                    throw new RuntimeException("PARENT CANNOT BE NULL");
                                }
                                boolean linked = false;
                                for (Pair<TypeVariable<?>, Object> pair : parent.mapping) {
                                    if (GLOBAL_OUTPUT) {
                                        println("TRYING: " + pair.first);
                                    }
                                    if (typeVariable1.toString().contentEquals(pair.first.getName())) {
                                        if (GLOBAL_OUTPUT) {
                                            println("NAME MATCHES: " + pair.first);
                                        }
                                        linked = true;
                                        Pair<TypeVariable<?>, Object> link = new Pair<>(typeVariables[i], pair.second);
                                        if (GLOBAL_OUTPUT) {
                                            println("LINK TO: " + link);
                                        }
                                        mapping.add(link);
                                    }
                                }
                                if (!linked) {
                                    Pair<TypeVariable<?>, Object> link = new Pair<>(typeVariables[i], resolveBounds((TypeVariable<?>) typeVariable1));
                                    if (GLOBAL_OUTPUT) {
                                        println("LINK TO: " + link);
                                    }
                                    mapping.add(link);
                                }
                            } else {
                                // we trust that we have correctly resolved information
                                Pair<TypeVariable<?>, Object> link = new Pair<>(typeVariables[i], actualTypeVariable);
                                if (GLOBAL_OUTPUT) {
                                    println("LINK TO: " + link);
                                }
                                mapping.add(link);
                            }
                        }
                    }
                    if (GLOBAL_OUTPUT) {
                        println("mapping: " + mapping);
                    }
                    typeInfo.ResolveTypeEX(mapping, clazz, 0, parent == null ? null : parent.typeInfo);
                }
            }
        }
        ClassInfo parent;
        Class<?> clazz;
        boolean isGeneric;
        ParameterizedType parameterizedType;
        ArrayList<TypeInfo> methods;
        ArrayList<TypeInfo> fields;
        ClassInfo superclass;
        ArrayList<ClassInfo> superinterfaces;
        TypeInfo typeInfo;

        TypeVariable<?>[] typeVariables;

        ArrayList<Pair<TypeVariable<?>, Object>> mapping;

        public ClassInfo(Class<?> clazz) {
            init((Class<?>) clazz, null, null);
        }

        public ClassInfo(TypeInfo typeInfo) {
            init(typeInfo);
        }

        ClassInfo(Class<?> clazz, ParameterizedType parameterizedType, ClassInfo parent) {
            init(clazz, parameterizedType, parent);
        }

        void init(TypeInfo typeInfo) {
            fields = new ArrayList<>();
            methods = new ArrayList<>();
            this.typeInfo = typeInfo;
            this.clazz = typeInfo.getType();
            map();
            Type superclass1 = clazz.getGenericSuperclass();
            if (superclass1 != null) {
                if (superclass1 instanceof ParameterizedType) {
                    ParameterizedType superclass11 = (ParameterizedType) superclass1;
                    superclass = new ClassInfo((Class<?>) superclass11.getRawType(), superclass11, this);
                } else {
                    superclass = new ClassInfo((Class<?>) superclass1, null, this);
                }
            }
            Type[] interfaces1 = clazz.getGenericInterfaces();
            if (interfaces1.length > 0) {
                superinterfaces = new ArrayList<>();
            }
            for (Type interface1 : interfaces1) {
                if (interface1 instanceof ParameterizedType) {
                    ParameterizedType interface11 = (ParameterizedType) interface1;
                    superinterfaces.add(new ClassInfo((Class<?>) interface11.getRawType(), interface11, this));
                } else {
                    superinterfaces.add(new ClassInfo((Class<?>) interface1, null, this));
                }
            }
        }

        void init(Class<?> clazz, ParameterizedType parameterizedType, ClassInfo parent) {
            fields = new ArrayList<>();
            methods = new ArrayList<>();
            this.clazz = clazz;
            this.typeInfo = new TypeInfo(clazz, parent == null ? null : parent.typeInfo);
            this.parameterizedType = parameterizedType;
            this.parent = parent;
            map();
            Type superclass1 = clazz.getGenericSuperclass();
            if (superclass1 != null) {
                if (superclass1 instanceof ParameterizedType) {
                    ParameterizedType superclass11 = (ParameterizedType) superclass1;
                    superclass = new ClassInfo((Class<?>) superclass11.getRawType(), superclass11, this);
                } else {
                    superclass = new ClassInfo((Class<?>) superclass1, null, this);
                }
            }
            Type[] interfaces1 = clazz.getGenericInterfaces();
            if (interfaces1.length > 0) {
                superinterfaces = new ArrayList<>();
            }
            for (Type interface1 : interfaces1) {
                if (interface1 instanceof ParameterizedType) {
                    ParameterizedType interface11 = (ParameterizedType) interface1;
                    superinterfaces.add(new ClassInfo((Class<?>) interface11.getRawType(), interface11, this));
                } else {
                    superinterfaces.add(new ClassInfo((Class<?>) interface1, null, this));
                }
            }
        }

        static List<TypeVariable<?>> getTypeVariables(Type type, int indent) {
            List<TypeVariable<?>> types = new ArrayList<>();
            if (type == null) {
                return types;
            }
            if (indent > MAX_RECUSIVE_DEPTH) {
                throw new RuntimeException("OVERFLOW");
            }
            if (type instanceof ParameterizedType) {
                // we have type parameters
                for (Type type1 : ((ParameterizedType) type).getActualTypeArguments()) {
                    types.addAll(getTypeVariables(type1, indent + 1));
                }
            } else if (type instanceof GenericArrayType) {
                // we have a generic array, Type[]... or Type ...
                Type type1 = type;
                while (type1 instanceof GenericArrayType) {
                    type1 = ((GenericArrayType) type1).getGenericComponentType();
                }
                types.addAll(getTypeVariables(type1, indent + 1));
            } else if (type instanceof WildcardType) {
                WildcardType wildcardType1 = (WildcardType) type;
                Type[] lowerBounds = wildcardType1.getLowerBounds();
                Type[] upperBounds = wildcardType1.getUpperBounds();
                // ? extends X   = LOWER [null] UPPER [X]
                // ? super   X   = LOWER [X] UPPER [Object]
                for (Type lowerBound : lowerBounds) {
                    types.addAll(getTypeVariables(lowerBound, indent+1));
                }
                for (Type upperBound : upperBounds) {
                    types.addAll(getTypeVariables(upperBound, indent+1));
                }
            } else if (!(type instanceof Class<?>)) {
                if (type instanceof TypeVariable<?>) {
                    types.add((TypeVariable<?>) type);
                } else {
                    throw new RuntimeException("UNKNOWN TYPE: [CLASS: " + type.getClass() + "], TYPE: " + type);
                }
            }
            return types;
        }

        void testSelfBounds(TypeInfo t, TypeVariable<?> type) {
            if (t.selfBound) return;
            if (type != null) {
                for (Type bound : type.getBounds()) {
                    List<TypeVariable<?>> typeVariables1 = getTypeVariables(bound, 0);
                    for (TypeVariable<?> typeVariable : typeVariables1) {
                        if (type.getName().contentEquals(typeVariable.getName())) {
                            t.selfBound = true;
                            return;
                        }
                    }
                }
            }
        }

        private Type resolveBounds(TypeVariable<?> t) {
            Type[] bounds = t.getBounds();
            if (GLOBAL_OUTPUT) {
                println("BOUNDS: " + Arrays.toString(bounds));
            }
            Type targ = null;
            if (bounds.length == 1) {
                targ = bounds[0];
            } else {
                for (Type bound : bounds) {
                    if (bound instanceof Class<?>) {
                        Class<?> aClass = (Class<?>) bound;
                        if (!aClass.isInterface()) {
                            targ = aClass;
                        }
                    }
                }
            }
            return targ == null ? Object.class : targ;
        }
    }

    // set to true to enable debug output for all TypeInfo invocations
    static public boolean GLOBAL_OUTPUT = false;
    Field field;
    Method method;
    Class<?> type;
    int typeRank = 0;
    ArrayList<TypeInfo> genericParameters;
    ArrayList<TypeInfo> methodParameters;
    TypeInfo returnType;
    boolean isTypeVariable_;
    TypeVariable<?> typeVariables;
    boolean isWildcard_;

    WildcardType wildcardType;
    private TypeInfo parent;
    boolean selfBound;
    String debugString;
    TypeInfo superclass;
    ArrayList<TypeInfo> interfaces;

    private TypeVariable<?>[] typeParameters;

    public TypeInfo getSuperclass() {
        return superclass;
    }

    public ArrayList<TypeInfo> getInterfaces() {
        return interfaces;
    }

    public String getDebugString() {
        return debugString;
    }

    public Field getField() {
        return field;
    }

    public Method getMethod() {
        return method;
    }

    public static TypeInfo[] getMethodsRecursive(TypeInfo clazz, String methodName) throws NoSuchMethodException {
        if (GLOBAL_OUTPUT) {
            println("obtaining method: " + methodName);
        }
        return getMethodsRecursive(new ClassInfo(clazz), methodName).toArray(new TypeInfo[0]);
    }

    public static TypeInfo[] getMethodsRecursive(Class<?> clazz, String methodName) throws NoSuchMethodException {
        if (GLOBAL_OUTPUT) {
            println("obtaining method: " + methodName);
        }
        return getMethodsRecursive(new ClassInfo(clazz), methodName).toArray(new TypeInfo[0]);
    }

    private static ArrayList<TypeInfo> getMethodsRecursive(ClassInfo classInfo, String methodName) throws NoSuchMethodException {
        ArrayList<TypeInfo> m = getMethodsRecursive(classInfo, methodName, 0, new ArrayList<>());
        ArrayList<TypeInfo> m3 = new ArrayList<>();
        for (TypeInfo method : m) {
            // 1. get all methods with the same parameters as the current method
            List<TypeInfo> m4 = new ArrayList<>();
            for (TypeInfo method11 : m) {
                if (Arrays.equals(method.method.getParameterTypes(), method11.method.getParameterTypes())) {
                    m4.add(method11);
                }
            }
            // 2. find highest returning superclass
            TypeInfo highest = null;
            for (TypeInfo method1 : m4) {
                Class<?> t = method1.returnType.type;
                if (highest == null) {
                    if (GLOBAL_OUTPUT) {
                        println("highest method1: " + method1.toModifiedTypeString());
                    }
                    highest = method1;
                } else {
                    if (highest.returnType.type.isAssignableFrom(t)) {
                        if (GLOBAL_OUTPUT) {
                            println("new highest method1: " + method1.toModifiedTypeString());
                        }
                        highest = method1;
                    }
                }
            }
            if (!m3.contains(highest)) {
                m3.add(highest);
            }
        }
        return m3;
    }

    public static TypeInfo getFieldRecursive(TypeInfo clazz, String fieldName) throws NoSuchFieldException {
        if (GLOBAL_OUTPUT) {
            println("obtaining field: " + fieldName);
        }
        return getFieldRecursive(new ClassInfo(clazz), fieldName);
    }

    public static TypeInfo getFieldRecursive(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        if (GLOBAL_OUTPUT) {
            println("obtaining field: " + fieldName);
        }
        return getFieldRecursive(new ClassInfo(clazz), fieldName);
    }

    private static TypeInfo getFieldRecursive(ClassInfo classInfo, String fieldName) throws NoSuchFieldException {
        ArrayList<TypeInfo> m = getFieldRecursive(classInfo, fieldName, 0, new ArrayList<>());
        // 1. find highest returning superclass
        TypeInfo highest = null;
        for (TypeInfo field : m) {
            Class<?> t = field.returnType.type;
            if (((Type)t) instanceof TypeVariable<?>) {
                throw new NoSuchFieldException("UNEXPECTED TYPE VARIABLE");
            }
            if (highest == null) {
                if (GLOBAL_OUTPUT) {
                    println("highest field: " + field.toModifiedTypeString());
                }
                highest = field;
            } else {
                if (highest.returnType.type.isAssignableFrom(t)) {
                    if (GLOBAL_OUTPUT) {
                        println("new highest field: " + field.toModifiedTypeString());
                    }
                    highest = field;
                }
            }
        }
        return highest;
    }

    private static ArrayList<TypeInfo> getFieldRecursive(ClassInfo classInfo, String fieldName, int depth, ArrayList<TypeInfo> seenFields) throws NoSuchFieldException {
        if (depth > MAX_RECUSIVE_DEPTH) {
            throw new RuntimeException("OVERFLOW");
        }
        // we must search top-down for the field name
        ArrayList<TypeInfo> info = new ArrayList<>();
        ClassInfo sc = classInfo;
        while (sc != null) {
            Field[] m = sc.clazz.getDeclaredFields();
            List<Field> m2 = new ArrayList<>();
            for (Field field2 : m) {
                if (!field2.isSynthetic() && field2.getName().contentEquals(fieldName)) {
                    m2.add(field2);
                }
            }

            if (sc.isGeneric) {
                if (GLOBAL_OUTPUT) {
                    println("GENERIC CLASS: " + sc.clazz);
                    println("GENERIC CLASS MAP: " + sc.mapping);
                }
                for (Field field : m2) {
                    TypeInfo typeInfo = new TypeInfo(sc.mapping, field, sc.typeInfo);
                    if (seenFields.isEmpty()) {
                        if (GLOBAL_OUTPUT) {
                            println("ADDING GENERIC FIELD: " + typeInfo.toModifiedTypeString());
                        }
                        seenFields.add(typeInfo);
                        sc.fields.add(typeInfo);
                    } else {
                        if (GLOBAL_OUTPUT) {
                            println("SKIPPING GENERIC FIELD: " + typeInfo.toModifiedTypeString());
                        }
                    }
                }
                info.addAll(sc.fields);

                if (sc.superinterfaces != null) {
                    // search superinterfaces
                    for (ClassInfo superinterface : sc.superinterfaces) {
                        if (GLOBAL_OUTPUT) {
                            println("INTERFACE: " + superinterface.clazz);
                        }
                        try {
                            info.addAll(getFieldRecursive(superinterface, fieldName, depth+1, seenFields));
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                }
                // finally search superclass
                sc = sc.superclass;
                if (sc != null) {
                    if (GLOBAL_OUTPUT) {
                        println("SUPERCLASS: " + sc.clazz);
                    }
                }
            } else {
                if (!m2.isEmpty()) {
                    // we have one or more fields in the current sc
                    for (Field field : m2) {
                        Type r = field.getGenericType();
                        if (containsTypeVariables(r)) {
                            // the field return type contains type variables
                            new NoSuchFieldException("CURRENT CLASS IS NOT GENERIC BUT WE FOUND TYPE VARIABLES: " + (sc.typeInfo != null ? sc.typeInfo : sc.clazz)).printStackTrace();
                            println();
                            return info;
                        }
                    }
                }
                if (GLOBAL_OUTPUT) {
                    println("STANDARD CLASS: " + sc.clazz);
                }
                for (Field field : m2) {
                    TypeInfo typeInfo = new TypeInfo(sc.mapping, field, sc.typeInfo);
                    if (seenFields.isEmpty()) {
                        if (GLOBAL_OUTPUT) {
                            println("ADDING STANDARD FIELD: " + typeInfo.toModifiedTypeString());
                        }
                        seenFields.add(typeInfo);
                        sc.fields.add(typeInfo);
                    } else {
                        if (GLOBAL_OUTPUT) {
                            println("SKIPPING STANDARD FIELD: " + typeInfo.toModifiedTypeString());
                        }
                    }
                }
                info.addAll(sc.fields);
                if (sc.superinterfaces != null) {
                    // search superinterfaces
                    for (ClassInfo superinterface : sc.superinterfaces) {
                        if (GLOBAL_OUTPUT) {
                            println("INTERFACE: " + superinterface.clazz);
                        }
                        try {
                            info.addAll(getFieldRecursive(superinterface, fieldName, depth + 1, seenFields));
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                }
                // finally search superclass
                sc = sc.superclass;
            }
        }
        if (info.isEmpty()) {
            throw new NoSuchFieldException(fieldName);
        }
        return info;
    }

    private String toGenericParametersString() {
        StringBuilder stringBuilderRet = new StringBuilder();
        if (isWildcard()) {
            if (genericParameters != null) {
                if (genericParameters.size() == 1) {
                    stringBuilderRet.append(genericParameters.get(0).toGenericParametersString());
                } else {
                    stringBuilderRet.append("### expected wildcard to have only 1 generic ###");
                }
            }
        } else {
            if (type == null) {
                if (selfBound) {
                    stringBuilderRet.append("### SELF BOUND ###");
                } else {
                    stringBuilderRet.append("### expected a type since we are not a wildcard ###");
                }
            } else {
                stringBuilderRet.append(Modifier.toString(type.getModifiers()));
                stringBuilderRet.append(" ");
                stringBuilderRet.append(type.getName());
            }
            if (genericParameters != null) {
                stringBuilderRet.append("<");
                StringJoiner joiner = new StringJoiner(", ");
                for (TypeInfo genericParameter : genericParameters) {
                    String toGenericParametersString = genericParameter.toGenericParametersString();
                    joiner.add(toGenericParametersString);
                }
                stringBuilderRet.append(
                        joiner
                );
                stringBuilderRet.append(">");
            }
        }
        for (int i = 0; i < typeRank; i++) {
            stringBuilderRet.append("[]");
        }
        return stringBuilderRet.toString();
    }

    private String toModifiedTypeString() {
        if (isType()) {
            return toGenericParametersString();
        } else {
            StringBuilder stringBuilderRet = new StringBuilder();
            stringBuilderRet.append(returnType == null ? "### incomplete type ###" : returnType.toGenericParametersString());
            stringBuilderRet.append(" ");
            StringBuilder stringBuilder = new StringBuilder();
            if (isField()) {
                stringBuilder.append(Modifier.toString(field.getModifiers()));
                stringBuilder.append(" ");
                stringBuilder.append(stringBuilderRet);
                stringBuilderRet = null;
                stringBuilder.append(field.getDeclaringClass().getName());
                stringBuilder.append("#");
                stringBuilder.append(field.getName());
            } else {
                stringBuilder.append(Modifier.toString(method.getModifiers()));
                stringBuilder.append(" ");
                stringBuilder.append(stringBuilderRet);
                stringBuilderRet = null;
                stringBuilder.append(method.getDeclaringClass().getName());
                stringBuilder.append("#");
                stringBuilder.append(method.getName());
                stringBuilder.append("(");
                if (methodParameters != null) {
                    StringJoiner joiner = new StringJoiner(", ");
                    for (TypeInfo methodParameter : methodParameters) {
                        String toGenericParametersString = methodParameter.toGenericParametersString();
                        joiner.add(toGenericParametersString);
                    }
                    stringBuilder.append(
                            joiner
                    );
                }
                stringBuilder.append(")");
            }
            return stringBuilder.toString();
        }
    }

    private static ArrayList<TypeInfo> getMethodsRecursive(ClassInfo classInfo, String methodName, int depth, ArrayList<TypeInfo> seenMethods) throws NoSuchMethodException {
        if (depth > MAX_RECUSIVE_DEPTH) {
            throw new RuntimeException("OVERFLOW");
        }
        // we must search top-down for the method name
        ArrayList<TypeInfo> info = new ArrayList<>();
        ClassInfo sc = classInfo;
        while (sc != null) {
            Method[] m = sc.clazz.getDeclaredMethods();
            List<Method> m2 = new ArrayList<>();
            for (Method method2 : m) {
                if (!method2.isBridge() && !method2.isSynthetic() && method2.getName().contentEquals(methodName)) {
                    m2.add(method2);
                }
            }

            if (sc.isGeneric) {
                if (GLOBAL_OUTPUT) {
                    println("GENERIC CLASS: " + sc.clazz);
                    println("GENERIC CLASS MAP: " + sc.mapping);
                }
                for (Method method : m2) {
                    TypeInfo typeInfo = new TypeInfo(sc.mapping, method, sc.typeInfo);
                    boolean seen = false;
                    for (TypeInfo seenMethod : seenMethods) {
                        boolean matches = seenMethod.methodParametersEquals(typeInfo);
                        if (matches) {
                            seen = true;
                        }
                    }
                    if (!seen) {
                        if (GLOBAL_OUTPUT) {
                            println("ADDING GENERIC METHOD: " + typeInfo.toModifiedTypeString());
                        }
                        seenMethods.add(typeInfo);
                        sc.methods.add(typeInfo);
                    } else {
                        if (GLOBAL_OUTPUT) {
                            println("SKIPPING GENERIC METHOD: " + typeInfo.toModifiedTypeString());
                        }
                    }
                }
                info.addAll(sc.methods);

                if (sc.superinterfaces != null) {
                    // search superinterfaces
                    for (ClassInfo superinterface : sc.superinterfaces) {
                        if (GLOBAL_OUTPUT) {
                            println("INTERFACE: " + superinterface.clazz);
                        }
                        try {
                        info.addAll(getMethodsRecursive(superinterface, methodName, depth+1, seenMethods));
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                }
                // finally search superclass
                sc = sc.superclass;
                if (sc != null) {
                    if (GLOBAL_OUTPUT) {
                        println("SUPERCLASS: " + sc.clazz);
                    }
                }
            } else {
                if (!m2.isEmpty()) {
                    // we have one or more methods in the current sc
                    for (Method method : m2) {
                        Type r = method.getGenericReturnType();
                        if (containsTypeVariables(r)) {
                            // the method return type contains type variables
                            new NoSuchMethodException("CURRENT CLASS IS NOT GENERIC BUT WE FOUND TYPE VARIABLES: " + (sc.typeInfo != null ? sc.typeInfo : sc.clazz)).printStackTrace();
                            println();
                            return info;
                        } else {
                            Type[] genericParameterTypes = method.getGenericParameterTypes();
                            for (Type genericParameterType : genericParameterTypes) {
                                if (containsTypeVariables(genericParameterType)) {
                                    // a method parameter type (argument) contains type variables
                                    new NoSuchMethodException("CURRENT CLASS IS NOT GENERIC BUT WE FOUND TYPE VARIABLES: " + (sc.typeInfo != null ? sc.typeInfo : sc.clazz)).printStackTrace();
                                    println();
                                    return info;
                                }
                            }
                        }
                    }
                }
                if (GLOBAL_OUTPUT) {
                    println("STANDARD CLASS: " + sc.clazz);
                }

                TypeVariable<?>[] typeParameters = sc.clazz.getTypeParameters();
                for (int i = 0, typeParametersLength = typeParameters.length; i < typeParametersLength; i++) {
                    // if we enter this, the parent must have
                }

                for (Method method : m2) {
                    TypeInfo typeInfo = new TypeInfo(sc.mapping, method, sc.typeInfo);
                    boolean seen = false;
                    for (TypeInfo seenMethod : seenMethods) {
                        boolean matches = seenMethod.methodParametersEquals(typeInfo);
                        if (matches) {
                            seen = true;
                        }
                    }
                    if (!seen) {
                        if (GLOBAL_OUTPUT) {
                            println("ADDING STANDARD METHOD: " + typeInfo.toModifiedTypeString());
                        }
                        seenMethods.add(typeInfo);
                        sc.methods.add(typeInfo);
                    } else {
                        if (GLOBAL_OUTPUT) {
                            println("SKIPPING STANDARD METHOD: " + typeInfo.toModifiedTypeString());
                        }
                    }
                }
                info.addAll(sc.methods);
                if (sc.superinterfaces != null) {
                    // search superinterfaces
                    for (ClassInfo superinterface : sc.superinterfaces) {
                        if (GLOBAL_OUTPUT) {
                            println("INTERFACE: " + superinterface.clazz);
                        }
                        try {
                            info.addAll(getMethodsRecursive(superinterface, methodName, depth + 1, seenMethods));
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                }
                // finally search superclass
                sc = sc.superclass;
            }
        }
        if (info.isEmpty()) {
            throw new NoSuchMethodException(methodName);
        }
        return info;
    }

    private boolean methodParametersEquals(TypeInfo other) {
        if (methodParameters == null || other.methodParameters == null) {
            // matches only if both has no method parameters
            // otherwise fails since BOTH DO NOT have method parameters
            return methodParameters == null && other.methodParameters == null;
        }

        // both have method parameters
        if (methodParameters.size() != other.methodParameters.size()) {
            // incorrect parameter sizes
            return false;
        }
        for (int i = 0, methodParametersSize = methodParameters.size(); i < methodParametersSize; i++) {
            TypeInfo methodParameter = methodParameters.get(i);
            TypeInfo parameter = other.methodParameters.get(i);
            if (!methodParameter.type.equals(parameter.type)) {
                if (GLOBAL_OUTPUT) {
                    println("NON MATCHING TYPE: TYPE 1 <" + methodParameter.type + ">, TYPE 2 <" + parameter.type + ">");
                }
                return false;
            }
        }
        // all parameters match
        return true;
    }

    public Class<?> getType() {
        return type;
    }

    public int getTypeRank() {
        return typeRank;
    }

    public ArrayList<TypeInfo> getGenericParameters() {
        return genericParameters;
    }

    public ArrayList<TypeInfo> getMethodParameters() {
        return methodParameters;
    }

    public TypeInfo getReturnType() {
        return returnType;
    }

    public TypeVariable<?> getTypeVariable() {
        return typeVariables;
    }

    public WildcardType getWildcardType() {
        return wildcardType;
    }

    public TypeInfo getParent() {
        return parent;
    }

    /**
     * @param mapping the class root, must not be a generic class
     * @param field   a field that is part of the class root or part of the superclasses/superinterfaces of the class root
     */
    public TypeInfo(ArrayList<Pair<TypeVariable<?>, Object>> mapping, Field field, TypeInfo parent) {
        this.parent = parent;
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;
        if (GLOBAL_OUTPUT) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(byteArrayOutputStream);
        }

        this.field = field;

        TypeInfo t = new TypeInfo();
        String s = t.ResolveType(mapping, field.getGenericType(), 0, this);
        if (GLOBAL_OUTPUT) {
            printStream.println(s);
        }
        returnType = t;

        if (GLOBAL_OUTPUT) {
            debugString = byteArrayOutputStream.toString();
        } else {
            debugString = "";
        }
    }

    public TypeInfo(ArrayList<Pair<TypeVariable<?>, Object>> mapping, Method method, TypeInfo parent) {
        this.parent = parent;
        this.method = method;
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;
        if (GLOBAL_OUTPUT) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(byteArrayOutputStream);
            printStream.println("METHOD = " + method);
            printStream.println("GENERIC RETURN TYPE = " + method.getGenericReturnType());
        }

        TypeInfo t = new TypeInfo();
        String s = t.ResolveType(mapping, method.getGenericReturnType(), 0, this);
        if (GLOBAL_OUTPUT) {
            printStream.println(s);
        }
        returnType = t;

        Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length > 0) {
            methodParameters = new ArrayList<>();
            for (Type genericParameterType : genericParameterTypes) {
                TypeInfo t2 = new TypeInfo();
                String s2 = t2.ResolveType(mapping, genericParameterType, 0, this);
                if (GLOBAL_OUTPUT) {
                    printStream.println(s2);
                }
                methodParameters.add(t2);
            }
        }
        if (GLOBAL_OUTPUT) {
            debugString = byteArrayOutputStream.toString();
        } else {
            debugString = "";
        }
    }

    public TypeInfo(Type type, TypeInfo parent) {
        String s = ResolveType(null, type, 0, parent);
        if (GLOBAL_OUTPUT) {
            debugString = s;
        } else {
            debugString = "";
        }
    }

    TypeInfo() {
    }

    String ResolveType(ArrayList<Pair<TypeVariable<?>, Object>> mapping, Type type, int indent, TypeInfo parent) {
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;
        if (GLOBAL_OUTPUT) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(byteArrayOutputStream);
        }
        Pair<String, Exception> ex = ResolveTypeEX(mapping, type, indent, parent);
        if (GLOBAL_OUTPUT) {
            printStream.println(ex.first);
        }
        if (ex.second != null) {
            if (GLOBAL_OUTPUT) {
                printStream.println();
                printStream.println();
                printStream.flush();
                println(byteArrayOutputStream.toString());
            }
            if (ex.second instanceof RuntimeException) {
                throw ((RuntimeException) ex.second);
            }
            throw new RuntimeException(ex.second);
        }
        if (GLOBAL_OUTPUT) {
            printStream.flush();
            return byteArrayOutputStream.toString();
        } else {
            return "";
        }
    }

    public TypeVariable<?> getTypeVariables() {
        return typeVariables;
    }

    public TypeVariable<?>[] getTypeParameters() {
        return typeParameters;
    }

    Pair<String, Exception> ResolveTypeEX(ArrayList<Pair<TypeVariable<?>, Object>> mapping, Type type, int indent, TypeInfo parent) {
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;
        if (GLOBAL_OUTPUT) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(byteArrayOutputStream);
        }
        if (parent != this) {
            this.parent = parent;
        }
        if (type == null) {
            return new Pair<>("", null);
        }
        Exception ex = null;
        try {
            if (indent > MAX_RECUSIVE_DEPTH) {
                throw new Exception("OVERFLOW");
            }
            if (GLOBAL_OUTPUT) {
                printStream.println();
                printStream.println(indent(indent) + "type = " + type);
                printStream.println(indent(indent) + "type is ParameterizedType = " + (type instanceof ParameterizedType));
                printStream.println(indent(indent) + "type is GenericArrayType = " + (type instanceof GenericArrayType));
                printStream.println(indent(indent) + "type is WildcardType = " + (type instanceof WildcardType));
                printStream.println(indent(indent) + "type is TypeVariable = " + (type instanceof TypeVariable<?>));
                printStream.println(indent(indent) + "type is Class = " + (type instanceof Class<?>));
            }
            if (type instanceof ParameterizedType) {
                // we have type parameters
                Type rawType = ((ParameterizedType) type).getRawType();
                this.type = (Class<?>) rawType;
                for (Type type1 : ((ParameterizedType) type).getActualTypeArguments()) {
                    if (GLOBAL_OUTPUT) {
                        printStream.print("GENERIC:");
                    }
                    TypeInfo t = new TypeInfo();
                    Pair<String, Exception> ex2 = t.ResolveTypeEX(mapping, type1, indent + 1, this);
                    if (GLOBAL_OUTPUT) {
                        printStream.print(ex2.first);
                    }
                    if (ex2.second != null) {
                        if (GLOBAL_OUTPUT) {
                            printStream.flush();
                            return new Pair<>(byteArrayOutputStream.toString(), ex2.second);
                        } else {
                            return new Pair<>("", ex2.second);
                        }
                    }
                    if (GLOBAL_OUTPUT) {
                        printStream.println();
                        printStream.println("GENERIC END:");
                    }
                    if (this.genericParameters == null) {
                        this.genericParameters = new ArrayList<>();
                    }
                    this.genericParameters.add(t);
                }
            } else if (type instanceof GenericArrayType) {
                // we have a generic array, Type[]... or Type ...
                Type type1 = type;
                while (type1 instanceof GenericArrayType) {
                    typeRank++;
                    type1 = ((GenericArrayType) type1).getGenericComponentType();
                }
                Pair<String, Exception> ex1 = ResolveTypeEX(mapping, type1, indent + 1, this);
                if (GLOBAL_OUTPUT) {
                    printStream.print(ex1.first);
                }
                if (ex1.second != null) {
                    if (GLOBAL_OUTPUT) {
                        printStream.flush();
                        return new Pair<>(byteArrayOutputStream.toString(), ex1.second);
                    } else {
                        return new Pair<>("", ex1.second);
                    }
                }
            } else if (type instanceof Class<?>) {
                Class<?> type1 = (Class<?>) type;
                if (type1.isArray()) {
                    while (type1.isArray()) {
                        this.typeRank++;
                        type1 = type1.getComponentType();
                    }
                    Pair<String, Exception> ex1 = ResolveTypeEX(mapping, type1, indent + 1, this);
                    if (GLOBAL_OUTPUT) {
                        printStream.print(ex1.first);
                    }
                    if (ex1.second != null) {
                        if (GLOBAL_OUTPUT) {
                            printStream.flush();
                            return new Pair<>(byteArrayOutputStream.toString(), ex1.second);
                        } else {
                            return new Pair<>("", ex1.second);
                        }
                    }
                } else {
                    this.type = type1;
                    typeParameters = type1.getTypeParameters();
                }
            } else if (type instanceof TypeVariable<?>) {
                if (GLOBAL_OUTPUT) {
                    println("TYPE VARIABLE: " + type);
                }
                if (mapping == null) {
                    throw new RuntimeException("Cannot resolve TypeVariable " + type + ", no mapping has been supplied");
                }
                boolean found = false;
                for (Pair<TypeVariable<?>, Object> typeVariableTypePair : mapping) {
                    if (typeVariableTypePair.first.equals(type)) {
                        found = true;
                        if (typeVariableTypePair.second instanceof TypeInfo) {
                            TypeInfo second = (TypeInfo) typeVariableTypePair.second;
                            if (GLOBAL_OUTPUT) {
                                printStream.println(indent(indent) + "TYPE VARIABLE MATCH: " + second.toModifiedTypeString());
                            }
                            if (mapping.size() == 1) {
                                imprint(second);
                            } else {
//                                throw new RuntimeException("Cannot yet resolve the following mappings:  more than one T=TypeInfo,   one T=TypeInfo and one or more Type, INFO: " + second.toDetailedString());
                                imprint(second);
                            }
                        } else {
                            Type second = (Type) typeVariableTypePair.second;
                            if (GLOBAL_OUTPUT) {
                                printStream.println(indent(indent) + "TYPE VARIABLE MATCH: " + second);
                            }
                            Pair<String, Exception> ex1 = ResolveTypeEX(mapping, second, indent + 1, this);
                            if (GLOBAL_OUTPUT) {
                                printStream.print(ex1.first);
                            }
                            if (ex1.second != null) {
                                if (GLOBAL_OUTPUT) {
                                    printStream.flush();
                                    return new Pair<>(byteArrayOutputStream.toString(), ex1.second);
                                } else {
                                    return new Pair<>("", ex1.second);
                                }
                            }
                        }
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Cannot resolve TypeVariable " + type + "\nA mapping has been supplied but " + type + " was not found inside it.\n" +
                            "This can be caused by attempting to resolve a generics method:\n" +
                            "    <T> T foo(T param) { // generics method\n" +
                            "        // body of method\n" +
                            "        return param;\n" +
                            "    }\n" +
                            "    void testFoo() { // usage of generics method\n" +
                            "        // body of method\n" +
                            "        String explicit = this.<String>foo(\"hello\");\n" +
                            "        String implicit1 = this.foo(\"hello\");\n" +
                            "        String implicit2 = foo(\"hello\");\n" +
                            "    }\n" +
                            "Where the generic parameter can only be verified by inspecting the source code, typically the body of a method.\n" +
                            "We do not support decompilation of any kind of bytecode.");
                }
//
//                this.genericParameters = new ArrayList<>();
//                boolean ln = false;
//                for (Type bound : bounds) {
//                    if (bound == null) continue;
//                    TypeInfo t = new TypeInfo();
//                    if (!ln) {
//                        printStream.println();
//                        ln = true;
//                    }
//                    printStream.print("GENERIC:");
//                    Pair<String, Exception> ex1 = ResolveTypeEX(mapping, t, bound, indent + 1);
//                    printStream.print(ex1.first);
//                    if (ex1.second != null) {
//                        printStream.flush();
//                        return new Pair<>(byteArrayOutputStream.toString(), ex1.second);
//                    }
//                    printStream.println();
//                    printStream.println("GENERIC END:");
//                    this.genericParameters.add(t);
//                }
            } else if (type instanceof WildcardType) {
                wildcardType = (WildcardType) type;
                isWildcard_ = true;
                Type[] lowerBounds = wildcardType.getLowerBounds();
                Type[] upperBounds = wildcardType.getUpperBounds();
                if (GLOBAL_OUTPUT) {
                    printStream.println();
                    printStream.println(indent(indent) + "TYPE WILDCARD LOWER BOUNDS: " + Arrays.toString(lowerBounds));
                    printStream.print(indent(indent) + "TYPE WILDCARD UPPER BOUNDS: " + Arrays.toString(upperBounds));
                }

                // ? extends X   = LOWER [null] UPPER [X]
                // ? super   X   = LOWER [X] UPPER [Object]

                if (GLOBAL_OUTPUT) {
                    printStream.print("GENERIC:");
                }
                TypeInfo t = new TypeInfo();
                Pair<String, Exception> ex1 = t.ResolveTypeEX(mapping, lowerBounds.length == 0 ? upperBounds[0] : lowerBounds[0], indent + 1, this);
                if (GLOBAL_OUTPUT) {
                    printStream.print(ex1.first);
                }
                if (ex1.second != null) {
                    if (GLOBAL_OUTPUT) {
                        printStream.flush();
                        return new Pair<>(byteArrayOutputStream.toString(), ex1.second);
                    } else {
                        return new Pair<>("", ex1.second);
                    }
                }
                if (GLOBAL_OUTPUT) {
                    printStream.println();
                    printStream.println("GENERIC END:");
                }
                if (this.genericParameters == null) {
                    this.genericParameters = new ArrayList<>();
                }
                this.genericParameters.add(t);
            }
        } catch (Exception e) {
            ex = e;
        }
        if (GLOBAL_OUTPUT) {
            printStream.flush();
            return new Pair<>(byteArrayOutputStream.toString(), ex);
        } else {
            return new Pair<>("", ex);
        }
    }

    void imprint(TypeInfo from) {
        this.genericParameters = from.genericParameters;
        this.debugString = from.debugString;
        this.methodParameters = from.methodParameters;
        this.type = from.type;
        this.returnType = from.returnType;
        this.field = from.field;
        this.interfaces = from.interfaces;
        this.superclass = from.superclass;
        this.typeVariables = from.typeVariables;
        this.isWildcard_ = from.isWildcard_;
        this.isTypeVariable_ = from.isTypeVariable_;
        this.typeRank = from.typeRank;
        this.selfBound = from.selfBound;
        this.wildcardType = from.wildcardType;
        this.typeParameters = from.typeParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TypeInfo typeInfo = (TypeInfo) o;

        if (typeRank != typeInfo.typeRank) {
            return false;
        }
        if (isTypeVariable_ != typeInfo.isTypeVariable_) {
            return false;
        }
        if (isWildcard_ != typeInfo.isWildcard_) {
            return false;
        }
        if (selfBound != typeInfo.selfBound) {
            return false;
        }
        if (!Objects.equals(field, typeInfo.field)) {
            return false;
        }
        if (!Objects.equals(method, typeInfo.method)) {
            return false;
        }
        if (!Objects.equals(type, typeInfo.type)) {
            return false;
        }
        if (!Objects.equals(genericParameters, typeInfo.genericParameters)) {
            return false;
        }
        if (!Objects.equals(methodParameters, typeInfo.methodParameters)) {
            return false;
        }
        if (!Objects.equals(returnType, typeInfo.returnType)) {
            return false;
        }
        if (!Objects.equals(typeVariables, typeInfo.typeVariables)) {
            return false;
        }
        if (!Objects.equals(wildcardType, typeInfo.wildcardType)) {
            return false;
        }
        if (!Objects.equals(debugString, typeInfo.debugString)) {
            return false;
        }
        if (!Objects.equals(superclass, typeInfo.superclass)) {
            return false;
        }
        if (!Objects.equals(interfaces, typeInfo.interfaces)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(typeParameters, typeInfo.typeParameters);
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + typeRank;
        result = 31 * result + (genericParameters != null ? genericParameters.hashCode() : 0);
        result = 31 * result + (methodParameters != null ? methodParameters.hashCode() : 0);
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        result = 31 * result + (isTypeVariable_ ? 1 : 0);
        result = 31 * result + (typeVariables != null ? typeVariables.hashCode() : 0);
        result = 31 * result + (isWildcard_ ? 1 : 0);
        result = 31 * result + (wildcardType != null ? wildcardType.hashCode() : 0);
        result = 31 * result + (selfBound ? 1 : 0);
        result = 31 * result + (debugString != null ? debugString.hashCode() : 0);
        result = 31 * result + (superclass != null ? superclass.hashCode() : 0);
        result = 31 * result + (interfaces != null ? interfaces.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(typeParameters);
        return result;
    }

    static boolean containsTypeVariables(Type type) {
        return containsTypeVariables(type, 0);
    }

    static boolean containsTypeVariables(Type type, int indent) {
        if (type == null) {
            return false;
        }
        if (indent > MAX_RECUSIVE_DEPTH) {
            throw new RuntimeException("OVERFLOW");
        }
        if (type instanceof ParameterizedType) {
            // we have type parameters
            for (Type type1 : ((ParameterizedType) type).getActualTypeArguments()) {
                if (containsTypeVariables(type1, indent + 1)) {
                    return true;
                }
            }
        } else if (type instanceof GenericArrayType) {
            // we have a generic array, Type[]... or Type ...
            Type type1 = type;
            while (type1 instanceof GenericArrayType) {
                type1 = ((GenericArrayType) type1).getGenericComponentType();
            }
            return containsTypeVariables(type1, indent + 1);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType1 = (WildcardType) type;
            Type[] lowerBounds = wildcardType1.getLowerBounds();
            Type[] upperBounds = wildcardType1.getUpperBounds();
            // ? extends X   = LOWER [null] UPPER [X]
            // ? super   X   = LOWER [X] UPPER [Object]
            for (Type lowerBound : lowerBounds) {
                if (containsTypeVariables(lowerBound)) {
                    return true;
                }
            }
            for (Type upperBound : upperBounds) {
                if (containsTypeVariables(upperBound)) {
                    return true;
                }
            }
        } else if (!(type instanceof Class<?>)) {
            if (type instanceof TypeVariable<?>) {
                return true;
            } else {
                throw new RuntimeException("UNKNOWN TYPE: [CLASS: " + type.getClass() + "], TYPE: " + type);
            }
        }
        return false;
    }

    /**
     * @throws RuntimeException if self bound is encountered or class was not found in classes
     */
    public void containsClasses(Class<?> ... classes) {
        if (!isType()) {
            // we are a field or a method
            getReturnType().containsClasses(this, classes);
            if (isField()) {
                return;
            }
            for (TypeInfo methodParameter : getMethodParameters()) {
                methodParameter.containsClasses(this, classes);
            }
            return;
        }
        // we are a type
        if (isSelfBound()) {
            // we are self bound
            throw new RuntimeException("SELF BOUND\nsrc: " + this);
        }
        if (isTypeVariable()) {
            // <T extends> or <T super>
            if (isGeneric()) {
                ArrayList<TypeInfo> genericParameters = getGenericParameters();
                for (TypeInfo genericParameter : genericParameters) {
                    genericParameter.containsClasses(this, classes);
                }
            } else {
                // we have <T>
                matches(this, Object.class, classes);
            }
        } else if (isWildcard()) {
            // <? extends> or <? super>
            if (isGeneric()) {
                ArrayList<TypeInfo> genericParameters = getGenericParameters();
                for (TypeInfo genericParameter : genericParameters) {
                    genericParameter.containsClasses(this, classes);
                }
            } else {
                // we have <?>
                matches(this, Object.class, classes);
            }
        } else {
            if (isGeneric()) {
                ArrayList<TypeInfo> genericParameters = getGenericParameters();
                for (TypeInfo genericParameter : genericParameters) {
                    genericParameter.containsClasses(this, classes);
                }
            }
            matches(this, getType(), classes);
        }
    }

    void containsClasses(TypeInfo src, Class<?> ... classes) {
        if (!isType()) {
            // we are a field or a method
            getReturnType().containsClasses(src, classes);
            if (isField()) {
                return;
            }
            for (TypeInfo methodParameter : getMethodParameters()) {
                methodParameter.containsClasses(src, classes);
            }
            return;
        }
        // we are a type
        if (isSelfBound()) {
            // we are self bound
            matches(src, type, classes);
        }
        if (isTypeVariable()) {
            // <T extends> or <T super>
            if (isGeneric()) {
                ArrayList<TypeInfo> genericParameters = getGenericParameters();
                for (TypeInfo genericParameter : genericParameters) {
                    genericParameter.containsClasses(src, classes);
                }
            } else {
                // we have <T>
                matches(src, Object.class, classes);
            }
        } else if (isWildcard()) {
            // <? extends> or <? super>
            if (isGeneric()) {
                ArrayList<TypeInfo> genericParameters = getGenericParameters();
                for (TypeInfo genericParameter : genericParameters) {
                    genericParameter.containsClasses(src, classes);
                }
            } else {
                // we have <?>
                matches(src, Object.class, classes);
            }
        } else {
            if (isGeneric()) {
                ArrayList<TypeInfo> genericParameters = getGenericParameters();
                for (TypeInfo genericParameter : genericParameters) {
                    genericParameter.containsClasses(src, classes);
                }
            }
            // what are we?
            if (type == null) {
                throw new RuntimeException(src.toDetailedString());
            }
            matches(src, getType(), classes);
        }
    }

    void matches(TypeInfo src, Class<?> T, Class<?>[] classes) {
        for (Class<?> aClass : classes) {
            if (T.equals(aClass)) {
                return;
            }
        }
        throw new RuntimeException("could not find class matching " + T + "\navailable classes: " + Arrays.toString(classes) + "\nsrc: " + src);
    }

    public boolean isType() {
        return !isField() && !isMethod();
    }

    public boolean isField() {
        return field != null;
    }
    public boolean isMethod() {
        return method != null;
    }
    public boolean isArray() {
        return typeRank != 0;
    }
    public boolean isGeneric() {
        return genericParameters != null || (type != null && type.getTypeParameters().length != 0);
    }

    public boolean isTypeVariable() {
        return isTypeVariable_;
    }

    public boolean isWildcard() {
        return isWildcard_;
    }

    public boolean isSelfBound() {
        return selfBound;
    }

    public String toString() {
        return toModifiedTypeString();
    }

    public String toDetailedString() {
        return toDetailedString(0);
    }

    String toDetailedString(int indent) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        printStream.println();
        printStream.println(indent(indent) + "TypeInfo {");
        indent++;
        if (type != null) {
            printStream.println(indent(indent) + "type: " + toModifiedTypeString());
            if (typeParameters != null && typeParameters.length > 0) {
                printStream.println(indent(indent) + "type parameters: " + Arrays.toString(typeParameters));
            }
        }
        if (isGeneric()) {
            printStream.println(indent(indent) + "isGeneric: true");
        }
        if (isField()) {
            printStream.println(indent(indent) + "field: " + toModifiedTypeString());
            if (returnType != null) {
                printStream.println(indent(indent) + "field type: " + returnType.toDetailedString(indent + 1));
            }
        } else if (isMethod()) {
            printStream.println(indent(indent) + "method: " + toModifiedTypeString());
            if (returnType != null) {
                printStream.println(indent(indent) + "method return type: " + returnType.toDetailedString(indent + 1));
            }
            if (methodParameters != null) {
                StringBuilder acc = new StringBuilder();
                for (TypeInfo a : methodParameters) {
                    String s = a.toDetailedString(indent + 1);
                    acc.append(s);
                }
                printStream.println(indent(indent) + "method parameters: " + methodParameters.size() + acc);
            }
        } else {
            if (isTypeVariable()) {
                printStream.println(indent(indent) + "typeVariable: " + true);
            }
            if (isSelfBound()) {
                printStream.println(indent(indent) + "self bound: " + true);
            }
            if (isWildcard()) {
                printStream.println(indent(indent) + "wildcard: " + true);
            }
            if (isArray()) {
                printStream.println(indent(indent) + "typeRank: " + typeRank);
            }
            if (genericParameters != null) {
                StringBuilder acc = new StringBuilder();
                for (TypeInfo a : genericParameters) {
                    String s = a.toDetailedString(indent + 1);
                    acc.append(s);
                }
                printStream.println(indent(indent) + "genericParameters: " + genericParameters.size() + acc);
            }
            if (superclass != null) {
                printStream.println(indent(indent) + "superclass: " + superclass.toDetailedString(indent+1));
            }
            if (interfaces != null) {
                StringBuilder acc = new StringBuilder();
                for (TypeInfo a : interfaces) {
                    String s = a.toDetailedString(indent + 1);
                    acc.append(s);
                }
                printStream.println(indent(indent) + "interfaces: " + interfaces.size() + acc);
            }
        }
        indent--;
        printStream.print(indent(indent) + "}");
        printStream.flush();
        return byteArrayOutputStream.toString();
    }

    private String indent(int indent) {
        String INDENT_STRING = "  ";
        switch (indent) {
            case 0:
                return "";
            case 1:
                return INDENT_STRING;
            default:
                StringBuilder acc = new StringBuilder();
                for (int i = 0; i < indent; i++) {
                    acc.append(INDENT_STRING);
                }
                return acc.toString();
        }
    }

    public void printDetailed() {
        System.out.println(this.toDetailedString());
    }

    public void printDebug() {
        if (debugString != null) {
            System.out.println(debugString);
        }
    }

    public void printDetailedWithDebug() {
        printDebug();
        printDetailed();
    }

    public void printModifiedTypeString() {
        System.out.println(toModifiedTypeString());
    }

    public void printModifiedTypeStringWithDebug() {
        printDebug();
        printModifiedTypeString();
    }
}
