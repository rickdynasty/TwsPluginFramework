package qrom.component.log;

import android.util.SparseArray;

/**
 * 配置Log输出参数的配置基类.
 *
 * <p>
 * <b>tips:</b>
 * <p>
 *  1. <em><strong>QRomLogBaseConfig</strong></em>是抽象类，不能直接初始化。<br/>
 *  2. 在项目中配置Log参数时，需要在 <b>src/qrom/component/config</b> 中声明 <b>QRomLogConfig.java</b> 继承于<em><strong>QRomLogBaseConfig</strong></em>，然后实现所有的abstract抽象方法，配置所需的参数。<br/>
 *  3. 如果在项目中缺少 <b>QRomLogConfig.java</b> ，会发生RuntimeException。
 *  <p>
 *  <b>demo:</b>
 *  <p>
 *
 * <code>
 * <p>
 *  package <strong><span style="color:#E53333;">qrom.component.config;</span></strong><br/>
 *  <br/>
 *  <p>
 *  public class <strong><span style="color:#E53333;">QRomLogConfig</span></strong> extends <em><strong>QRomLogBaseConfig</strong></em> {<br />
 *  <br />
 * <br />
 * &nbsp; &nbsp; <span style="color:#006600;">/**</span><br />
 * <span style="color:#006600;"> &nbsp; &nbsp; &nbsp;* 设置日志打印模式</span><br />
 * <span style="color:#006600;"> &nbsp; &nbsp; &nbsp;* （参见Log输出模式常量）</span><br />
 * <span style="color:#006600;"> &nbsp; &nbsp; &nbsp;* /</span><br />
 *        &nbsp; &nbsp; @Override<br />
 *        &nbsp; &nbsp; public int getLogMode() {<br />
 *        &nbsp; &nbsp; &nbsp; &nbsp; return QRomLogBaseConfig.LOG_BOTH;<br />
 *        &nbsp; &nbsp; }<br />
 * <br />
 *        &nbsp; &nbsp;<span style="color:#006600;">/**</span><br />
 * <span style="color:#006600;">&nbsp; &nbsp; &nbsp;* 设置日志的模块的包名</span><br />
 * <span style="color:#006600;">&nbsp; &nbsp; &nbsp;* （第三方模块请将其配置成自己对应的包名）</span><br />
 * <span style="color:#006600;">&nbsp; &nbsp; &nbsp;* /</span><br />
 *        &nbsp; &nbsp; @Override<br />
 *        &nbsp; &nbsp; public String getPackageName() {<br />
 *        &nbsp; &nbsp; &nbsp; &nbsp; return "com.example.AndroidTest"; &nbsp;&nbsp;&nbsp;<span style="color:#E56600;">// 使用SDK的app的package name</span><br />
 *        &nbsp; &nbsp; }<br />
 *        }
 * </p>
 * <div>
 * <br />
 * </div>
 * <p>
 * <br />
 * </p>
 * </code>
 */

public abstract class QRomLogBaseConfig {

	/**
	 * Log输出模式常量：不打印任何日志
	 */
    public static final int LOG_NONE = 0;
    /**
     * Log输出模式常量：将日志打印到控制台
     */
    public static final int LOG_CONSOLE = 1;
    /**
     * Log输出模式常量：将日志打印到文件
     */
    public static final int LOG_FILE = 2;
    /**
     * Log输出模式常量：将日志同时打印到控制台和文件
     */
    public static final int LOG_BOTH = 3;


    protected QRomLogBaseConfig() {
    }

    /**
     * 设置日志打印模式
     * 注：行为等同Logcat，在AndroidManifest.xml文件中Debugable为false时不输入任何日志
     * （参见Log输出模式常量）
     */
    public abstract int getLogMode();

    /**
     * 设置日志的模块的包名
     * （第三方模块请将其配置成自己对应的包名）
     */
    public abstract String getPackageName();





}
