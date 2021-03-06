package android.core;


//import android.app.ActivityManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Time;

class Start {
	public static void main(String[] args) {
		Thread mainThread = new Thread(){
			@Override
			public void run(){
                /**
                 * @Mayloon update These 2 classes cannot be loaded successfully
                 *          So load them here
                 */
                try {
                    Class.forName("android.test.suitebuilder.annotation.HasClassAnnotation");
                } catch (ClassNotFoundException e) {
                }
                try {
                    Class.forName("android.test.suitebuilder.annotation.HasMethodAnnotation");
                } catch (ClassNotFoundException e) {
                }

                Log.w("System", "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
                Log.w("System", "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
                Log.w("System", "System starts loading at: " + Time.getCurrentTime());
				long start = System.currentTimeMillis();
				// start PackageManager, install launcher package
				PackageManager pm = (PackageManager) Context.getSystemContext()
						.getSystemService(Context.PACKAGE_SERVICE);
				// start ActivityManager
                Log.w("System", "before ActivityManager.main: "
						+ Time.getCurrentTime());
				ActivityManager am = (ActivityManager) Context.getSystemContext()
						.getSystemService(Context.ACTIVITY_SERVICE);
				am.main();
				long end = System.currentTimeMillis();
				long duration = end -start;
				Log.w("System","Mayloon launch time:" + duration + "ms");
                Log.w("System", "after ActivityManager.main: "
						+ Time.getCurrentTime());
                Log.w("System", "System finishes loading at: "
								+ Time.getCurrentTime());
				
			}
		};
		mainThread.start();
	
	}
}