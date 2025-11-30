package com.bytedance.tiktok_contribute;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

public class LocationUtil {
    private Context context;
    private LocationManager locationManager;
    private OnLocationGetListener listener;

    // 定位结果回调
    public interface OnLocationGetListener {
        void onSuccess(double latitude, double longitude); // 获取成功（纬度、经度）
        void onFailed(String errorMsg); // 获取失败
    }

    public LocationUtil(Context context, OnLocationGetListener listener) {
        this.context = context;
        this.listener = listener;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // 开始获取定位
    public void getCurrentLocation() {
        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            listener.onFailed("未获取定位权限");
            return;
        }

        // 检查定位服务是否开启
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            listener.onFailed("定位服务未开启");
            return;
        }

        //用GPS定位
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    listener.onSuccess(location.getLatitude(), location.getLongitude());
                    locationManager.removeUpdates(this); // 只获取一次
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            }, null);
        }
        // GPS不可用则用网络定位
        else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    listener.onSuccess(location.getLatitude(), location.getLongitude());
                    locationManager.removeUpdates(this);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            }, null);
        }
    }
}