/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app;

import java.util.ArrayList;

import android.app.ActivityStack.ActivityState;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;

/**
 * An entry in the history stack, representing an activity.
 */
class ActivityRecord extends Binder/** extends IApplicationToken.Stub /**/
{
	final ActivityManager service; // owner
	final ActivityStack stack; // owner
	final ActivityInfo info; // all about me
	final int launchedFromUid; // always the uid who started the activity.
	final Intent intent; // the original intent that generated us
	final ComponentName realActivity; // the intent component, or target of an alias.
	final String shortComponentName; // the short component name of the intent
	final String resolvedType; // as per original caller;
	final String packageName; // the package implementing intent's component
	final String processName; // process where this component wants to run
	final String taskAffinity; // as per ActivityInfo.taskAffinity
	final boolean stateNotNeeded; // As per ActivityInfo.flags
	final boolean fullscreen; // covers the full screen?
	final boolean componentSpecified; // did caller specifiy an explicit component?
	final boolean isHomeActivity; // do we consider this to be a home activity?
	final String baseDir; // where activity source (resources etc) located
	final String resDir; // where public activity source (public resources etc) located
	final String dataDir; // where activity data should go
	CharSequence nonLocalizedLabel; // the label information from the package mgr.
	int labelRes; // the label information from the package mgr.
	int icon; // resource identifier of activity's icon.
	int theme; // resource identifier of activity's theme.
	TaskRecord task; // the task this is in.
	long launchTime; // when we starting launching this activity
	long startTime; // last time this activity was started
	long cpuTimeAtResume; // the cpu time of host process at the time of resuming activity
	Configuration configuration; // configuration activity was last running in
	ActivityRecord resultTo; // who started this entry, so will get our reply
	final String resultWho; // additional identifier for use by resultTo.
	final int requestCode; // code given by requester (resultTo)
	@SuppressWarnings("rawtypes")
	ArrayList results; // pending ActivityResult objs we have received
	@SuppressWarnings("rawtypes")
	ArrayList newIntents; // any pending new intents for single-top mode
	ProcessRecord app; // if non-null, hosting application
	CharSequence description; // textual description of paused screen
	int state; // current state we are in
	Bundle icicle; // last saved activity state
	boolean frontOfTask; // is this the root activity of its task?
	boolean launchFailed; // set if a launched failed, to abort on 2nd try
	boolean haveState; // have we gotten the last activity state?
	boolean stopped; // is activity pause finished?
	boolean delayedResume; // not yet resumed because of stopped app switches?
	boolean finishing; // activity in pending finish list?
	boolean configDestroy; // need to destroy due to config change?
	int configChangeFlags; // which config values have changed
	boolean keysPaused; // has key dispatching been paused for it?
	boolean inHistory; // are we in the history stack?
	int launchMode; // the launch mode activity attribute.
	boolean visible; // does this activity's window need to be shown?
	boolean waitingVisible; // true if waiting for a new act to become vis
	boolean nowVisible; // is this activity's window visible?
	boolean thumbnailNeeded;// has someone requested a thumbnail?
	boolean idle; // has the activity gone idle?
	boolean hasBeenLaunched;// has this activity ever been launched?
	boolean frozenBeforeDestroy;// has been frozen but not yet destroyed.

	String stringName; // for caching of toString().

