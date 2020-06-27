package com.xiaojiang.sbu.justwifi;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class DeviceList extends ArrayAdapter<UpdateDevice> {

    private TextView t2, deviceId;
    private ImageView t1;
    private Activity context;
    private List<UpdateDevice> deviceLists;
    Double lat;
    Double longit;
    public static String node;


    public DeviceList(Activity context, List<UpdateDevice> userlist){
        super(context, R.layout.rowoflistview, userlist);
        this.context = context;
        this.deviceLists = userlist;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();

        View listviewwItem = inflater.inflate(R.layout.rowoflistview, null, true);

        t1 = (ImageView) listviewwItem.findViewById(R.id.t1);
        t2 = (TextView) listviewwItem.findViewById(R.id.t2);
        deviceId = (TextView) listviewwItem.findViewById(R.id.driveId);


        UpdateDevice device = deviceLists.get(position);
        String name = device.getDeviceName();

        String deviceId = device.getDeviceId();
        lat = device.getLat();
        longit = device.getLongit();

        if(name.contains("HTC")){
            t1.setImageResource(R.drawable.htc);
        }else if(name.contains("Galaxy")){
            t1.setImageResource(R.drawable.sum);
        }else if(name.contains("Emulator")){
            t1.setImageResource(R.drawable.pxiel);
        }else{
            t1.setImageResource(R.drawable.all);
        }

        t2.setText("My " + name);


        return listviewwItem;
    }
}
