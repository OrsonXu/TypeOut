package com.aware.smartphoneuse;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = Constants.TAG;

    private String last_package_name = "blank";
    private ComponentName last_component_name =  new ComponentName("blank","blank");

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16)
            //Just in case this helps
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );

                Plugin.currentComponentName = componentName;
                ActivityInfo activityInfo = tryGetActivity(componentName);
                Log.i(TAG,"~~~CurrentComponentName "+componentName.flattenToShortString());
                boolean isActivity = activityInfo != null;
                if (isActivity) {
//                    Log.i(TAG,"~~~CurrentActivity "+componentName.flattenToShortString());
//                    Log.i(TAG, "~~~onAccessibilityEvent: event.PackageName() = "+event.getPackageName()+" last_package_name="+last_package_name+" last_component_name="+last_component_name.flattenToShortString());
//                    Log.i(TAG, "~~~onAccessibilityEvent: "+event.getPackageName().equals(Constants.WeChatPackageName)+" "+event.getPackageName().equals(last_package_name)+" "+componentName.flattenToShortString().equals(last_component_name.flattenToShortString()));
                    if(event.getPackageName().equals(Constants.WeChatPackageName) && (event.getPackageName().equals(last_package_name))
                            && (!componentName.flattenToShortString().equals(last_component_name.flattenToShortString()))
                            && (!componentName.flattenToShortString().equals("com.tencent.mm/.plugin.sns.ui.SnsBrowseUI"))) {
                        last_package_name = event.getPackageName().toString();
                        last_component_name = componentName;
                        boolean is_bad_activity = false;
                        String[] WeChatActivities;
                        if(Constants.WithPyq[0] && Constants.WithMini[0]){
                            WeChatActivities = Constants.WeChatActivities_with_both;
                        }else if(Constants.WithPyq[0]){
                            WeChatActivities = Constants.WeChatActivities_with_pyq;
                        }else if(Constants.WithMini[0]){
                            WeChatActivities = Constants.WeChatActivities_with_mini;
                        }else {
                            WeChatActivities = Constants.WeChatActivities_without_both;
                        }
                        for(String bad_activity:WeChatActivities) {
                            if(bad_activity.equals(componentName.flattenToShortString())) {
                                is_bad_activity = true;
                                break;
                            }
                        }
//                        Log.d(TAG, "~~~onAccessibilityEvent: is_bad_activity = "+is_bad_activity);
                        if(is_bad_activity) {
                            Plugin.Plugin_instance.pseudo_onForeground(componentName.flattenToShortString(),System.currentTimeMillis());
                            Log.d(TAG, "~~~onAccessibilityEvent: call psuedo_onForeGround()");
                        }
                    }
                    else if(event.getPackageName().equals(Constants.WeChatPackageName)) {
                        last_package_name = event.getPackageName().toString();
                        last_component_name = componentName;
                    }

                }
            }

        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
    @Override
    public void onInterrupt() {  }
}