	ActivityRecord(ActivityManager _service, ActivityStack _stack,
			ProcessRecord _caller, int _launchedFromUid, Intent _intent,
			String _resolvedType, ActivityInfo aInfo,
			Configuration _configuration, ActivityRecord _resultTo,
			String _resultWho, int _reqCode, boolean _componentSpecified) {
		service = _service;
		stack = _stack;
		info = aInfo;
		launchedFromUid = _launchedFromUid;
		intent = _intent;
		shortComponentName = _intent.getComponent().flattenToShortString();
		resolvedType = _resolvedType;
		componentSpecified = _componentSpecified;
		configuration = _configuration;
		resultTo = _resultTo;
		resultWho = _resultWho;
		requestCode = _reqCode;
		state = ActivityState.INITIALIZING;
		frontOfTask = false;
		launchFailed = false;
		haveState = false;
		stopped = false;
		delayedResume = false;
		finishing = false;
		configDestroy = false;
		keysPaused = false;
		inHistory = false;
		visible = true;
		waitingVisible = false;
		nowVisible = false;
		thumbnailNeeded = false;
		idle = false;
		hasBeenLaunched = false;

		if (aInfo != null) {
			if (aInfo.targetActivity == null
					|| aInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE
					|| aInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
				realActivity = _intent.getComponent();
			} else {
				realActivity = new ComponentName(aInfo.packageName,
						aInfo.targetActivity);
			}
			taskAffinity = aInfo.taskAffinity;
			stateNotNeeded = (aInfo.flags & ActivityInfo.FLAG_STATE_NOT_NEEDED) != 0;
			baseDir = aInfo.applicationInfo.sourceDir;
			resDir = aInfo.applicationInfo.publicSourceDir;
			dataDir = aInfo.applicationInfo.dataDir;
			nonLocalizedLabel = aInfo.nonLocalizedLabel;
			labelRes = aInfo.labelRes;
			if (nonLocalizedLabel == null && labelRes == 0) {
				ApplicationInfo app = aInfo.applicationInfo;
				nonLocalizedLabel = app.nonLocalizedLabel;
				labelRes = app.labelRes;
			}
			icon = aInfo.getIconResource();
			theme = aInfo.getThemeResource();
			//			if ((aInfo.flags & ActivityInfo.FLAG_MULTIPROCESS) != 0
			//					&& _caller != null
			//					&& (aInfo.applicationInfo.uid == Process.SYSTEM_UID || aInfo.applicationInfo.uid == _caller.info.uid)) {
			if ((aInfo.flags & ActivityInfo.FLAG_MULTIPROCESS) != 0
					&& _caller != null
					&& aInfo.applicationInfo.uid == _caller.info.uid) {
				processName = _caller.processName;
			} else {
				if (aInfo.processName == null)
					processName = aInfo.processName = aInfo.packageName;
				else
					processName = aInfo.processName;
			}

			if (intent != null
					&& (aInfo.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) != 0) {
				intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			}

			packageName = aInfo.applicationInfo.packageName;
			launchMode = aInfo.launchMode;

			if (aInfo.theme == com.android.internal.R.style.Theme_Dialog) {
			    fullscreen = false;
			} else {
			    fullscreen = true;
			}
			if (!_componentSpecified || _launchedFromUid == 0) {
				//				 If we know the system has determined the component, then
				//				 we can consider this to be a home activity...
				if (Intent.ACTION_MAIN.equals(_intent.getAction())
						&& _intent.hasCategory(Intent.CATEGORY_HOME)
						&& _intent.getCategories().size() == 1
						&& _intent.getData() == null
						&& _intent.getType() == null
						&& (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0
						&& !"android".equals(realActivity.getClassName())) {
					// This sure looks like a home activity!
					// Note the last check is so we don't count the resolver
					// activity as being home...  really, we don't care about
					// doing anything special with something that comes from
					// the core framework package.
					isHomeActivity = true;
				} else {
					isHomeActivity = false;
				}
			} else {
				isHomeActivity = false;
			}
		} else {
			realActivity = null;
			taskAffinity = null;
			stateNotNeeded = false;
			baseDir = null;
			resDir = null;
			dataDir = null;
			processName = null;
			packageName = null;
			fullscreen = true;
			isHomeActivity = false;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void addResultLocked(ActivityRecord from, String resultWho,
			int requestCode, int resultCode, Intent resultData) {
		ActivityResult r = new ActivityResult(from, resultWho, requestCode,
				resultCode, resultData);
		if (results == null) {
			results = new ArrayList();
		}
		results.add(r);
	}

	void removeResultsLocked(ActivityRecord from, String resultWho,
			int requestCode) {
		if (results != null) {
			for (int i = results.size() - 1; i >= 0; i--) {
				ActivityResult r = (ActivityResult) results.get(i);
				if (r.mFrom != from)
					continue;
				if (r.mResultWho == null) {
					if (resultWho != null)
						continue;
				} else {
					if (!r.mResultWho.equals(resultWho))
						continue;
				}
				if (r.mRequestCode != requestCode)
					continue;

				results.remove(i);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void addNewIntentLocked(Intent intent) {
		if (newIntents == null) {
			newIntents = new ArrayList();
		}
		newIntents.add(intent);
	}

	/**
	 * Deliver a new Intent to an existing activity, so that its onNewIntent()
	 * method will be called at the proper time.
	 */
	//	final void deliverNewIntentLocked(int callingUid, Intent intent) {
	//		boolean sent = false;
	//		if (state == ActivityState.RESUMED && app != null && app.thread != null) {
	//			try {
	//				ArrayList<Intent> ar = new ArrayList<Intent>();
	//				intent = new Intent(intent);
	//				ar.add(intent);
	//				//				service.grantUriPermissionFromIntentLocked(callingUid,
	//				//						packageName, intent, getUriPermissionsLocked());
	//				app.thread.scheduleNewIntent(ar, this);
	//				sent = true;
	//			} catch (RemoteException e) {
	//				//				Slog.w(ActivityManager.TAG,
	//				//						"Exception thrown sending new intent to " + this, e);
	//			} catch (NullPointerException e) {
	//				//				Slog.w(ActivityManager.TAG,
	//				//						"Exception thrown sending new intent to " + this, e);
	//			}
	//		}
	//		if (!sent) {
	//			addNewIntentLocked(new Intent(intent));
	//		}
	//	}

	void pauseKeyDispatchingLocked() {
		if (!keysPaused) {
			keysPaused = true;
			//			service.mWindowManager.pauseKeyDispatching(this);
		}
	}

	void resumeKeyDispatchingLocked() {
		if (keysPaused) {
			keysPaused = false;
			//			service.mWindowManager.resumeKeyDispatching(this);
		}
	}

	// IApplicationToken
	public void windowsVisible() {
		synchronized (service) {
			if (launchTime != 0) {
				final long curTime = SystemClock.uptimeMillis();
				final long thisTime = curTime - launchTime;
				final long totalTime = stack.mInitialStartTime != 0 ? (curTime - stack.mInitialStartTime)
						: thisTime;
				//				if (ActivityManager.SHOW_ACTIVITY_START_TIME) {
				//					EventLog.writeEvent(EventLogTags.ACTIVITY_LAUNCH_TIME,
				//							System.identityHashCode(this), shortComponentName,
				//							thisTime, totalTime);
				//					StringBuilder sb = service.mStringBuilder;
				//					sb.setLength(0);
				//					sb.append("Displayed ");
				//					sb.append(shortComponentName);
				//					sb.append(": ");
				//					TimeUtils.formatDuration(thisTime, sb);
				//					if (thisTime != totalTime) {
				//						sb.append(" (total ");
				//						TimeUtils.formatDuration(totalTime, sb);
				//						sb.append(")");
				//					}
				//					Log.i(ActivityManager.TAG, sb.toString());
				//				}
				//				stack.reportActivityLaunchedLocked(false, this, thisTime,
				//						totalTime);
				if (totalTime > 0) {
					//					service.mUsageStatsService.noteLaunchTime(realActivity,
					//							(int) totalTime);
				}
				launchTime = 0;
				stack.mInitialStartTime = 0;
			}
			startTime = 0;
			//			stack.reportActivityVisibleLocked(this);
			//			if (ActivityManager.DEBUG_SWITCH)
			//				Log.v(ActivityManager.TAG, "windowsVisible(): " + this);
			if (!nowVisible) {
				nowVisible = true;
				if (!idle) {
					// Instead of doing the full stop routine here, let's just
					// hide any activities we now can, and let them stop when
					// the normal idle happens.
					//					stack.processStoppingActivitiesLocked(false);
				} else {
					// If this activity was already idle, then we now need to
					// make sure we perform the full stop of any activities
					// that are waiting to do so.  This is because we won't
					// do that while they are still waiting for this one to
					// become visible.
					final int N = stack.mWaitingVisibleActivities.size();
					if (N > 0) {
						for (int i = 0; i < N; i++) {
							ActivityRecord r = (ActivityRecord) stack.mWaitingVisibleActivities
									.get(i);
							r.waitingVisible = false;
							//							if (ActivityManager.DEBUG_SWITCH)
							//								Log.v(ActivityManager.TAG,
							//										"Was waiting for visible: " + r);
						}
						stack.mWaitingVisibleActivities.clear();
						Message msg = Message.obtain();
						msg.what = ActivityStack.IDLE_NOW_MSG;
						stack.mHandler.sendMessage(msg);
					}
				}
				//				service.scheduleAppGcsLocked();
			}
		}
	}

	public void windowsGone() {
		//		if (ActivityManager.DEBUG_SWITCH)
		//			Log.v(ActivityManager.TAG, "windowsGone(): " + this);
		nowVisible = false;
	}

	//	private ActivityRecord getWaitingHistoryRecordLocked() {
	//		// First find the real culprit...  if we are waiting
	//		// for another app to start, then we have paused dispatching
	//		// for this activity.
	//		ActivityRecord r = this;
	//		if (r.waitingVisible) {
	//			// Hmmm, who might we be waiting for?
	//			r = stack.mResumedActivity;
	//			if (r == null) {
	//				r = stack.mPausingActivity;
	//			}
	//			// Both of those null?  Fall back to 'this' again
	//			if (r == null) {
	//				r = this;
	//			}
	//		}
	//
	//		return r;
	//	}

	//	public boolean keyDispatchingTimedOut() {
	//		ActivityRecord r;
	//		ProcessRecord anrApp = null;
	//		synchronized (service) {
	//			r = getWaitingHistoryRecordLocked();
	//			if (r != null && r.app != null) {
	//				if (r.app.debugging) {
	//					return false;
	//				}
	//
	//				if (service.mDidDexOpt) {
	//					// Give more time since we were dexopting.
	//					service.mDidDexOpt = false;
	//					return false;
	//				}
	//
	//				if (r.app.instrumentationClass == null) {
	//					anrApp = r.app;
	//				} else {
	//					Bundle info = new Bundle();
	//					info.putString("shortMsg", "keyDispatchingTimedOut");
	//					info.putString("longMsg",
	//							"Timed out while dispatching key event");
	//					service.finishInstrumentationLocked(r.app,
	//							Activity.RESULT_CANCELED, info);
	//				}
	//			}
	//		}
	//
	//		if (anrApp != null) {
	//			service.appNotResponding(anrApp, r, this, "keyDispatchingTimedOut");
	//		}
	//
	//		return true;
	//	}

	/** Returns the key dispatching timeout for this application token. */
	//	public long getKeyDispatchingTimeout() {
	//		synchronized (service) {
	//			ActivityRecord r = getWaitingHistoryRecordLocked();
	//			if (r == null || r.app == null
	//					|| r.app.instrumentationClass == null) {
	//				return ActivityManager.KEY_DISPATCHING_TIMEOUT;
	//			}
	//
	//			return ActivityManager.INSTRUMENTATION_KEY_DISPATCHING_TIMEOUT;
	//		}
	//	}

	/**
	 * This method will return true if the activity is either visible, is becoming visible, is
	 * currently pausing, or is resumed.
	 */
	public boolean isInterestingToUserLocked() {
		return visible || nowVisible || state == ActivityState.PAUSING
				|| state == ActivityState.RESUMED;
	}

	public String toString() {
		if (stringName != null) {
			return stringName;
		}
		StringBuilder sb = new StringBuilder(128);
		sb.append("HistoryRecord{");
		sb.append(this.packageName);
		sb.append(' ');
		sb.append(intent.getComponent().flattenToShortString());
		sb.append('}');
		return stringName = sb.toString();
	}
}
