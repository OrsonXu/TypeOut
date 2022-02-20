package com.aware.smartphoneuse;

import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


public class WhitelistGridAdapter extends RecyclerView.Adapter<WhitelistGridAdapter.ViewHolder> {
//    private List<String> mData;
//    private List<Integer> mDataStatus; // 1 is whitelisted, 0 is not
    private List<WhitelistActivity.AppInfo> mDataInfo;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private boolean currentPhaseSaveAllowed = false;

    // data is passed into the constructor
//    WhitelistGridAdapter(Context context, List<String> data, List<Integer> dataStatus) {
//        this.mInflater = LayoutInflater.from(context);
//        this.mData = data;
//        this.mDataStatus = dataStatus;
//    }
    WhitelistGridAdapter(Context context, List<WhitelistActivity.AppInfo> datainfo, boolean phaseSaveAllowed) {
        this.mInflater = LayoutInflater.from(context);
        this.mDataInfo = datainfo;
        this.currentPhaseSaveAllowed = phaseSaveAllowed;
    }

    // inflates the cell layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(view);
    }
    // binds the data to the TextView in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //holder.myTextView.setText(mData.get(position));
        holder.myImageView.setImageDrawable(mDataInfo.get(position).getIcon());
        String appText = mDataInfo.get(position).getName() + "\n" + mDataInfo.get(position).getPkg();
        SpannableString spannableString = new SpannableString(appText);
        int startIdx = 0;
        int endIdx = mDataInfo.get(position).getName().length();
        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD)
                , startIdx, endIdx,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(1.5f)
                , startIdx, endIdx,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.myAppView.setText(spannableString);
        if (mDataInfo.get(position).getStat()){
            holder.myTextView.setBackgroundResource(R.color.black);
        }
        else {
            holder.myTextView.setBackgroundResource(R.color.colorNoSel);
        }

        if(mDataInfo.get(position).getUnChangeable()){
            holder.myTextView.setBackgroundResource(R.color.primary);
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        //return mData.size();
        return mDataInfo.size();
    }
    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        TextView myAppView;
        ImageView myImageView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.info_text);
            myAppView = itemView.findViewById(R.id.info_app);
            myImageView = itemView.findViewById(R.id.info_image);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                int pos = getAdapterPosition();
                //int status = mDataStatus.get(pos);
                Boolean status = mDataInfo.get(pos).getStat();
                Boolean unChangeable = mDataInfo.get(pos).getUnChangeable();
                Boolean newstatus;
                if (currentPhaseSaveAllowed && !unChangeable) {
                    if (!status) { // blacklist --> whitelist
                        newstatus = true;
                        myTextView.setBackgroundResource(R.color.black);
                    } else { // whitelist --> blacklist
                        newstatus = false;
                        myTextView.setBackgroundResource(R.color.colorNoSel);
                    }
                }
                else {
                    newstatus = status;
                }
                //mDataStatus.set(pos, newstatus);
                mDataInfo.set(pos, new WhitelistActivity.AppInfo(mDataInfo.get(pos), newstatus));
                mClickListener.onItemClick(view, pos, newstatus, unChangeable);
            }
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position, Boolean newstatus, Boolean unChangeable);
    }
}
