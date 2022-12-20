package com.example.examplebluetooth;

import android.bluetooth.BluetoothDevice;
import android.location.GnssAntennaInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private static final String TAG = "DeviceListAdapter";
    public ArrayList<BluetoothDevice> devices = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item, parent, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try{
            String name = devices.get(position).getName();
            if(name == null){
                name = devices.get(position).getAddress();
            }
            holder.textView.setText(name);
            holder.mListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mAdapterItemClickListener !=null){
                        mAdapterItemClickListener.onItemClicked(devices.get(position));
                    }
                }
            };
        }catch(SecurityException se){
            Log.d(TAG, "onBindViewHolder: Cannot bind view due to permission errors");
        }
    }

    public interface IAdapterItemClick {
        void onItemClicked(BluetoothDevice selectedItem);
    }
    IAdapterItemClick mAdapterItemClickListener;
    public void setOnAdapterItemClick(
            IAdapterItemClick adapterItemClickHandler) {
        mAdapterItemClickListener = adapterItemClickHandler;
    }
    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView textView;
        View.OnClickListener mListener;
        public ViewHolder(View view){
            super(view);
            textView = view.findViewById(R.id.textView);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(mListener!=null){
                mListener.onClick(view);
            }
        }
    }
}
