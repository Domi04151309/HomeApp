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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

class ListAdapter extends BaseAdapter {

    private final String[] title;
    private final String[] summary;
    private final String[] hidden;
    private final int[] drawable;
    private final boolean[] state;
    private final CompoundButton.OnCheckedChangeListener stateListener;
    private static LayoutInflater inflater = null;

    public ListAdapter(Context context, String[] title, String[] summary, String[] hidden) {
        this.title = title;
        this.summary = summary;
        this.hidden = hidden;
        this.drawable = null;
        this.state = null;
        this.stateListener = null;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ListAdapter(Context context, String[] title, String[] summary, int[] drawable) {
        this.title = title;
        this.summary = summary;
        this.hidden = null;
        this.drawable = drawable;
        this.state = null;
        this.stateListener = null;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ListAdapter(Context context, String[] title, String[] summary, String[] hidden, int[] drawable) {
        this.title = title;
        this.summary = summary;
        this.hidden = hidden;
        this.drawable = drawable;
        this.state = null;
        this.stateListener = null;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ListAdapter(Context context, String[] title, String[] summary, String[] hidden, int[] drawable, boolean[] state, CompoundButton.OnCheckedChangeListener stateListener) {
        this.title = title;
        this.summary = summary;
        this.hidden = hidden;
        this.drawable = drawable;
        this.state = state;
        this.stateListener = stateListener;
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
            vi = inflater.inflate(R.layout.list_item, parent, false);
        playAnimation(vi);
        ImageView drawableView = vi.findViewById(R.id.drawable);
        TextView titleTxt = vi.findViewById(R.id.title);
        TextView summaryTxt = vi.findViewById(R.id.summary);
        TextView hiddenTxt = vi.findViewById(R.id.hidden);
        Switch stateSwitch = vi.findViewById(R.id.state);
        titleTxt.setText(title[position]);
        if (drawable == null) {
            drawableView.setVisibility(View.GONE);
        } else {
            try {
                drawableView.setImageResource(drawable[position]);
            } catch (Exception e){
                drawableView.setImageResource(android.R.color.transparent);
            }
        }
        try {
            summaryTxt.setText(summary[position]);
        } catch (Exception e){
            Log.w(Global.LOG_TAG, String.valueOf(e.getClass()));
        }
        try {
            hiddenTxt.setText(hidden[position]);
        } catch (Exception e){
            hiddenTxt.setText("");
        }
        if (state == null) {
            stateSwitch.setVisibility(View.GONE);
        } else {
            stateSwitch.setChecked(state[position]);
            stateSwitch.setOnCheckedChangeListener(stateListener);
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