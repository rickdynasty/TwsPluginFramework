package com.example.plugindemo.vo;

import java.io.Serializable;

/**
 * @author yongchen
 */
public class ParamVO implements Serializable {
    public String name;

    @Override
    public String toString() {
        return "name:" + name;
    }
}
