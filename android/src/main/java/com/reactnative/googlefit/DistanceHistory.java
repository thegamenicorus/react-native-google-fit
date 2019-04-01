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

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class DistanceHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "DistanceHistory";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public DistanceHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public void aggregateDataByInterval(int minutes,
                                        long startTime,
                                        long endTime,
                                        Callback errorCallback,
                                        Callback successCallback) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        dateFormat.setTimeZone(TimeZone.getDefault());

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(minutes, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Log.i(TAG, "aggregateDataByInterval DistanceHistory ");

        Fitness.getHistoryClient(mReactContext, GoogleSignIn.getLastSignedInAccount(mReactContext))
                .readData(readRequest)
                .addOnSuccessListener((dataReadResult) -> {
                    WritableArray map = Arguments.createArray();

                    //Used for aggregated data
                    if (dataReadResult.getBuckets().size() > 0) {
                        Log.i(TAG, "Number of buckets: " + dataReadResult.getBuckets().size());
                        for (Bucket bucket : dataReadResult.getBuckets()) {
                            List<DataSet> dataSets = bucket.getDataSets();
                            for (DataSet dataSet : dataSets) {
                                processIntervalDataSet(dataSet, map);
                            }
                        }
                    }
                    //Used for non-aggregated data
                    else if (dataReadResult.getDataSets().size() > 0) {
                        Log.i(TAG, "Number of returned DataSets: " + dataReadResult.getDataSets().size());
                        for (DataSet dataSet : dataReadResult.getDataSets()) {
                            processIntervalDataSet(dataSet, map);
                        }
                    }

                    successCallback.invoke(map);
                })
                .addOnFailureListener((e) -> {
                    Log.i(TAG, "Aggregate data failed: " + e);
//                    errorCallback.invoke(e);
                })
                .addOnCompleteListener((dataReadResult) -> {
                    Log.i(TAG, "Aggregate onComplete()");
                });
    }

    public ReadableArray aggregateDataByDate(long startTime, long endTime) {

        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        //Check how much distance were walked and recorded in specified days
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);


        WritableArray map = Arguments.createArray();

        // Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    processDataSet(dataSet, map);
                }
            }
        }
        // Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map);
            }
        }

        return map;
    }


    private void processDataSet(DataSet dataSet, WritableArray map) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();
        Format formatter = new SimpleDateFormat("EEE");

        WritableMap stepMap = Arguments.createMap();


        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));

            String day = formatter.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "Day: " + day);

            for (Field field : dp.getDataType().getFields()) {
                Log.i("History", "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));

                stepMap.putString("day", day);
                stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("distance", dp.getValue(field).asFloat());
                map.pushMap(stepMap);
            }
        }
    }

    private void processIntervalDataSet(DataSet dataSet, WritableArray map) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        WritableMap stepMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            for (Field field : dp.getDataType().getFields()) {
                stepMap.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                stepMap.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                stepMap.putDouble("value", dp.getValue(field).asFloat());
                map.pushMap(stepMap);
            }
        }
    }

}
