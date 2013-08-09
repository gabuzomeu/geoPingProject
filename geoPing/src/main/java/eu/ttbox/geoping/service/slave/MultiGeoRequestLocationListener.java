package eu.ttbox.geoping.service.slave;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.util.concurrent.ConcurrentLinkedQueue;

import eu.ttbox.geoping.service.slave.GeoPingSlaveLocationService.GeoPingRequest;
import eu.ttbox.osm.ui.map.mylocation.sensor.v2.OsmAndLocationProvider;
import eu.ttbox.osm.ui.map.mylocation.sensor.v2.OsmLocation;

public class MultiGeoRequestLocationListener implements LocationListener, OsmAndLocationProvider.OsmAndLocationListener {

	private ConcurrentLinkedQueue<GeoPingRequest> geoPingRequestList;

	public MultiGeoRequestLocationListener() {
		super();
		this.geoPingRequestList = new ConcurrentLinkedQueue<GeoPingRequest>();
	}




    @Override
    public void onLocationChanged(OsmLocation location) {
        if (!geoPingRequestList.isEmpty()) {
            for (GeoPingRequest request : geoPingRequestList) {
                Location loc = location!=null?location.getLocation(): null;
                request.onLocationChanged(loc);
            }
        }
    }

	@Override
	public void onLocationChanged(Location location) {
		if (!geoPingRequestList.isEmpty()) {
			for (GeoPingRequest request : geoPingRequestList) {
				request.onLocationChanged(location);
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (!geoPingRequestList.isEmpty()) {
			for (GeoPingRequest request : geoPingRequestList) {
				request.onStatusChanged(provider, status, extras);
			}
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		if (!geoPingRequestList.isEmpty()) {
			for (GeoPingRequest request : geoPingRequestList) {
				request.onProviderEnabled(provider);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (!geoPingRequestList.isEmpty()) {
			for (GeoPingRequest request : geoPingRequestList) {
				request.onProviderDisabled(provider);
			}
		}
	}

	public boolean isEmpty() {
		return geoPingRequestList.isEmpty();
	}

	public boolean remove(GeoPingRequest request) {
		return geoPingRequestList.remove(request);
	}

	public boolean add(GeoPingRequest request) {
		return geoPingRequestList.add(request);
	}

	public void clear() {
		geoPingRequestList.clear();
	}

}
