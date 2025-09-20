package net.timelegend.chaka.viewer;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ColorAdapter extends BaseAdapter {
    public Context mContext;
    public List<ColorItem> mDataSource;
    public OnItemClickListener itemClickListener;

    public ColorAdapter(Context context, List<ColorItem> dataSource, OnItemClickListener itemClickListener) {
        this.mContext = context;
        this.mDataSource = dataSource;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public int getCount() {
        return mDataSource.size();
    }

    @Override
    public ColorItem getItem(int position) {
        return mDataSource.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.color_row, parent, false);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.colortitle);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        ColorItem item = getItem(position);

        if (item != null) {
            holder.tvTitle.setText(item.title);
            holder.tvTitle.setTextColor(item.black);
            holder.tvTitle.setBackgroundColor(item.white);

            holder.tvTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onItemClick(position);
                }
            });
        }

        return convertView;
    }

    public int getWidth() {
        int maxWidth = 0;
        View itemView = null;
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        for (int i = 0; i < getCount(); i++) {
            itemView = getView(i, itemView, null);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            int itemWidth = itemView.getMeasuredWidth();
            maxWidth = Math.max(maxWidth, itemWidth);
        }

        return maxWidth;
    }

    public class ViewHolder {
        public TextView tvTitle;
    }

    public interface OnItemClickListener {
        public void onItemClick(int position);
    }
}
