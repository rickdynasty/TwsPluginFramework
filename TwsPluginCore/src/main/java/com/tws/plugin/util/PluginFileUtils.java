package com.tws.plugin.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import qrom.component.log.QRomLog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import com.tws.plugin.core.PluginLoader;

public class PluginFileUtils {

    private static final boolean DEBUG = false;
    private static final String TAG = "rick_Print:" + PluginFileUtils.class.getSimpleName();

    public static final String ASSETS_PLUGINS = "assets/plugins/";
    public static final String ICON_FOLDER = "plugins";
    public static final String LIB_FOLDER = "lib";
    public static final String DALVIK_CACHE_FOLDER = "dalvik-cache";
    private static final String PREFIX_ICON_PATH = "assets" + File.separator + ICON_FOLDER + File.separator;
    private static final String PREFIX_LIB_PATH = LIB_FOLDER + File.separator;
    public static final String FIX_ICON_NAME = ".png"; // 注意这里是小写的
    public static final String FIX_LIB_NAME = ".so";
    public static final String XHDPI = "drawable-xhdpi";
    public static final String XXHDPI = "drawable-xxhdpi";
    public static final String XXXHDPI = "drawable-xxxhdpi";
    private static float fdensity = 0.0f;// 1080p 是3.0f
    public static final float XXHDPI_DENSITY = 3.0f;

