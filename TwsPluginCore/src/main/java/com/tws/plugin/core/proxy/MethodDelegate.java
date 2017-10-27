package com.tws.plugin.core.proxy;

import java.lang.reflect.Method;

/**
 * @author yongchen
 */
public abstract class MethodDelegate {

	public Object beforeInvoke(Object target, Method method, Object[] args) {
		return null;
	}

	public Object afterInvoke(Object target, Method method, Object[] args, Object beforeInvoke, Object invokeResult) {
		if (beforeInvoke != null) {
			return beforeInvoke;
		}
		return invokeResult;
	}

}
