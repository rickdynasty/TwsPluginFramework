package com.tws.plugin.core.multidex;

import java.util.List;

import android.app.PackageInstallObserver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.EphemeralApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstrumentationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.KeySet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;

import com.tws.plugin.core.PluginLoader;

public class PluginMultiDexHelper {

	public static PackageManager fixPackageManagerForMultDexInstaller(final String pluginPackageName,
			final PackageManager packageManager) {

		return new PackageManager() {

			@Override
			public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public String[] currentToCanonicalPackageNames(String[] names) {
				return new String[0];
			}

			@Override
			public String[] canonicalToCurrentPackageNames(String[] names) {
				return new String[0];
			}

			@Override
			public Intent getLaunchIntentForPackage(String packageName) {
				return null;
			}

			@Override
			public Intent getLeanbackLaunchIntentForPackage(String packageName) {
				return null;
			}

			@Override
			public int[] getPackageGids(String packageName) throws NameNotFoundException {
				return new int[0];
			}

			// android-N
			public int[] getPackageGids(String s, int i) throws NameNotFoundException {
				return new int[0];
			}

			// android-N
			public int getPackageUid(String s, int i) throws NameNotFoundException {
				return 0;
			}

			@Override
			public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
				return null;
			}

			@Override
			public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
				if (packageName.equals(PluginLoader.getApplication().getPackageName())) {
					packageName = pluginPackageName;
				}
				return packageManager.getApplicationInfo(packageName, flags);
			}

