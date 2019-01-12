package com.fobumi.clevermessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.fobumi.clevermessenger.data.Message;
import com.fobumi.clevermessenger.utilities.QueryUtils;

import java.net.URL;

public class SMSReceiver extends BroadcastReceiver {
    // Reference to ChatActivity's onReceiverListener to update it when message received
    private OnReceiveListener mOnReceiveListener;

    // Store current number and cleverbot state (stores conversation history)
    private String mPhoneNumber;
    private String mCleverbotState;

    // Auto-reply flag
    private boolean mAutoReply = false;

    public SMSReceiver(OnReceiveListener onReceiveListener) {
        mOnReceiveListener = onReceiveListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.isEmpty(mPhoneNumber)) {
            return;
        }

        Bundle intentExtras = intent.getExtras();
        SmsMessage smsMessage = null;
        StringBuilder contents = new StringBuilder();

        // Fetch SMS message from intent
        if (intentExtras != null) {
            Object[] pdus = (Object[]) intentExtras.get("pdus");
            for (int i = 0; i < pdus.length; i++) {
                smsMessage = Build.VERSION.SDK_INT >= 19
                        ? Telephony.Sms.Intents.getMessagesFromIntent(intent)[i]
                        : SmsMessage.createFromPdu((byte[]) pdus[i]);
                contents.append(smsMessage.getMessageBody());
            }
        }

        // If message is from current receiver/number
        if (smsMessage.getDisplayOriginatingAddress().contains(mPhoneNumber)) {
            Message message = new Message(contents.toString(), System.currentTimeMillis(), true);

            // Notify ChatActivity so that UI can be updated with new message
            mOnReceiveListener.onReceive(message);

            // If auto-reply option enabled, get response from Cleverbot API
            if (mAutoReply) {
                new GetResponseTask().execute(mCleverbotState, contents.toString());
            }
        }
    }

    // Update current receiver/number from ChatActivity
    public void setPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    // Set auto-reply flag from ChatActivity
    public void setAutoReply(boolean autoReply) {
        mAutoReply = autoReply;
    }

    // Interface used to communicate with ChatActivity and UI
    public interface OnReceiveListener {
        void onReceive(Message message);
    }

    // AsyncTask to get response from Cleverbot API
    private class GetResponseTask extends AsyncTask<String, Integer, String[]> {

        @Override
        protected String[] doInBackground(String... strings) {
            // Create request URL from conversation state and receiver's reply
            URL requestUrl = QueryUtils.createRequestUrl(
                    strings[0],
                    strings[1]);

            // Delay reply to simulate real reply
            SystemClock.sleep(5000);

            return QueryUtils.getResponse(requestUrl);
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if (strings.length == 0) {
                return;
            }

            // Update cleverbot state
            mCleverbotState = strings[0];

            // Send text message with Cleverbot's reply
            SmsManager.getDefault().sendTextMessage(
                    mPhoneNumber,
                    null,
                    strings[1],
                    null,
                    null);

            // Update UI with new message
            Message message = new Message(strings[1], System.currentTimeMillis(), false);
            mOnReceiveListener.onReceive(message);
        }

    }
}
