### Structure ###

Files: (./readme_src/file_tree.PNG)



- `MainActivity`
  - The start page, open survices provided by `Plugin` 
- `Plugin`(Service)
  - Manage the function provided by the plugin of AWARE Framework
  - mainly used for monitoring user behavior of smartphone using and
  - connect with `Timeout` for intervention
- `Timeout`(Service)
  - Only be called by `Plugin` for providing intervention.
  - Every function with Overlay in name are related with the logic of triggering intervention.
  - sendInterventionFailurData -- sendBroadcast, When a broadcast is sent, the system automatically routes broadcasts to apps that have subscribed to receive that particular type of broadcast.
  - sendInterventionData -- Similar with sendInterventionFailurData
- `WhitelistActivity`
  - Define the logic for whitelist, which will control which APPs will trigger intervention
  - Have a class named `AppInfo` , which have information about APPs existed in the smartphone.
- `WhitelistGridAdapater`
  - Can be ignored, a class which required by `RecyclerView`.
- `OverlayActivity`
  - Interact with users with intervention.
  

https://developer.android.com/guide/

### Connect to Your Database ###

In MainActivity.java:
1. Change comment
// join.setVisibility(View.VISIBLE);
// sync.setVisibility(View.VISIBLE);

into not comment and delete the nearby 

join.setVisibility(View.INVISIBLE);
sync.setVisibility(View.INVISIBLE);

2. Change the second parameter of 

Aware.joinStudy(getApplicationContext(), ...)

into link to your AWARE database

### Signature ###

SmartphoneAddiction.jks / alias: SmartphoneAddictionKey

Password: SmartphoneAddiction