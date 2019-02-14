# react-native-google-fit

[![npm version](https://badge.fury.io/js/react-native-google-fit.svg)](https://badge.fury.io/js/react-native-google-fit) ![Downloads](https://img.shields.io/npm/dm/react-native-google-fit.svg)

A React Native bridge module for interacting with Google Fit

### Getting started

`$ npm install react-native-google-fit --save`

### Enable Google Fitness API for your application

In order for your app to communicate properly with the Google Fitness API you need to enable Google Fit API in your Google API Console.
Also you need to generate new client ID for your app and provide both debug and release SHA keys.
Another step is to configure the consent screen, etc.

More detailed info available at
https://developers.google.com/fit/android/get-api-key

```
1. In order for the library to work correctly, you'll need following SDK setups:
   
   Android Support Repository
   Android Support Library
   Google Play services
   Google Repository
   Google Play APK Expansion Library
   
2. In order for your app to communicate properly with the Google Fitness API,
   you need to provide the SHA1 sum of the certificate used for signing your
   application to Google. This will enable the GoogleFit plugin to communicate
   with the Fit application in each smartphone where the application is installed.
   https://developers.google.com/fit/android/get-api-key
```

### Mostly Automatic installation

`$ react-native link react-native-google-fit`

then pass your package name to the module in MainApplication.java (google fit requires package name to save data)

`new GoogleFitPackage(BuildConfig.APPLICATION_ID)`
_**Note**: Do not change BuildConfig.APPLICATION_ID - it's a constant value._

### Manual installation

1. Open up `android/app/src/main/java/[...]/MainApplication.java`

    * Add `import com.reactnative.googlefit.GoogleFitPackage;` to the imports at the top of the file
    * Add `new GoogleFitPackage(BuildConfig.APPLICATION_ID),` to the list returned by the `getPackages()` method.
    _**Note**: Do not change BuildConfig.APPLICATION_ID - it's a constant value._

2. Append the following lines to `android/settings.gradle`:
   ```
   include ':react-native-google-fit'
   project(':react-native-google-fit').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-google-fit/android')
   ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:

   ```
     compile project(':react-native-google-fit')
   ```

### USAGE

1. `import GoogleFit from 'react-native-google-fit';`

2. Authorize:

    ```javascript
    GoogleFit.onAuthorize(() => {
      dispatch('AUTH SUCCESS');
    });
    
    GoogleFit.onAuthorizeFailure(() => {
      dispatch('AUTH ERROR');
    });
    
    GoogleFit.authorize();
    
    // ...
    // Call when authorized
    GoogleFit.startRecording((callback) => {
      // Process data from Google Fit Recording API (no google fit app needed)
    });
    ```

3. Retrieve Steps For Period

    ```javascript
    const options = {
      startDate: "2017-01-01T00:00:17.971Z", // required ISO8601Timestamp
      endDate: new Date().toISOString() // required ISO8601Timestamp
    };
    
    GoogleFit.getDailyStepCountSamples(options, (err, res) => {
      if (err) {
        throw err;
      }
    
      console.log("Daily steps >>>", res);
    });
    ```

**Response:**

```javascript
[
  { source: "com.google.android.gms:estimated_steps", steps: [] },
  { source: "com.google.android.gms:merge_step_deltas", steps: [] },
  { source: "com.xiaomi.hm.health", steps: [] }
];
```

4. Retrieve Weights

    ```javascript
    const opt = {
      unit: "pound", // required; default 'kg'
      startDate: "2017-01-01T00:00:17.971Z", // required
      endDate: new Date().toISOString(), // required
      ascending: false // optional; default false
    };
    
    GoogleFit.getWeightSamples(opt, (err, res) => {
      console.log(res);
    });
    ```
    
5. Retrieve Heights

    ```javascript
    const opt = {
      startDate: "2017-01-01T00:00:17.971Z", // required
      endDate: new Date().toISOString(), // required
    };
    
    GoogleFit.getHeightSamples(opt, (err, res) => {
      console.log(res);
    });
    ```

6. Save Weights

    ```javascript
    const opt = {
      value: 200,
      date: new Date().toISOString(),
      unit: "pound"
    };
    
    GoogleFit.saveWeight(opt, (err, res) => {
      if (err) throw "Cant save data to the Google Fit";
    });
    ```
    
7. Get all activities
    ```javascript
      let options = {
        startDate: new Date(2018, 9, 17).valueOf(), // simply outputs the number of milliseconds since the Unix Epoch
        endDate: new Date(2018, 9, 18).valueOf()
      };
      GoogleFit.getActivitySamples(options, (err, res) => {
        console.log(err, res)
      });
    ```
    response:
    ```javascript
     [ { 
      sourceName: 'Android',
      device: 'Android',
      sourceId: 'com.google.android.gms',
      calories: 764.189208984375,
      quantity: 6,
      end: 1539774300992,
      tracked: true,
      activityName: 'still',
      start: 1539727200000 },
    { sourceName: 'Android',
      device: 'Android',
      sourceId: 'com.google.android.gms',
      calories: 10.351096153259277,
      quantity: 138,
      end: 1539774486088,
      tracked: true,
      distance: 88.09545135498047,
      activityName: 'walking',
    }]
    ```
    Where:
    ```
    sourceName = device - 'Android' or 'Android Wear' string
    sourceId - return a value of dataSource.getAppPackageName(). For more info see: https://developers.google.com/fit/android/data-attribution
    start/end - timestamps of activity in format of milliseconds since the Unix Epoch
    tracked - bool flag, is this activity was entered by user or tracked by device. Detected by checking milliseconds of start/end timestamps. Since when user log activity in googleFit they can't set milliseconds
    distance(opt) - A distance in meters.
    activityName - string, equivalent one of these https://developers.google.com/fit/rest/v1/reference/activity-types 
    calories(opt) - double value of burned Calories in kcal.
    quantity(opt) - equivalent of steps number
    ```
    Note that optional parametrs are not presented in all activities - only where google fit return some results for this field.
    Like no distance for still activity. 

8. Other methods:

    ```javascript
    observeSteps(callback); // On Step Changed Event
    
    unsubscribeListeners(); // Put into componentWillUnmount() method to prevent leaks
    
    getDailyCalorieSamples(options, callback); // method to get calories per day
    
    getDailyDistanceSamples(options, callback); // method to get daily distance
    
    isAvailable(callback: (available: boolean)); // Checks is GoogleFit available for current account / installed on device
    
    isEnabled(callback: (enabled: boolean)); // Checks is permissions granted
    
    deleteWeight(options, callback); // method to delete weights by options (same as in save weights)
 
    openFit(); //method to open google fit app
    
    saveHeight(options, callback);
 
    deleteHeight(options, callback);
 
    deleteWeight(options, callback);
 
    disconnect(); // Closes the connection to Google Play services.
    ```

### Changelog:

```
0.7.1   - Fix for disconnect() (@dmitriys-lits thanks for the PR)
0.7     - Retrieve Heights, open fit activity, unified body method (@EJohnF thanks for the PR!)

0.6     - RN 0.56+ support (@skb1129 thanks for the PR)
        - nutrition scenario (@13thdeus thanks for the PR)
        
0.5     - New auth process (@priezz thanks for PR)
        - Fix unsubscribe listeners
        - Readme refactoring
        
0.4.0-beta
        - Recording API implemetation (@reboss thanks for PR)
        - Just use startRecording(callback) function which listens
        to STEPS and DISTANCE (for now) activities from Google Fitness
        API (no need to install Google fit app)

0.3.5   - Fix Error: Fragments should be static
        - Updated readme

0.3.4   - Burned Calories History (getDailyCalorieSamples)

0.3.2
        - React Native 0.46 Support

0.3.1-beta
        - better cancel/deny support

0.3.0-beta (@firodj thanks for this PR!)
        - steps adapter to avoid errors;
        - authorize: allow cancel;
        - authorize: using callback instead event;
        - strict dataSource;
        - xiaomi support;

0.2.0   - getDailyDistanceSamples();
        - isAvailable();
        - isEnabled();
        - deleteWeight();
0.1.1-beta
        - getDailyStepCountSamples method compatible with Apple Healthkit module
        - started to implement JSDoc documentation

0.1.0
        - getting activity within module itself
        - fixed package name dependency
        - provided more detailed documentation

0.0.9   - Weights Save Support
        - Refactor methods to be compatible with react-native-apple-healthkit module
        - Remove 'moment.js' dependency

0.0.8   - Weights Samples support

0.0.1   - 0.0.7 Initial builds
```

### PLANS / TODO

* code refactoring
* optimization

Copyright (c) 2017-present, Stanislav Doskalenko
doskalenko.s@gmail.com

Based on Asim Malik android source code, copyright (c) 2015, thanks mate!
