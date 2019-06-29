package io.github.domi04151309.home.hue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import io.github.domi04151309.home.R;

class HueScenesGridAdapter extends BaseAdapter {

    private final int[] drawable;
    private final String[] name;
    private final String[] hidden;
    private static LayoutInflater inflater = null;

    public HueScenesGridAdapter(Context context, int[] drawable, String[] name, String[] hidden) {
        this.drawable = drawable;
        this.name = name;
        this.hidden = hidden;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return name.length;
    }

    @Override
    public Object getItem(int position) {
        return name[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.grid_item, parent, false);
        ImageView drawableView = vi.findViewById(R.id.drawable);
        TextView nameTxt = vi.findViewById(R.id.name);
        TextView hiddenTxt = vi.findViewById(R.id.hidden);
        drawableView.setImageResource(drawable[position]);
        nameTxt.setText(name[position]);
        hiddenTxt.setText(hidden[position]);
        return vi;
    }
}