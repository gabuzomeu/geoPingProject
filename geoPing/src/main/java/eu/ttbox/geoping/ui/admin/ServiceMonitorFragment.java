package eu.ttbox.geoping.ui.admin;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.domain.pairing.SpyServiceComponentName;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;

public class ServiceMonitorFragment extends Fragment {

    private static final String TAG = "ServiceMonitorFragment";


    private Button refreshButton;
    private ListView serviceMonitorListView;
    private ServiceMonitorListAdapter adapter;

    // ===========================================================
    // Constructor
    // ===========================================================

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.service_monitor, container, false);
        // Binding
        refreshButton = (Button) v.findViewById(R.id.service_monitor_refresh_button);
        serviceMonitorListView = (ListView) v.findViewById(R.id.service_monitor_list);
        // init
        ArrayList<ComponentName> serviceMonitor =  getComponentNameList(getActivity());
        adapter = new ServiceMonitorListAdapter( getActivity(),serviceMonitor);
        serviceMonitorListView.setAdapter(adapter);
        // Init Listener
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshServiceSattus();
            }
        });

        return v;
    }


    // ===========================================================
    // User Action
    // ===========================================================

    private void refreshServiceSattus() {
        adapter.notifyDataSetChanged();
    }


    // ===========================================================
    // Data Loader
    // ===========================================================


    private ArrayList<ComponentName> getComponentNameList(Context context) {
        ArrayList<ComponentName> list = new ArrayList<ComponentName>();
        // Resend
        list.add(ExtraFeatureHelper.getComponentNameReSentSmsMessageReceiver(context));
        // Geofence
        addAll(list,   ExtraFeatureHelper.getComponentNameGeofenceReceiver(context));
        // Spy
        addAll(list,   SpyServiceComponentName.getComponentSpyBootShutdownReceiver(context));
        addAll(list,   SpyServiceComponentName.getComponentSpyLowBatteryReceiver(context));
        addAll(list,   SpyServiceComponentName.getComponentSimChangeReceiver(context));
        addAll(list,   SpyServiceComponentName.getComponentPhoneCallReceiver(context));
        return  list;
    }


    private void addAll(ArrayList<ComponentName> list, ComponentName[] comps) {
        for (ComponentName comp :  comps) {
            list.add(comp);
        }
    }

}
