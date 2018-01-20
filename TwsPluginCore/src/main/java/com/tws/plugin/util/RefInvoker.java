package com.tws.plugin.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import qrom.component.log.QRomLog;

@SuppressWarnings("unchecked")
public class RefInvoker {
	private static final String TAG = "rick_Print:RefInvoker";

	private static final ClassLoader system = ClassLoader.getSystemClassLoader();
	private static final ClassLoader bootloader = system.getParent();
	private static final ClassLoader application = RefInvoker.class.getClassLoader();

	private static HashMap<String, Class> clsCache = new HashMap<String, Class>();

	public static Class forName(String clsName) throws ClassNotFoundException {
		Class cls = clsCache.get(clsName);
		if (cls == null) {
			cls = Class.forName(clsName);
			ClassLoader cl = cls.getClassLoader();
			if (cl == system || cl == application || cl == bootloader) {
				clsCache.put(clsName, cls);
			}
		}
		return cls;
	}

	public static Object newInstance(String className, Class[] paramTypes, Object[] paramValues) {
		try {
			Class cls = forName(className);
			Constructor constructor = cls.getConstructor(paramTypes);
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance(paramValues);
		} catch (ClassNotFoundException e) {
			QRomLog.e(TAG, "ClassNotFoundException", e);
		} catch (NoSuchMethodException e) {
			QRomLog.e(TAG, "NoSuchMethodException", e);
		} catch (IllegalAccessException e) {
			QRomLog.e(TAG, "IllegalAccessException", e);
		} catch (InstantiationException e) {
			QRomLog.e(TAG, "InstantiationException", e);
		} catch (InvocationTargetException e) {
			QRomLog.e(TAG, "InvocationTargetException", e);
		}
		return null;
	}

	public static Object invokeMethod(Object target, String className, String methodName, Class[] paramTypes,
			Object[] paramValues) {

		try {
			Class cls = forName(className);
			return invokeMethod(target, cls, methodName, paramTypes, paramValues);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object invokeMethod(Object target, Class cls, String methodName, Class[] paramTypes,
			Object[] paramValues) {
		try {
			Method method = cls.getDeclaredMethod(methodName, paramTypes);
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			return method.invoke(target, paramValues);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static Object getField(Object target, String className, String fieldName) {
		try {
			Class cls = forName(className);
			return getField(target, cls, fieldName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static Object getField(Object target, Class cls, String fieldName) {
		try {
			Field field = cls.getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			return field.get(target);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// try supper for Miui, Miui has a class named MiuiPhoneWindow
			try {
				Field field = cls.getSuperclass().getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(target);
			} catch (Exception superE) {
				e.printStackTrace();
				superE.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;

	}

	@SuppressWarnings("rawtypes")
	public static void setField(Object target, String className, String fieldName, Object fieldValue) {
		try {
			Class cls = forName(className);
			setField(target, cls, fieldName, fieldValue);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void setField(Object target, Class cls, String fieldName, Object fieldValue) {
		try {
			Field field = cls.getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(target, fieldValue);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// try supper for Miui, Miui has a class named MiuiPhoneWindow
			try {
				Field field = cls.getSuperclass().getDeclaredField(fieldName);
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}
				field.set(target, fieldValue);
			} catch (Exception superE) {
				e.printStackTrace();
				// superE.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static Method findMethod(Object object, String methodName, Class[] clses) {
		try {
			return object.getClass().getDeclaredMethod(methodName, clses);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Method findMethod(Object object, String methodName, Object[] args) {
		if (args == null) {
			try {
				return object.getClass().getDeclaredMethod(methodName, (Class[]) null);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			return null;
		} else {
			Method[] methods = object.getClass().getDeclaredMethods();
			boolean isFound = false;
			Method method = null;
			for (Method m : methods) {
				if (m.getName().equals(methodName)) {
					Class<?>[] types = m.getParameterTypes();
					if (types.length == args.length) {
						isFound = true;
						for (int i = 0; i < args.length; i++) {
							if (!(types[i] == args[i].getClass() || (types[i].isPrimitive() && primitiveToWrapper(types[i]) == args[i]
									.getClass()))) {
								isFound = false;
								break;
							}
						}
						if (isFound) {
							method = m;
							break;
						}
					}
				}
			}
			return method;
		}
	}

	private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<Class<?>, Class<?>>();

	static {
		primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
		primitiveWrapperMap.put(Byte.TYPE, Byte.class);
		primitiveWrapperMap.put(Character.TYPE, Character.class);
		primitiveWrapperMap.put(Short.TYPE, Short.class);
		primitiveWrapperMap.put(Integer.TYPE, Integer.class);
		primitiveWrapperMap.put(Long.TYPE, Long.class);
		primitiveWrapperMap.put(Double.TYPE, Double.class);
		primitiveWrapperMap.put(Float.TYPE, Float.class);
		primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
	}

	static Class<?> primitiveToWrapper(final Class<?> cls) {
		Class<?> convertedClass = cls;
		if (cls != null && cls.isPrimitive()) {
			convertedClass = primitiveWrapperMap.get(cls);
		}
		return convertedClass;
	}

}