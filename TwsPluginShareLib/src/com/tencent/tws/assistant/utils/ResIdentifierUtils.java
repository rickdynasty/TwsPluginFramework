package com.tencent.tws.assistant.utils;

import java.lang.reflect.Field;

import android.content.res.Resources;

import com.tencent.tws.framework.utils.HostProxy;

public class ResIdentifierUtils {
	private static Resources sApplicationRes = null;

	private static void ensureRes() {
		if (sApplicationRes == null) {
			sApplicationRes = HostProxy.getApplication().getResources();
		}
	}

	public static int getSysId(String name) {
		ensureRes();
		if (sApplicationRes == null)
			return 0;

		return sApplicationRes.getIdentifier(name, "id", "android");
	}

	public static int getSysAttrId(String name) {
		ensureRes();
		if (sApplicationRes == null)
			return 0;

		return sApplicationRes.getIdentifier(name, "attr", "android");
	}

	public static int getSysBoolId(String name) {
		ensureRes();
		if (sApplicationRes == null)
			return 0;

		return sApplicationRes.getIdentifier(name, "bool", "android");
	}

	public static int getSysDimenId(String name) {
		ensureRes();
		if (sApplicationRes == null)
			return 0;

		return sApplicationRes.getIdentifier(name, "dimen", "android");
	}

	public static int getSysLayoutId(String name) {
		ensureRes();
		if (sApplicationRes == null)
			return 0;

		return sApplicationRes.getIdentifier(name, "layout", "android");
	}

	public static int getSysStringId(String name) {
		ensureRes();
		if (sApplicationRes == null)
			return 0;

		return sApplicationRes.getIdentifier(name, "string", "android");
	}

	public static int[] getSysStyleableId(String name) {
		String className = "com.android.internal.R";
		try {
			Class<?> cls = Class.forName(className);
			for (Class<?> childClass : cls.getClasses()) {
				String simple = childClass.getSimpleName();
				if (simple.equals("styleable")) {
					for (Field field : childClass.getFields()) {
						String fieldName = field.getName();
						if (fieldName.equals(name)) {
							System.out.println(fieldName);
							return (int[]) field.get(null);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
