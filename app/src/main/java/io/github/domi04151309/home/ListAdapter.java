package io.github.domi04151309.home;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class ListAdapter extends BaseAdapter {

    private final String[] title;
    private final String[] summary;
    private final String[] hidden;
    private static LayoutInflater inflater = null;

    public ListAdapter(Context context, String[] title, String[] summary) {
        this.title = title;
        this.summary = summary;
        this.hidden = null;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ListAdapter(Context context, String[] title, String[] summary, String[] hidden) {
        this.title = title;
        this.summary = summary;
        this.hidden = hidden;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return title.length;
    }

    @Override
    public Object getItem(int position) {
        return title[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.list_item, null);
        TextView titleTxt = vi.findViewById(R.id.title);
        TextView summaryTxt = vi.findViewById(R.id.summary);
        TextView hiddenTxt = vi.findViewById(R.id.hidden);
        titleTxt.setText(title[position]);
        try {
            summaryTxt.setText(summary[position]);
        } catch (Exception e){
            Log.w("Home", String.valueOf(e.getClass()));
        }
        try {
            hiddenTxt.setText(hidden[position]);
        } catch (Exception e){
            Log.w("Home", String.valueOf(e.getClass()));
        }
        return vi;
    }
}