			@Override
			public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
				return null;
			}

			@Override
			public List<PackageInfo> getInstalledPackages(int flags) {
				return null;
			}

			@Override
			public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
				return null;
			}

			@Override
			public int checkPermission(String permName, String pkgName) {
				return 0;
			}

			public boolean isPermissionRevokedByPolicy(String permName, String pkgName) {
				return false;
			}

			@Override
			public boolean addPermission(PermissionInfo info) {
				return false;
			}

			@Override
			public boolean addPermissionAsync(PermissionInfo info) {
				return false;
			}

			@Override
			public void removePermission(String name) {

			}

			@Override
			public int checkSignatures(String pkg1, String pkg2) {
				return 0;
			}

			@Override
			public int checkSignatures(int uid1, int uid2) {
				return 0;
			}

			@Override
			public String[] getPackagesForUid(int uid) {
				return new String[0];
			}

			@Override
			public String getNameForUid(int uid) {
				return null;
			}

			@Override
			public List<ApplicationInfo> getInstalledApplications(int flags) {
				return null;
			}

			@Override
			public String[] getSystemSharedLibraryNames() {
				return new String[0];
			}

			@Override
			public FeatureInfo[] getSystemAvailableFeatures() {
				return new FeatureInfo[0];
			}

			@Override
			public boolean hasSystemFeature(String name) {
				return false;
			}

			@Override
			public ResolveInfo resolveActivity(Intent intent, int flags) {
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics,
					Intent intent, int flags) {
				return null;
			}

			@Override
			public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
				return null;
			}

			@Override
			public ResolveInfo resolveService(Intent intent, int flags) {
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
				return null;
			}

			@Override
			public ProviderInfo resolveContentProvider(String name, int flags) {
				return null;
			}

			@Override
			public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
				return null;
			}

			@Override
			public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags)
					throws NameNotFoundException {
				return null;
			}

			@Override
			public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
				return null;
			}

			@Override
			public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
				return null;
			}

			@Override
			public Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getDefaultActivityIcon() {
				return null;
			}

			@Override
			public Drawable getApplicationIcon(ApplicationInfo info) {
				return null;
			}

			@Override
			public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getApplicationBanner(ApplicationInfo info) {
				return null;
			}

			@Override
			public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getApplicationLogo(ApplicationInfo info) {
				return null;
			}

			@Override
			public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
				return null;
			}

			@Override
			public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation,
					int badgeDensity) {
				return null;
			}

			@Override
			public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
				return null;
			}

			@Override
			public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
				return null;
			}

			@Override
			public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
				return null;
			}

			@Override
			public CharSequence getApplicationLabel(ApplicationInfo info) {
				return null;
			}

			@Override
			public Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException {
				return null;
			}

			@Override
			public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
				return null;
			}

			@Override
			public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
				return null;
			}

			@Override
			public void verifyPendingInstall(int id, int verificationCode) {

			}

			@Override
			public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {

			}

			@Override
			public void setInstallerPackageName(String targetPackage, String installerPackageName) {

			}

			@Override
			public String getInstallerPackageName(String packageName) {
				return null;
			}

			@Override
			public void addPackageToPreferred(String packageName) {

			}

			@Override
			public void removePackageFromPreferred(String packageName) {

			}

			@Override
			public List<PackageInfo> getPreferredPackages(int flags) {
				return null;
			}

			@Override
			public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {

			}

			@Override
			public void clearPackagePreferredActivities(String packageName) {

			}

			@Override
			public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities,
					String packageName) {
				return 0;
			}

			@Override
			public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {

			}

			@Override
			public int getComponentEnabledSetting(ComponentName componentName) {
				return 0;
			}

			@Override
			public void setApplicationEnabledSetting(String packageName, int newState, int flags) {

			}

			@Override
			public int getApplicationEnabledSetting(String packageName) {
				return 0;
			}

			@Override
			public boolean isSafeMode() {
				return false;
			}

			@Override
			public PackageInstaller getPackageInstaller() {
				return null;
			}

			// android-N
			public boolean hasSystemFeature(String arg1, int agr2) {
				return false;
			}

			@Override
			public void addCrossProfileIntentFilter(IntentFilter arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void addOnPermissionsChangeListener(OnPermissionsChangedListener arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void clearApplicationUserData(String arg0, IPackageDataObserver arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void clearCrossProfileIntentFilters(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void deleteApplicationCacheFiles(String arg0, IPackageDataObserver arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void deleteApplicationCacheFilesAsUser(String arg0, int arg1, IPackageDataObserver arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void deletePackage(String arg0, IPackageDeleteObserver arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void deletePackageAsUser(String arg0, IPackageDeleteObserver arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void flushPackageRestrictionsAsUser(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void freeStorage(String arg0, long arg1, IntentSender arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void freeStorageAndNotify(String arg0, long arg1, IPackageDataObserver arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public List<IntentFilter> getAllIntentFilters(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean getApplicationHiddenSettingAsUser(String arg0, UserHandle arg1) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public ApplicationInfo getApplicationInfoAsUser(String arg0, int arg1, int arg2)
					throws NameNotFoundException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getDefaultBrowserPackageNameAsUser(int arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Drawable getEphemeralApplicationIcon(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<EphemeralApplicationInfo> getEphemeralApplications() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] getEphemeralCookie() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getEphemeralCookieMaxSizeBytes() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public ComponentName getHomeActivities(List<ResolveInfo> arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<PackageInfo> getInstalledPackagesAsUser(int arg0, int arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<IntentFilterVerificationInfo> getIntentFilterVerifications(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getIntentVerificationStatusAsUser(String arg0, int arg1) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public KeySet getKeySetByAlias(String arg0, String arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Drawable getManagedUserBadgedDrawable(Drawable arg0, Rect arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getMoveStatus(int arg0) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public List<VolumeInfo> getPackageCandidateVolumes(ApplicationInfo arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VolumeInfo getPackageCurrentVolume(ApplicationInfo arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public PackageInfo getPackageInfoAsUser(String arg0, int arg1, int arg2) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void getPackageSizeInfoAsUser(String arg0, int arg1, IPackageStatsObserver arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public int getPackageUidAsUser(String arg0, int arg1) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getPackageUidAsUser(String arg0, int arg1, int arg2) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getPermissionControllerPackageName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getPermissionFlags(String arg0, String arg1, UserHandle arg2) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public List<VolumeInfo> getPrimaryStorageCandidateVolumes() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VolumeInfo getPrimaryStorageCurrentVolume() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Resources getResourcesForApplicationAsUser(String arg0, int arg1) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getServicesSystemSharedLibraryPackageName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getSharedSystemSharedLibraryPackageName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public KeySet getSigningKeySet(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getUidForSharedUser(String arg0) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public Drawable getUserBadgeForDensity(UserHandle arg0, int arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Drawable getUserBadgeForDensityNoBackground(UserHandle arg0, int arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VerifierDeviceIdentity getVerifierDeviceIdentity() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void grantRuntimePermission(String arg0, String arg1, UserHandle arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public int installExistingPackage(String arg0) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int installExistingPackageAsUser(String arg0, int arg1) throws NameNotFoundException {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			@Deprecated
			public void installPackage(Uri arg0, IPackageInstallObserver arg1, int arg2, String arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			@Deprecated
			public void installPackage(Uri arg0, PackageInstallObserver arg1, int arg2, String arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean isEphemeralApplication() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isPackageAvailable(String arg0) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isPackageSuspendedForUser(String arg0, int arg1) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isSignedBy(String arg0, KeySet arg1) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isSignedByExactly(String arg0, KeySet arg1) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isUpgrade() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Drawable loadItemIcon(PackageItemInfo arg0, ApplicationInfo arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Drawable loadUnbadgedItemIcon(PackageItemInfo arg0, ApplicationInfo arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int movePackage(String arg0, VolumeInfo arg1) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int movePrimaryStorage(VolumeInfo arg0) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public List<ResolveInfo> queryBroadcastReceiversAsUser(Intent arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentActivitiesAsUser(Intent arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentContentProvidersAsUser(Intent arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<ResolveInfo> queryIntentServicesAsUser(Intent arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void registerMoveCallback(MoveCallback arg0, Handler arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removeOnPermissionsChangeListener(OnPermissionsChangedListener arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			@Deprecated
			public void replacePreferredActivity(IntentFilter arg0, int arg1, ComponentName[] arg2, ComponentName arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public ResolveInfo resolveActivityAsUser(Intent arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ProviderInfo resolveContentProviderAsUser(String arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void revokeRuntimePermission(String arg0, String arg1, UserHandle arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean setApplicationHiddenSettingAsUser(String arg0, boolean arg1, UserHandle arg2) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean setDefaultBrowserPackageNameAsUser(String arg0, int arg1) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean setEphemeralCookie(byte[] arg0) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public String[] setPackagesSuspendedAsUser(String[] arg0, boolean arg1, int arg2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean shouldShowRequestPermissionRationale(String arg0) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void unregisterMoveCallback(MoveCallback arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean updateIntentVerificationStatusAsUser(String arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void updatePermissionFlags(String arg0, String arg1, int arg2, int arg3, UserHandle arg4) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void verifyIntentFilter(int arg0, int arg1, List<String> arg2) {
				// TODO Auto-generated method stub
				
			}
		};
	}
}
