package com.fobumi.clevermessenger.utilities;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;

import com.fobumi.clevermessenger.data.Contact;

import java.util.ArrayList;

// Utility class to access contacts from content provider
public final class ContactUtils {
    // Return all contacts that have a phone number
    public static ArrayList<Contact> getContactNames(Context context) {
        ArrayList<Contact> contactNames = new ArrayList<>();

        String[] projection = {
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.LOOKUP_KEY,
                Contacts.HAS_PHONE_NUMBER};

        Cursor cursor = context.getContentResolver().query(
                Contacts.CONTENT_URI,
                projection,
                null,
                null,
                Contacts.DISPLAY_NAME_PRIMARY + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int hasPhone = cursor.getInt(cursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER));
                String contactName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY));
                String lookupKey = cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY));
                if (hasPhone == 1) {
                    contactNames.add(new Contact(contactName, lookupKey));
                }
            }
            cursor.close();
        }

        return contactNames;
    }

    // Return the phone number of a specific contact
    public static String getNumberFromContact(Context context, Contact contact) {
        String selection = CommonDataKinds.Phone.LOOKUP_KEY + "=?";
        String[] selectionArgs = {contact.getLookupKey()};

        Cursor cursor = context.getContentResolver().query(
                CommonDataKinds.Phone.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(CommonDataKinds.Phone.NORMALIZED_NUMBER);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }
}
