package io.github.domi04151309.home;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class ListAdapter extends BaseAdapter {

    private final String[] title;
    private final String[] summary;
    private final String[] hidden;
    private final int[] drawable;
    private static LayoutInflater inflater = null;

    public ListAdapter(Context context, String[] title, String[] summary, String[] hidden) {
        this.title = title;
        this.summary = summary;
        this.hidden = hidden;
        this.drawable = null;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ListAdapter(Context context, String[] title, String[] summary, String[] hidden, int[] drawable) {
        this.title = title;
        this.summary = summary;
        this.hidden = hidden;
        this.drawable = drawable;
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
        playAnimation(vi);
        ImageView drawableView = vi.findViewById(R.id.drawable);
        TextView titleTxt = vi.findViewById(R.id.title);
        TextView summaryTxt = vi.findViewById(R.id.summary);
        TextView hiddenTxt = vi.findViewById(R.id.hidden);
        titleTxt.setText(title[position]);
        if (drawable == null) {
            drawableView.setVisibility(View.GONE);
        }
        try {
            assert drawable != null;
            drawableView.setImageDrawable(drawableView.getResources().getDrawable(drawable[position], null));
        } catch (Exception e){
            drawableView.setImageDrawable(drawableView.getResources().getDrawable(android.R.color.transparent, null));
        }
        try {
            summaryTxt.setText(summary[position]);
        } catch (Exception e){
            Log.w(General.LOG_TAG, String.valueOf(e.getClass()));
        }
        try {
            hiddenTxt.setText(hidden[position]);
        } catch (Exception e){
            Log.w(General.LOG_TAG, String.valueOf(e.getClass()));
        }
        return vi;
    }

    private void playAnimation(View v){
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(300);
        set.addAnimation(animation);

        v.startAnimation(set);
    }
}