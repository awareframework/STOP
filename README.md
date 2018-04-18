# STOP: Sentient Tracking of Parkinson's
STOP is the application for regular informing Parkinson's Disease patients on their medication adherence

[![STOP | How to use](https://i.imgur.com/GUFlvyv.png)](https://www.youtube.com/watch?v=4_Tl7sEJI44)

## Features

### Ball game
The built-in game that asks a user to keep the ball in the center of the circles as still as possible. The game tracks and records accelerometer, linear accelerometer, gyroscope and rotation data that is used for the esimation of PD severity level of a user. The size of the ball and circles, ball speed and game time are customizable in the application settings.

### Medication journal
Journal for recording medication sessions time to follow the medication adherece. Timestams recording is available either with intelligence voice recognition or with manual input.

### Notifications
The application triggers notifications four times per day to remind user to play a game and record last medication sessions and once per day at 10:00 to ask user to report the severity level of PD for the previous day.

### Feedback
The feedback for the application can be sent straight from the application. Both manual input and voice recognition are supported.

## Data collected

### Ball game data
> content://com.aware.app.stop.database.provider.stop_project/ball_game

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
data | LONGTEXT | ball game data in JSON forma: ball size; ball speed; device screen resolution; game score; accelerometer, linear accelerometer, gyroscope and rotation samplings

### Medication data
> content://com.aware.app.stop.database.provider.stop_project/medication

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds when record is added to database
medication_timestamp | REAL | unix timestamp in milliseconds when medication has been taken
device_id | TEXT | AWARE device ID

### Health state survey data
> content://com.aware.app.stop.database.provider.stop_project/health

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of record
device_id | TEXT | AWARE device ID
pd_value | TEXT | survery response (none, some, severe)

### Notfication data
> content://com.aware.app.stop.database.provider.stop_project/notification_data

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of event
device_id | TEXT | AWARE device ID
event | TEXT | type of notification state (morning_shown, morning_opened, noon_shown, etc.)

### Feedback
> content://com.aware.app.stop.database.provider.stop_project/feedback

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of record
device_id | TEXT | AWARE device ID
device_name | TEXT | device manufacturer and model information
feedback | TEXT | user's feedback

## Authors
Created by [Valerii Kan](https://github.com/valeriikan) at the Center for Ubiquitous Computing for the [STOP: Sentient Tracking of Parkinson's research project](http://ubicomp.oulu.fi/stop-sentient-tracking-of-parkinsons-funded-by-the-academy-of-finland-ict-2023-programme/) funded by the Academy of Finland.
