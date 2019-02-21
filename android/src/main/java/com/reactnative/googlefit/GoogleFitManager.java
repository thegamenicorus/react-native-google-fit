/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * Based on Asim Malik android source code, copyright (c) 2015
 **/
package com.reactnative.googlefit;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.Fitness.*;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.DataType;


public class GoogleFitManager implements ActivityEventListener {

    private ReactContext mReactContext;
    private GoogleApiClient mApiClient;
    private static final int REQUEST_OAUTH = 1001;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean mAuthInProgress = false;
    private Activity mActivity;

    private DistanceHistory distanceHistory;
    private StepHistory stepHistory;
    private BodyHistory bodyHistory;
    private CalorieHistory calorieHistory;
    private StepCounter mStepCounter;
    private StepSensor stepSensor;
    private RecordingApi recordingApi;
    private ActivityHistory activityHistory;

    private static final String TAG = "RNGoogleFit";

    public GoogleFitManager(ReactApplicationContext reactContext, Activity activity) {

        //Log.i(TAG, "Initializing GoogleFitManager" + mAuthInProgress);
        this.mReactContext = reactContext;
        this.mActivity = activity;

        mReactContext.addActivityEventListener(this);

        this.mStepCounter = new StepCounter(mReactContext, this, activity);
        this.stepHistory = new StepHistory(mReactContext, this);
        this.bodyHistory = new BodyHistory(mReactContext, this);
        this.distanceHistory = new DistanceHistory(mReactContext, this);
        this.calorieHistory = new CalorieHistory(mReactContext, this);
        this.recordingApi = new RecordingApi(mReactContext, this);
        this.activityHistory = new ActivityHistory(mReactContext, this);
        //        this.stepSensor = new StepSensor(mReactContext, activity);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mApiClient;
    }

    public RecordingApi getRecordingApi() {
        return recordingApi;
    }

    public StepCounter getStepCounter() {
        return mStepCounter;
    }

    public StepHistory getStepHistory() {
        return stepHistory;
    }

    public BodyHistory getBodyHistory() {
        return bodyHistory;
    }

    public DistanceHistory getDistanceHistory() {
        return distanceHistory;
    }

    public void resetAuthInProgress() {
        if (!isAuthorized()) {
            mAuthInProgress = false;
        }
    }

    public CalorieHistory getCalorieHistory() {
        return calorieHistory;
    }

    public void authorize() {
        FitnessOptions fitnessOptions = getFitnessOptions();
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(mReactContext);

        if (!isAuthorized()) {
            Log.i(TAG, "Authorization requestPermissions");
            GoogleSignIn.requestPermissions(mReactContext.getCurrentActivity(), REQUEST_OAUTH, googleAccount, fitnessOptions);
        } else {
            Log.i(TAG, "Authorization - Connected");
            sendEvent(mReactContext, "GoogleFitAuthorizeSuccess", null);
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Authorization onActivityResult - requestCode: " + requestCode);
        Log.i(TAG, "Authorization onActivityResult - resultCode: " + resultCode);

        if (requestCode == REQUEST_OAUTH) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Authorization onActivityResult - Connected");
                sendEvent(mReactContext, "GoogleFitAuthorizeSuccess", null);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e(TAG, "Authorization onActivityResult - Cancel");
                WritableMap map = Arguments.createMap();
                map.putString("message", "RESULT_CANCELED");
                sendEvent(mReactContext, "GoogleFitAuthorizeFailure", map);
            }
        }
    }


    public void disconnect() {
        if (mApiClient != null) {
            GoogleSignInAccount gsa = GoogleSignIn.getAccountForScopes(mReactContext, new Scope(Scopes.FITNESS_ACTIVITY_READ));
            Fitness.getConfigClient(mReactContext, gsa).disableFit();
            mApiClient.disconnect();
        }
    }

    public boolean isAuthorized() {
        GoogleSignInAccount googleAccount = GoogleSignIn.getAccountForExtension(mReactContext, getFitnessOptions());

        if (googleAccount == null) {
            Log.i(TAG, "isAuthorized hasPermission: " + false);
            return false;
        }

        boolean hasPermission = GoogleSignIn.hasPermissions(googleAccount, getFitnessOptions());

        Log.i(TAG, "isAuthorized hasPermission: " + hasPermission);
        Log.i(TAG, "isAuthorized googleAccount: " + googleAccount.getGrantedScopes());

        return hasPermission;
    }

    protected void stop() {
        if (mApiClient != null) {
            Fitness.SensorsApi.remove(mApiClient, mStepCounter)
                    .setResultCallback((status) -> {
                        if (status.isSuccess()) {
                            mApiClient.disconnect();
                        }
                    });
        }
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    public ActivityHistory getActivityHistory() {
        return activityHistory;
    }

    public void setActivityHistory(ActivityHistory activityHistory) {
        this.activityHistory = activityHistory;
    }

    public static class GoogleFitCustomErrorDialig extends ErrorDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(AUTH_PENDING);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_OAUTH);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mAuthInProgress = false;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        if (mActivity != null) {
            // Create a fragment for the error dialog
            GoogleFitCustomErrorDialig dialogFragment = new GoogleFitCustomErrorDialig();
            // Pass the error that should be displayed
            Bundle args = new Bundle();
            args.putInt(AUTH_PENDING, errorCode);
            dialogFragment.setArguments(args);
            dialogFragment.show(mActivity.getFragmentManager(), "error dialog");
        }
    }

    private FitnessOptions getFitnessOptions() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
//                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_BASAL_METABOLIC_RATE, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .build();
        return fitnessOptions;
    }
}
