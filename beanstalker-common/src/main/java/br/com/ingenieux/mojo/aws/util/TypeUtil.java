package br.com.ingenieux.mojo.aws.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeUtil {
        private TypeUtil() {
        }

        public static Class<?> getRealClass(Class<?> c) {
                while (c.getName().contains("$$"))
                        c = c.getSuperclass();
                return c;
        }

        public static Type[] getTypes(Class<?> c) {
                Class<?> realClass = getRealClass(c);

                return ((ParameterizedType) realClass.getGenericSuperclass()).getActualTypeArguments();
        }

}