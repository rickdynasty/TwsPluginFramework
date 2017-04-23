# TwsPluginFramework插件框架

TwsPluginFramework(下面简称TPF框架)能很好的解决大中项目团队的队协作问题，实现模块解耦、并行开发、模块动态更新，适用于Android 4.3以上系统版本的应用开发。


**开始使用TwsPluginFramework框架**

- [1. 使用TPF框架](#1)
- [2. 开发插件应用的工程配置](#2)
- [3. 非独立插件应用配置](#3)

## 1. 使用TPF框架
	Step 1. clone工程到本地
		【其实你只需要里面的sdk】

	Step 2. 引用twsplugincore.jar
		然后将sdk目录下面的twsplugincore.jar引用到工程里面，同时将主工程的Application继承PluginApplication。
	
	Step 3. 配置AndroidManifest.xml【这里以TwsPluginHost为案例】
		①、配置两个框架需要的Provider：

		<!-- core -->
        <provider
            android:name="com.tws.plugin.manager.PluginManagerProvider"
            android:authorities="com.tencent.tws.pluginhost.manager"
            android:exported="false" />
        <provider
            android:name="com.tws.plugin.servicemanager.ServiceProvider"
            android:authorities="com.tencent.tws.pluginhost.svcmgr"
            android:exported="false" />

		②、然后申明预备的组件Receiver(注册1个即可)、service[配置多个，同时需要配置多个不同进程的]、activity[配置多个、同时需要配置多个不同Mode的]，框架通过action来查询。
			普通组件的规则：
		    <intent-filter>
                <action android:name="com.tencent.tws.pluginhost.STUB_DEFAULT" />

                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
	
			独立进程Service组件的申明规则：
			<intent-filter>
                <action android:name="com.tencent.tws.pluginhost.MP_STUB_DEFAULT" />

                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
			*独立进程Service的进程名配置规则：android:process=":plugin【 + 后缀(建议用数字)】"

		③、配置工程中显示插件fragment的容器：			
        	<!-- 要展示插件中的fragment -->
        	<activity
            	android:name=".plugindebug.PluginFragmentActivity"
            	android:screenOrientation="portrait" />
       		<!-- 要展示插件中的（Tws）fragment -->
        	<activity
            	android:name=".plugindebug.PluginTwsFragmentActivity"
            	android:screenOrientation="portrait" /> 

## 2. 开发插件应用的工程配置
	step 1.更换aapt
	当前暂时只编译5.1的aapt【在TwsPluginFramework\sdk\TwsWidgetTools\5.1\aapt.exe or aapt】
	step 2.在工程目录下添加tws.properties文件
		文件指定了工程的资源pid 和 packagename，如下：
		package_id=0x5e	#根据工程要求取值[0x02~0x7e]
		packagename=com.example.plugindemo	#工程的包名]

## 3. 非独立插件应用配置
	Step 1. 指定宿主包名
		插件如果要使用宿主的共享功能(代码/资源)，需要显示的指定宿主包名，这样框架就会在构建插件的ClassLoader和Resources的时候就会将宿主的构建进插件里面。

		android:sharedUserId="com.tencent.tws.pluginhost"

	Step 2. 配置可见的fragment
		<!-- 通知插件框架哪些fragment是可以嵌入宿主Activity的，如果fragment只是插件内使用，无需对外暴露则无需配置 -->
        <exported-fragment
            android:name="some_id_for_fragment3"
            android:value="com.example.plugindemo.fragment.PluginSpecTwsFragment" />