package com.example.plugindemo.communicationdma.filetransfer;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.plugindemo.R;

public class FileTransferListAdapter extends BaseAdapter {
    
    private Context mContext;
    
    private List<SdCardFileInfo> mList;
    
    FileTransferListAdapter(Context context, List<SdCardFileInfo> fileList) {
        mContext = context;
        mList = fileList;
    }
    
    @Override
    public int getCount() {
        return mList.size();
    }
    
    protected void updateListViewItem(long id, int state, int progress, int errorCode) {
        int size = mList.size();
        for (int i = 0; i < size; i++) {
            SdCardFileInfo info = mList.get(i);
            if (info.getId() == id) {
                info.setStatus(state);
                info.setProgress(progress);
                info.setErrorCode(errorCode);
                break;
            }
        }
        notifyDataSetChanged();
    }
    
    @Override
    public SdCardFileInfo getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.local_app_list_item, parent, false);
            holder = new ViewHolder();
            
            holder.fileIcon = (ImageView) convertView.findViewById(R.id.file_icon);
            holder.fileName = (TextView) convertView.findViewById(R.id.file_name_tv);
            holder.filePath = (TextView) convertView.findViewById(R.id.file_path_tv);
            holder.transferProgress = (ProgressBar) convertView.findViewById(R.id.transfer_progress);
            holder.sendButton = (Button) convertView.findViewById(R.id.item_bt);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        updateItemStatus(holder, getItem(position));
        return convertView;
    }
    
    private void updateItemStatus(ViewHolder holder, SdCardFileInfo info){
        boolean isTransferring = false;
        int status = info.getStatus();
        int progress = (int) info.getProgress();

        switch (status) {
            case FileTransferDemoActivity.STATUS_IDLE:
                holder.filePath.setVisibility(View.VISIBLE);
                holder.transferProgress.setVisibility(View.INVISIBLE);
                holder.sendButton.setText("Send");
                holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_purple));
                isTransferring = false;
                break;
            case FileTransferDemoActivity.STATUS_QUEUING:
                holder.filePath.setVisibility(View.VISIBLE);
                holder.transferProgress.setVisibility(View.INVISIBLE);
                holder.sendButton.setText("Queuing...");
                holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_green_light));
                isTransferring = false;
                break;
            case FileTransferDemoActivity.STATUS_TRANSFERING:
                if (!isTransferring) {
                    holder.filePath.setVisibility(View.INVISIBLE);
                    holder.transferProgress.setVisibility(View.VISIBLE);
                    holder.sendButton.setText("Transferring...");
                    holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_orange_light));
                }
                holder.transferProgress.setProgress(progress);
                isTransferring = true;
                break;
            case FileTransferDemoActivity.STATUS_CANCEL:
                holder.filePath.setVisibility(View.VISIBLE);
                holder.transferProgress.setVisibility(View.INVISIBLE);
                holder.sendButton.setText("Pause");
                holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_red_light));
                isTransferring = false;
                break;
            case FileTransferDemoActivity.STATUS_ERROR:
                holder.filePath.setVisibility(View.VISIBLE);
                holder.transferProgress.setVisibility(View.INVISIBLE);
                holder.sendButton.setText("Error:" + info.getErrorCode());
                holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_red_light));
                isTransferring = false;
                break;
            case FileTransferDemoActivity.STATUS_COMPLETE:
                holder.filePath.setVisibility(View.VISIBLE);
                holder.transferProgress.setVisibility(View.INVISIBLE);
                holder.sendButton.setText("OK");
                holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_purple));
                isTransferring = false;
                break;
            case FileTransferDemoActivity.STATUS_PATH_INVALID:
                holder.filePath.setVisibility(View.VISIBLE);
                holder.transferProgress.setVisibility(View.INVISIBLE);
                holder.sendButton.setText("PATH_INVALID");
                holder.sendButton.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_red_light));
                isTransferring = false;
                break;
            default:
                break;
        }
        
        if (!isTransferring) {
            holder.fileIcon.setImageResource(R.drawable.ic_launcher);
            holder.fileName.setText(info.getFile().getName());
            holder.filePath.setText(info.getFile().getPath());
        }
    }
    
    private static class ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView filePath;
        Button sendButton;
        ProgressBar transferProgress;
    }
}




