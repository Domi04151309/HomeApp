package io.github.domi04151309.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class ListAdapter extends BaseAdapter {

    private final String[] title;
    private final String[] summary;
    private static LayoutInflater inflater = null;

    public ListAdapter(Context context, String[] title, String[] summary) {
        this.title = title;
        this.summary = summary;
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
        TextView text = vi.findViewById(R.id.title);
        text.setText(title[position]);
        TextView texts = vi.findViewById(R.id.summary);
        texts.setText(summary[position]);
        return vi;
    }
}