    public static boolean copyFile(String source, String dest) {
        try {
            return copyFile(new FileInputStream(new File(source)), dest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean copyFile(final InputStream inputStream, String dest) {
        QRomLog.i(TAG, "copyFile to " + dest);

        if (Build.VERSION.SDK_INT >= 23) {// Build.VERSION_CODES.M)
            if (dest.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                int permissionState = PluginLoader.getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionState != PackageManager.PERMISSION_GRANTED) {
                    //6.0的系统即使申请了读写sdcard的权限,仍然可以在设置中关闭, 则需要requestPermissons
                    QRomLog.e(TAG, "6.0以上的系统, targetSDK>=23时, sdcard读写默认为未授权,需requestPermissons或者在设置中开启:" + dest);
                    return false;
                }
            }
        }
        FileOutputStream oputStream = null;
        try {
            File destFile = new File(dest);
            File parentDir = destFile.getParentFile();
            if (!parentDir.isDirectory() || !parentDir.exists()) {
                destFile.getParentFile().mkdirs();
            }
            oputStream = new FileOutputStream(destFile);
            byte[] bb = new byte[48 * 1024];
            int len = 0;
            while ((len = inputStream.read(bb)) != -1) {
                oputStream.write(bb, 0, len);
            }
            oputStream.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oputStream != null) {
                try {
                    oputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean copySo(File sourceDir, String so, String dest) {
        QRomLog.i(TAG, "copySo - sourceDir=" + sourceDir + " so is " + so + " dest=" + dest);
        try {
            boolean isSuccess = false;
            if (Build.VERSION.SDK_INT >= 21) {
                String[] abis = Build.SUPPORTED_ABIS;
                if (abis != null) {
                    for (String abi : abis) {
                        QRomLog.i(TAG, "try supported abi:" + abi);
                        String name = LIB_FOLDER + File.separator + abi + File.separator + so;
                        File sourceFile = new File(sourceDir, name);
                        if (sourceFile.exists()) {
                            isSuccess = copyFile(sourceFile.getAbsolutePath(), dest + File.separator + LIB_FOLDER + File.separator + so);
                            // api21 64位系统的目录可能有些不同
                            // copyFile(sourceFile.getAbsolutePath(), dest +
                            // File.separator + name);
                            break;
                        }
                    }
                }
            } else {
                QRomLog.i(TAG, "supported api:" + Build.CPU_ABI + " " + Build.CPU_ABI2);

                String name = LIB_FOLDER + File.separator + Build.CPU_ABI + File.separator + so;
                File sourceFile = new File(sourceDir, name);

                if (!sourceFile.exists() && Build.CPU_ABI2 != null) {
                    name = LIB_FOLDER + File.separator + Build.CPU_ABI2 + File.separator + so;
                    sourceFile = new File(sourceDir, name);

                    if (!sourceFile.exists()) {
                        name = LIB_FOLDER + File.separator + "armeabi" + File.separator + so;
                        sourceFile = new File(sourceDir, name);
                    }
                }
                if (sourceFile.exists()) {
                    isSuccess = copyFile(sourceFile.getAbsolutePath(), dest + File.separator + LIB_FOLDER
                            + File.separator + so);
                }
            }

            if (!isSuccess) {
                QRomLog.e(TAG, "安装 :" + so + " 失败: NO_MATCHING_ABIS");
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    Toast.makeText(PluginLoader.getApplication(), "安装 " + so + " 失败: NO_MATCHING_ABIS", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public static float getDensity() {
        if (0.0f == fdensity) {
            WindowManager wm = (WindowManager) PluginLoader.getApplication().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            fdensity = dm.density;
        }

        return fdensity;
    }

    public static boolean copyIcon(File sourceDir, String icon, String dest) {
        getDensity();

        String iconFolder = XXHDPI;
        if (XXHDPI_DENSITY < fdensity) {
            iconFolder = XXXHDPI;
        } else if (fdensity < XXHDPI_DENSITY && 0.0f != fdensity) {
            iconFolder = XHDPI;
        }

        try {
            boolean isSuccess = false;

            String name = PREFIX_ICON_PATH + File.separator + iconFolder + File.separator + icon;
            File sourceFile = new File(sourceDir, name);

            if (!sourceFile.exists()) {
                if (XXHDPI_DENSITY < fdensity) {
                    name = PREFIX_ICON_PATH + File.separator + XXHDPI + File.separator + icon;
                    sourceFile = new File(sourceDir, name);
                    if (!sourceFile.exists()) {
                        name = PREFIX_ICON_PATH + File.separator + XHDPI + File.separator + icon;
                        sourceFile = new File(sourceDir, name);
                    }
                } else if (XXHDPI_DENSITY == fdensity) {
                    name = PREFIX_ICON_PATH + File.separator + XXXHDPI + File.separator + icon;
                    sourceFile = new File(sourceDir, name);
                    if (!sourceFile.exists()) {
                        name = PREFIX_ICON_PATH + File.separator + XHDPI + File.separator + icon;
                        sourceFile = new File(sourceDir, name);
                    }
                } else {
                    name = PREFIX_ICON_PATH + File.separator + XXHDPI + File.separator + icon;
                    sourceFile = new File(sourceDir, name);
                    if (!sourceFile.exists()) {
                        name = PREFIX_ICON_PATH + File.separator + XXXHDPI + File.separator + icon;
                        sourceFile = new File(sourceDir, name);
                    }
                }

                if (!sourceFile.exists()) {
                    name = PREFIX_ICON_PATH + File.separator + icon;
                    sourceFile = new File(sourceDir, name);
                }
            }

            if (sourceFile.exists()) {
                isSuccess = copyFile(sourceFile.getAbsolutePath(), dest + File.separator + ICON_FOLDER + File.separator + icon);
            }

            if (!isSuccess) {
                QRomLog.e(TAG, "获取 :" + icon + " 失败: NO_MATCHING");
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    Toast.makeText(PluginLoader.getApplication(), "获取 " + icon + " 失败: NO_MATCHING", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 解析必要的资源：so、图标等
     *
     * @return Set
     */
    public static Set<String> unZipNecessaryRes(String apkFile, File tempDir) {

        HashSet<String> result = null;

        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        QRomLog.i(TAG, "开始解析获取必要的资源：" + tempDir.getAbsolutePath());

        ZipFile zfile = null;
        boolean isSuccess = false;
        BufferedOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
            zfile = new ZipFile(apkFile);
            ZipEntry ze = null;
            Enumeration zList = zfile.entries();
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                String relativePath = ze.getName();

                if (!relativePath.startsWith(PREFIX_LIB_PATH) && !relativePath.startsWith(PREFIX_ICON_PATH)) {
                    if (DEBUG) {
                        QRomLog.i(TAG, "不是lib也不是插件资源目录，跳过:" + relativePath);
                    }
                    continue;
                }

                if (ze.isDirectory()) {
                    File folder = new File(tempDir, relativePath);
                    if (DEBUG) {
                        QRomLog.i(TAG, "正在创建目录:" + folder.getAbsolutePath());
                    }
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }

                } else {

                    if (result == null) {
                        result = new HashSet<String>(4);
                    }

                    File targetFile = new File(tempDir, relativePath);
                    QRomLog.i(TAG, "正在解压必要的资源文件:" + targetFile.getAbsolutePath());
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    targetFile.createNewFile();

                    fos = new BufferedOutputStream(new FileOutputStream(targetFile));
                    bis = new BufferedInputStream(zfile.getInputStream(ze));
                    byte[] buffer = new byte[2048];
                    int count = -1;
                    while ((count = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                        fos.flush();
                    }
                    fos.close();
                    fos = null;
                    bis.close();
                    bis = null;

                    result.add(relativePath.substring(relativePath.lastIndexOf(File.separator) + 1));
                }
            }
            isSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zfile != null) {
                try {
                    zfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        QRomLog.i(TAG, "解压必要的资源文件结束 " + isSuccess);
        return result;
    }

    public static void readFileFromJar(String jarFilePath, String metaInfo) {
        QRomLog.i(TAG, "call readFileFromJar(" + jarFilePath + ", " + metaInfo + ")");
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarFilePath);
            JarEntry entry = jarFile.getJarEntry(metaInfo);
            if (entry != null) {
                InputStream input = jarFile.getInputStream(entry);

                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return;

    }

    /**
     * 递归删除文件及文件夹
     *
     * @param file
     */
    public static boolean deleteAll(File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles != null && 0 < childFiles.length) {
                for (int i = 0; i < childFiles.length; i++) {
                    deleteAll(childFiles[i]);
                }
            }
        }
        QRomLog.i(TAG, "delete:" + file.getAbsolutePath());
        return file.delete();
    }

    public static void printAll(File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles != null && 0 < childFiles.length) {
                for (int i = 0; i < childFiles.length; i++) {
                    printAll(childFiles[i]);
                }
            }
        }
    }

    public static String streamToString(InputStream input) throws IOException {

        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(isr);

        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        isr.close();
        return sb.toString();
    }

    /**
     * 从assets目录下拷贝整个文件夹，不管是文件夹还是文件都能拷贝
     *
     * @param context           上下文
     * @param rootDirFullPath   文件目录，要拷贝的目录如assets目录下有一个SBClock文件夹：SBClock
     * @param targetDirFullPath 目标文件夹位置如：/sdcrad/SBClock
     */
    public static void copyFolderFromAssets(Context context, String rootDirFullPath, String targetDirFullPath) {
        QRomLog.i(TAG, "copyFolderFromAssets " + "rootDirFullPath-" + rootDirFullPath + " targetDirFullPath-" + targetDirFullPath);
        try {
            String[] listFiles = context.getAssets().list(rootDirFullPath);// 遍历该目录下的文件和文件夹  
            for (String string : listFiles) {// 看起子目录是文件还是文件夹，这里只好用.做区分了  
                QRomLog.i(TAG, "name-" + rootDirFullPath + "/" + string);
                if (isFileByName(string)) {// 文件  
                    copyFileFromAssets(context, rootDirFullPath + "/" + string, targetDirFullPath + "/" + string);
                } else {// 文件夹  
                    String childRootDirFullPath = rootDirFullPath + "/" + string;
                    String childTargetDirFullPath = targetDirFullPath + "/" + string;
                    new File(childTargetDirFullPath).mkdirs();
                    copyFolderFromAssets(context, childRootDirFullPath, childTargetDirFullPath);
                }
            }
        } catch (IOException e) {
            QRomLog.i(TAG, "copyFolderFromAssets " + "IOException-" + e.getMessage());
            QRomLog.i(TAG, "copyFolderFromAssets " + "IOException-" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private static boolean isFileByName(String string) {
        if (string.contains(".")) {
            return true;
        }
        return false;
    }

    /**
     * 从assets目录下拷贝文件
     *
     * @param context            上下文
     * @param assetsFilePath     文件的路径名如：SBClock/0001cuteowl/cuteowl_dot.png
     * @param targetFileFullPath 目标文件路径如：/sdcard/SBClock/0001cuteowl/cuteowl_dot.png
     */
    public static void copyFileFromAssets(Context context, String assetsFilePath, String targetFileFullPath) {
        QRomLog.i(TAG, "copyFileFromAssets assetsFilePath = " + assetsFilePath + " , targetFileFullPath = " + targetFileFullPath);
        InputStream assestsFileImputStream;
        try {
            assestsFileImputStream = context.getAssets().open(assetsFilePath);
            copyFile(assestsFileImputStream, targetFileFullPath);
        } catch (IOException e) {
            QRomLog.i(TAG, "copyFileFromAssets " + "IOException-" + e.getMessage());
            e.printStackTrace();
        }
    }
}
