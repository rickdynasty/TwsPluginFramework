# TwsPluginFramework插件框架使用

**目录**

- [1. TPF简介](#1)
- [2. 操作步骤](#2)
  - [2.1 替换掉环境下的aapt](#21)
  - [2.2 指定输出工程的pid等信息](#22)
  - [2.3 引入框架module](#22)
  - [2.4 主工程的Application继承PluginApplication](#23)
- [3. 其他](#3)
  - [3.1 替换掉环境下的aapt](#21)
  - [3.2 other](#22)

## 1. TPF简介
	(略)
TwsPluginFramework github库结构

 - 【doc】内部或外部分享的一些文档，仅供参考
 - 【sdk】里面存放了一个25.0.3的aapt，你需要用这个aapt替换掉你开发环境下"sdk\build-tools\"里面用到的aapt【修改aapt的为了指定工程资源的pid】。
 - 【TwsPluginCore】**真正的插件框架module,你要的module就只有这一个。**
 - 【TwsPluginDemo】TPF的插件module[插件示例]
 - 【TwsPluginHost】TPF的宿主module[宿主示例]
 - 【TwsPluginShareLib】 TPF的共享库module[共享库示例]，宿主提供给插件的共享内容，需要编译成jar 输出给插件工程开发者使用。
 - 【config.gradle】 TPF工程的base config

## 2. 操作步骤

### 2.1 替换掉环境下的aapt
	将sdk目录下的aapt（.exe）拷贝替换掉你本地环境下buildtools里面的aapt。

### 2.2 指定输出工程的pid等信息
	在工程module下的build.gradle里添加需要指定的报名信息和

    aaptOptions {
        additionalParameters '--package-name',
                'com.tencent.tws.pluginhost',
                '--forced-package-id',
                '0x6f'
    }

### 2.3 引入框架module
	将框架TwsPluginCore引入进你的主工程里面，并将主工程依赖TwsPluginCore。【这个后面会直接放到gradle的库里面，当前暂时这样吧，方便各位研究和定制】

### 2.4 主工程的Application继承[插件框架TwsPluginCore的]PluginApplication
	[插件框架TwsPluginCore的]PluginApplication，在onCreate()里面做一些你该做的内容，参考TwsPluginHost的HostApplication。

## 3. 其他
### 3.1 工程module - TwsPluginShareLib
	这个不是框架的内容，是宿主将自己的一部分能力分离出来给插件共享的，这样插件就不用重复开发。
### 3.2 other
	其实也没什好写了，重点是你得先看看doc里面的文档，结合系统处理应用的思想了解清楚插件的原理。其他的都是一些零星琐碎的小点。

有兴趣的可以先看文档，了解清楚原理，然后在看代码。

- **Android 插件技术实战总结:** https://mp.weixin.qq.com/s/1p5Y0f5XdVXN2EZYT0AM_A
- 分享文档：doc/Android插件技术.pdf ![](doc/Android插件技术.pdf)

如果你公司须要用这套插件框架并且存在很多不清楚的地方，深圳地区可直接联系我，可考虑做一个简单的分享。

个人微信，琐事勿扰，谢谢！![](doc/img/rick.png)
