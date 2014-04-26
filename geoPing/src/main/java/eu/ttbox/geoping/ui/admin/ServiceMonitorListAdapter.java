package eu.ttbox.geoping.ui.admin;


import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import eu.ttbox.geoping.R;

public class ServiceMonitorListAdapter extends ArrayAdapter<ComponentName> {


    private static final String TAG = "ServiceMonitorListAdapter";

    private Context mContext;
    private LayoutInflater mInflater;

    // Service
    private PackageManager pm;


    // Instance
    private int mLayout = R.layout.service_monitor_list_item;


    // ===========================================================
    // Constructor
    // ===========================================================

    public ServiceMonitorListAdapter(Context context, List<ComponentName> objects) {
        super(context, R.layout.service_monitor_list_item, objects);
        // Init
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Service
        pm = context.getPackageManager();

    }


    // ===========================================================
    // Binding
    // ===========================================================

    public View getView(int position, View convertView, ViewGroup parent) {
        ComponentName item = getItem(position);
        View v;
        if (convertView == null) {
            v = newView(mContext, item, parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, item);
        return v;
    }


    public void bindView(View view, final Context context, ComponentName item) {
        ViewHolder holder = (ViewHolder) view.getTag();
        // Bind Name
        String name = item.getClassName();

        // Description
        holder.nameText.setText(name);
        try {
            ServiceInfo serviceInfo = pm.getServiceInfo(item, PackageManager.GET_META_DATA);
            holder.descriptionText.setText(serviceInfo.toString());
        } catch (PackageManager.NameNotFoundException e) {
            holder.descriptionText.setText("Error : " + e.getMessage());
        }

        // Bind Status
        int setting = pm.getComponentEnabledSetting(item);
        switch (setting) {
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
                holder.descriptionText.setText("STATE_DEFAULT");
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                holder.descriptionText.setText("ENABLED");
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                holder.descriptionText.setText("DISABLED_USER");
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                holder.descriptionText.setText("DISABLED");
                break;
            default:
                holder.descriptionText.setText("ENABLED_STATE : " + setting);
                Log.w(TAG, "Not manage component setting : " + setting);
                break;
        }
    }


    public View newView(Context context, ComponentName item, ViewGroup parent) {
        View view = mInflater.inflate(mLayout, parent, false);
        // Then populate the ViewHolder
        ViewHolder holder = new ViewHolder();
        holder.icon = (ImageView) view.findViewById(R.id.service_monitor_list_item_icon);
        holder.nameText = (TextView) view.findViewById(R.id.service_monitor_list_item_name);
        holder.descriptionText = (TextView) view.findViewById(R.id.service_monitor_list_item_description);
        // and store it inside the layout.
        view.setTag(holder);
        return view;

    }

    static class ViewHolder {
        ImageView icon;
        TextView nameText;
        TextView descriptionText;
    }


}
