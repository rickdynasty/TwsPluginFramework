package com.tws.plugin.sharelib;

import java.io.Serializable;

/**
 * 仅仅用来测试插件程序中Intent是否可以使用宿主程序中的VO
 * 
 * @author yongchen
 * 
 */
public class SharePOJO implements Serializable {

	public SharePOJO(String name) {
		this.name = name;
	}

	public String name;
}
