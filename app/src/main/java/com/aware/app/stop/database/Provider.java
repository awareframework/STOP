package com.aware.app.stop.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.app.stop.database.provider.stop_project";

    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * Database stored in external folder: /AWARE/stop.db
     */
    public static final String DATABASE_NAME = "stop.db";

    //Database table names
    public static final String DB_TBL_GAME = "ball_game";
    public static final String DB_TBL_MEDICATION = "medication";
    public static final String DB_TBL_FEEDBACK = "feedback";
    public static final String DB_TBL_NOTIFICATION = "notification_data";
    public static final String DB_TBL_HEALTH = "health";
    public static final String DB_TBL_CONSENT = "consent";

    //ContentProvider query indexes
    private static final int TABLE_GAME_DIR = 1;
    private static final int TABLE_GAME_ITEM = 2;
    private static final int TABLE_MEDICATION_DIR = 3;
    private static final int TABLE_MEDICATION_ITEM = 4;
    private static final int TABLE_FEEDBACK_DIR = 5;
    private static final int TABLE_FEEDBACK_ITEM = 6;
    private static final int TABLE_NOTIFICATION_DIR = 7;
    private static final int TABLE_NOTIFICATION_ITEM = 8;
    private static final int TABLE_HEALTH_DIR = 9;
    private static final int TABLE_HEALTH_ITEM = 10;
    private static final int TABLE_CONSENT_DIR = 11;
    private static final int TABLE_CONSENT_ITEM = 12;

    /**
     * Database tables:
     * - ball game data, medication data, feedback, notification data, health, consent
     */
    public static final String[] DATABASE_TABLES = {
            DB_TBL_GAME, DB_TBL_MEDICATION, DB_TBL_FEEDBACK, DB_TBL_NOTIFICATION, DB_TBL_HEALTH, DB_TBL_CONSENT
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    /**
     * Game table
     */
    public static final class Game_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_GAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.ball_game";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.ball_game";

        public static final String DATA = "data";
    }

    //Game table fields
    private static final String DB_TBL_GAME_FIELDS =
            Game_Data._ID + " integer primary key autoincrement," +
                    Game_Data.TIMESTAMP + " real default 0," +
                    Game_Data.DEVICE_ID + " text default ''," +
                    Game_Data.DATA + " longtext default ''";

    /**
     * Medication table
     */
    public static final class Medication_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_MEDICATION);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.medication";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.medication";

        public static final String MEDICATION_TIMESTAMP = "double_medication";
    }

    //Medication table fields
    private static final String DB_TBL_MEDICATION_FIELDS =
            Medication_Data._ID + " integer primary key autoincrement," +
                    Medication_Data.TIMESTAMP + " real default 0," +
                    Medication_Data.MEDICATION_TIMESTAMP + " real default 0," +
                    Medication_Data.DEVICE_ID + " text default ''";

    /**
     * Feedback table
     */
    public static final class Feedback_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_FEEDBACK);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.feedback";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.feedback";

        public static final String DEVICE_NAME = "device_name";
        public static final String FEEDBACK = "feedback";
    }

    //Feedback table fields
    private static final String DB_TBL_FEEDBACK_FIELDS =
            Feedback_Data._ID + " integer primary key autoincrement," +
                    Feedback_Data.TIMESTAMP + " real default 0," +
                    Feedback_Data.DEVICE_ID + " text default ''," +
                    Feedback_Data.DEVICE_NAME + " text default ''," +
                    Feedback_Data.FEEDBACK + " text default ''";

    /**
     * Notificaion table
     */
    public static final class Notification_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_NOTIFICATION);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.notification_data";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.notification_data";

        public static final String EVENT = "event";
    }

    //Notification table fields
    private static final String DB_TBL_NOTIFICATION_FIELDS =
            Notification_Data._ID + " integer primary key autoincrement," +
                    Notification_Data.TIMESTAMP + " real default 0," +
                    Notification_Data.DEVICE_ID + " text default ''," +
                    Notification_Data.EVENT + " text default ''";

    /**
     * Health table
     */
    public static final class Health_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_HEALTH);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.health";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.health";

        public static final String PD_VALUE = "pd_value";
    }

    //Health table fields
    private static final String DB_TBL_HEALTH_FIELDS =
            Health_Data._ID + " integer primary key autoincrement," +
                    Health_Data.TIMESTAMP + " real default 0," +
                    Health_Data.DEVICE_ID + " text default ''," +
                    Health_Data.PD_VALUE + " text default ''";

    /**
     * Consent table
     */
    public static final class Consent_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_CONSENT);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.consent";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.consent";

        public static final String USER_DATA = "user_data";

    }

    //Health table fields
    private static final String DB_TBL_CONSENT_FIELDS =
            Consent_Data._ID + " integer primary key autoincrement," +
                    Consent_Data.TIMESTAMP + " real default 0," +
                    Consent_Data.DEVICE_ID + " text default ''," +
                    Consent_Data.USER_DATA + " text default ''";


    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_GAME_FIELDS, DB_TBL_MEDICATION_FIELDS, DB_TBL_FEEDBACK_FIELDS,
            DB_TBL_NOTIFICATION_FIELDS, DB_TBL_HEALTH_FIELDS, DB_TBL_CONSENT_FIELDS
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
    //--

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> tableGameHash;
    private HashMap<String, String> tableMedicationHash;
    private HashMap<String, String> tableFeedbackHash;
    private HashMap<String, String> tableNotificationHash;
    private HashMap<String, String> tableHealthHash;
    private HashMap<String, String> tableConsentHash;

    /**
     * Returns the provider authority that is dynamic
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".database.provider.stop_project";
        return AUTHORITY;
    }

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".database.provider.stop_project";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //Game table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], TABLE_GAME_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", TABLE_GAME_ITEM);

        //Medication table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], TABLE_MEDICATION_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", TABLE_MEDICATION_ITEM);

        //Feedback table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2], TABLE_FEEDBACK_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2] + "/#", TABLE_FEEDBACK_ITEM);

        //Notification table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[3], TABLE_NOTIFICATION_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[3] + "/#", TABLE_NOTIFICATION_ITEM);

        //Health table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[4], TABLE_HEALTH_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[4] + "/#", TABLE_HEALTH_ITEM);

        //Health table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[5], TABLE_CONSENT_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[5] + "/#", TABLE_CONSENT_ITEM);

        //Game table HasMap
        tableGameHash = new HashMap<>();
        tableGameHash.put(Game_Data._ID, Game_Data._ID);
        tableGameHash.put(Game_Data.TIMESTAMP, Game_Data.TIMESTAMP);
        tableGameHash.put(Game_Data.DEVICE_ID, Game_Data.DEVICE_ID);
        tableGameHash.put(Game_Data.DATA, Game_Data.DATA);

        //Medication table HasMap
        tableMedicationHash = new HashMap<>();
        tableMedicationHash.put(Medication_Data._ID, Medication_Data._ID);
        tableMedicationHash.put(Medication_Data.TIMESTAMP, Medication_Data.TIMESTAMP);
        tableMedicationHash.put(Medication_Data.MEDICATION_TIMESTAMP, Medication_Data.MEDICATION_TIMESTAMP);
        tableMedicationHash.put(Medication_Data.DEVICE_ID, Medication_Data.DEVICE_ID);

        //Feedback table HasMap
        tableFeedbackHash = new HashMap<>();
        tableFeedbackHash.put(Feedback_Data._ID, Feedback_Data._ID);
        tableFeedbackHash.put(Feedback_Data.TIMESTAMP, Feedback_Data.TIMESTAMP);
        tableFeedbackHash.put(Feedback_Data.DEVICE_ID, Feedback_Data.DEVICE_ID);
        tableFeedbackHash.put(Feedback_Data.DEVICE_NAME, Feedback_Data.DEVICE_NAME);
        tableFeedbackHash.put(Feedback_Data.FEEDBACK, Feedback_Data.FEEDBACK);

        //Notification table HasMap
        tableNotificationHash = new HashMap<>();
        tableNotificationHash.put(Notification_Data._ID, Notification_Data._ID);
        tableNotificationHash.put(Notification_Data.TIMESTAMP, Notification_Data.TIMESTAMP);
        tableNotificationHash.put(Notification_Data.DEVICE_ID, Notification_Data.DEVICE_ID);
        tableNotificationHash.put(Notification_Data.EVENT, Notification_Data.EVENT);

        //Health table HasMap
        tableHealthHash = new HashMap<>();
        tableHealthHash.put(Health_Data._ID, Health_Data._ID);
        tableHealthHash.put(Health_Data.TIMESTAMP, Health_Data.TIMESTAMP);
        tableHealthHash.put(Health_Data.DEVICE_ID, Health_Data.DEVICE_ID);
        tableHealthHash.put(Health_Data.PD_VALUE, Health_Data.PD_VALUE);

        //Consent table HasMap
        tableConsentHash = new HashMap<>();
        tableConsentHash.put(Consent_Data._ID, Consent_Data._ID);
        tableConsentHash.put(Consent_Data.TIMESTAMP, Consent_Data.TIMESTAMP);
        tableConsentHash.put(Consent_Data.DEVICE_ID, Consent_Data.DEVICE_ID);
        tableConsentHash.put(Consent_Data.USER_DATA, Consent_Data.USER_DATA);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            case TABLE_MEDICATION_DIR:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;

            case TABLE_FEEDBACK_DIR:
                count = database.delete(DATABASE_TABLES[2], selection, selectionArgs);
                break;

            case TABLE_NOTIFICATION_DIR:
                count = database.delete(DATABASE_TABLES[3], selection, selectionArgs);
                break;

            case TABLE_HEALTH_DIR:
                count = database.delete(DATABASE_TABLES[4], selection, selectionArgs);
                break;

            case TABLE_CONSENT_DIR:
                count = database.delete(DATABASE_TABLES[5], selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                long game_id = database.insert(DATABASE_TABLES[0], Game_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (game_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Game_Data.CONTENT_URI, game_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case TABLE_MEDICATION_DIR:
                long medication_id = database.insert(DATABASE_TABLES[1], Medication_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (medication_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Medication_Data.CONTENT_URI, medication_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case TABLE_FEEDBACK_DIR:
                long feedback_id = database.insert(DATABASE_TABLES[2], Feedback_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (feedback_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Feedback_Data.CONTENT_URI, feedback_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case TABLE_NOTIFICATION_DIR:
                long notification_id = database.insert(DATABASE_TABLES[3], Notification_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (notification_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Notification_Data.CONTENT_URI, notification_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case TABLE_HEALTH_DIR:
                long health_id = database.insert(DATABASE_TABLES[4], Health_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (health_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Health_Data.CONTENT_URI, health_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case TABLE_CONSENT_DIR:
                long consent_id = database.insert(DATABASE_TABLES[5], Consent_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (consent_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Consent_Data.CONTENT_URI, consent_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableGameHash); //the hashmap of the table
                break;

            case TABLE_MEDICATION_DIR:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(tableMedicationHash); //the hashmap of the table
                break;

            case TABLE_FEEDBACK_DIR:
                qb.setTables(DATABASE_TABLES[2]);
                qb.setProjectionMap(tableFeedbackHash); //the hashmap of the table
                break;

            case TABLE_NOTIFICATION_DIR:
                qb.setTables(DATABASE_TABLES[3]);
                qb.setProjectionMap(tableNotificationHash); //the hashmap of the table
                break;

            case TABLE_HEALTH_DIR:
                qb.setTables(DATABASE_TABLES[4]);
                qb.setProjectionMap(tableHealthHash); //the hashmap of the table
                break;

            case TABLE_CONSENT_DIR:
                qb.setTables(DATABASE_TABLES[5]);
                qb.setProjectionMap(tableConsentHash); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                return Game_Data.CONTENT_TYPE;
            case TABLE_GAME_ITEM:
                return Game_Data.CONTENT_ITEM_TYPE;

            case TABLE_MEDICATION_DIR:
                return Medication_Data.CONTENT_TYPE;
            case TABLE_MEDICATION_ITEM:
                return Medication_Data.CONTENT_ITEM_TYPE;

            case TABLE_FEEDBACK_DIR:
                return Feedback_Data.CONTENT_TYPE;
            case TABLE_FEEDBACK_ITEM:
                return Feedback_Data.CONTENT_ITEM_TYPE;

            case TABLE_NOTIFICATION_DIR:
                return Notification_Data.CONTENT_TYPE;
            case TABLE_NOTIFICATION_ITEM:
                return Notification_Data.CONTENT_ITEM_TYPE;

            case TABLE_HEALTH_DIR:
                return Health_Data.CONTENT_TYPE;
            case TABLE_HEALTH_ITEM:
                return Health_Data.CONTENT_ITEM_TYPE;

            case TABLE_CONSENT_DIR:
                return Consent_Data.CONTENT_TYPE;
            case TABLE_CONSENT_ITEM:
                return Consent_Data.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;

            case TABLE_MEDICATION_DIR:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;

            case TABLE_FEEDBACK_DIR:
                count = database.update(DATABASE_TABLES[2], values, selection, selectionArgs);
                break;

            case TABLE_NOTIFICATION_DIR:
                count = database.update(DATABASE_TABLES[3], values, selection, selectionArgs);
                break;

            case TABLE_HEALTH_DIR:
                count = database.update(DATABASE_TABLES[4], values, selection, selectionArgs);
                break;

            case TABLE_CONSENT_DIR:
                count = database.update(DATABASE_TABLES[5], values, selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);

        return count;
    }
}
