package com.example.geofencing;

import com.google.android.gms.maps.model.LatLng;

import java.security.acl.LastOwnerException;
import java.util.List;

public interface IOnLoadLocationListener {
    void onLoadLocationSuccess(List<MylatLng>latLngs);
    void onLoadLocationFailed(String message);
}
