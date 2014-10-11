package br.com.ingenieux.mojo.aws.util;

import java.lang.reflect.ParameterizedType;

public class TypeUtil {

  private TypeUtil() {
  }

  public static Class<?> getServiceClass(Class<?> c) {
    Class<?> prevClass = c;

    while (0 == c.getTypeParameters().length) {
      prevClass = c;
      c = c.getSuperclass();
    }

    return (Class<?>) ((ParameterizedType) prevClass.getGenericSuperclass())
        .getActualTypeArguments()[0];
  }

}