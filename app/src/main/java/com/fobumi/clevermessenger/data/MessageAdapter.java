package com.fobumi.clevermessenger.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fobumi.clevermessenger.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MessageAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_SEND = 1;
    private static final int VIEW_TYPE_RECEIVE = 2;

    private Context mContext;
    private List<Message> mMessageList;

    public MessageAdapter(Context context, List<Message> messageList) {
        mContext = context;
        mMessageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        if (mMessageList.get(position).isReceived()) {
            return VIEW_TYPE_RECEIVE;
        } else {
            return VIEW_TYPE_SEND;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView;

        // Show correct view based on whether the message was sent or received
        if (viewType == VIEW_TYPE_SEND) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item_send, viewGroup, false);
            return new SendViewHolder(itemView);
        } else {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item_receive, viewGroup, false);
            return new ReceiveViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Message message = mMessageList.get(position);
        String contents = message.getContents();
        String timeCreated = formatTimeCreated(message.getTimeCreated());

        // Bind data to corresponding ViewHolder views
        if (viewHolder.getItemViewType() == VIEW_TYPE_SEND) {
            ((SendViewHolder) viewHolder).contents.setText(contents);
            ((SendViewHolder) viewHolder).timeCreated.setText(timeCreated);
            ((SendViewHolder) viewHolder).timeCreated.setVisibility(View.GONE);
        } else if (viewHolder.getItemViewType() == VIEW_TYPE_RECEIVE) {
            ((ReceiveViewHolder) viewHolder).contents.setText(contents);
            ((ReceiveViewHolder) viewHolder).timeCreated.setText(timeCreated);
            ((ReceiveViewHolder) viewHolder).timeCreated.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Return formatted String for time created text
    private String formatTimeCreated(long timeCreated) {
        Date date = new Date(timeCreated);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        DateFormat dateFormat;

        long currentTime = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());

        // If message is more than a week old, show time created in mm/dd/yy format
        if (currentTime - TimeUnit.MILLISECONDS.toDays(timeCreated) > 7) {
            dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        } else {
            // Else, show time created as day of week and time
            dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        }

        return dateFormat.format(date) + " at " + timeFormat.format(date);
    }

    // ViewHolder class for messages that were sent
    private static class SendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView contents;
        TextView timeCreated;

        SendViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            // Cache view references
            contents = itemView.findViewById(R.id.tv_contents_send);
            timeCreated = itemView.findViewById(R.id.tv_time_send);
        }

        @Override
        public void onClick(View view) {
            // When message clicked toggle visibility of time created text
            if (timeCreated.getVisibility() == View.VISIBLE) {
                timeCreated.setVisibility(View.GONE);
            } else {
                timeCreated.setVisibility(View.VISIBLE);
            }
        }
    }

    // ViewHolder class for messages that were received
    private static class ReceiveViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView contents;
        TextView timeCreated;

        ReceiveViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            // Cache view references
            contents = itemView.findViewById(R.id.tv_contents_receive);
            timeCreated = itemView.findViewById(R.id.tv_time_receive);
        }

        @Override
        public void onClick(View view) {
            // When message clicked toggle visibility of time created text
            if (timeCreated.getVisibility() == View.VISIBLE) {
                timeCreated.setVisibility(View.GONE);
            } else {
                timeCreated.setVisibility(View.VISIBLE);
            }
        }
    }
}
