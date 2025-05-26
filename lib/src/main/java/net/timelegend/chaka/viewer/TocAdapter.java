package net.timelegend.chaka.viewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TocAdapter extends RecyclerView.Adapter<TocAdapter.ViewHolder> {
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView titleView;
        public TextView openView;
        public TextView pageView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleView = (TextView)itemView.findViewById(R.id.oltitle);
            openView = (TextView)itemView.findViewById(R.id.olopen);

            openView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAbsoluteAdapterPosition();

                    if (position != RecyclerView.NO_POSITION) {
                        TocItem item = outline2.get(position);

                        if (item.count > 0) {
                            changeOutline(item, position, outline2);
                        }
                        else {
                            Intent data = new Intent();
                            data.putExtra("pagetogo", Activity.RESULT_FIRST_USER + item.page);
                            getActivity().setResult(Activity.RESULT_OK, data);
		                    getActivity().finish();
                        }
                    }
                }
            });
            pageView = (TextView)itemView.findViewById(R.id.olpage);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAbsoluteAdapterPosition();

            if (position != RecyclerView.NO_POSITION) {
                TocItem item = outline2.get(position);
                Intent data = new Intent();
                data.putExtra("pagetogo", Activity.RESULT_FIRST_USER + item.page);
                getActivity().setResult(Activity.RESULT_OK, data);
		        getActivity().finish();
            }
        }

    }

    protected Activity activity;
    protected ArrayList<TocItem> outline2;

    public TocAdapter(Activity activity, Bundle bundle) {
        this.activity = activity;
        this.outline2 = getOutline(bundle);
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View outlineView = inflater.inflate(R.layout.outline_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(outlineView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TocItem item = outline2.get(position);
        holder.titleView.setText(item.title);
        holder.titleView.setPadding(80 * item.level, 0, 0, 0);

        if (item.count == 0)
            holder.openView.setText("");
        else if (item.open)
            holder.openView.setText("❮");
        else
            holder.openView.setText("❯");

        holder.pageView.setText(String.valueOf(item.page + 1));
    }

    @Override
    public int getItemCount() {
        return outline2.size();
    }

    protected ArrayList<TocItem> outline;
	protected int found = -1;

    public int getfound() {
        return found;
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    protected ArrayList<TocItem> getOutline(Bundle bundle) {
        ArrayList<TocItem> result = new ArrayList<>();

		if (bundle != null) {
			int currentPage = bundle.getInt("POSITION");

            // below android 13 (api33)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ArrayList<TocItem> ai = new ArrayList<>();
                outline = (ArrayList<TocItem>)bundle.getSerializable("OUTLINE", ai.getClass());
            }
            else {
                outline = (ArrayList<TocItem>)bundle.getSerializable("OUTLINE");
            }

            for (int i = 0; i < outline.size(); ++i) {
				TocItem item = outline.get(i);

                if (item.page >= currentPage) {
                    while (item.level > 0) {
                        TocItem item2 = outline.get(--i);

                        if (item2.level < item.level) {
                            item = item2;
                            item.open = true;
                        }
                    }
                    break;
                }
            }

            int j = -1;
            int opens = 0;

			for (int i = 0; i < outline.size(); ++i) {
				TocItem item = outline.get(i);

                if (item.level == 0 || opens > 0) {
                    if (opens > 0) opens--;
                    result.add(item);
                    j++;

				    if (found < 0 && item.page > currentPage)
					    found = j - 1;

                    if (item.open) {
                        opens += item.count;
                    }
                    else {
                        i = countClose(item, i);
                    }
                }
                else {
                    i = countClose(item, i);
                }
			}

			if (found < 0) found = j;
		}
        return result;
	}

    private int countClose(TocItem item, int i) {
        int kc = item.count;
        while (kc > 0) {
            kc--;
            i++;
            TocItem item2 = outline.get(i);
            kc += item2.count;
        }
        return i;
    }

    protected void changeOutline(TocItem item, int position, ArrayList<TocItem> outline2) {
        for (int i = 0; i < outline.size(); ++i) {
            TocItem i2 = outline.get(i);

            if (i2.equals(item)) {
                int count = item.count;
                item.open = !item.open;

                while (count-- > 0) {
                    notifyItemChanged(position);

                    if (item.open) {
                        TocItem i3 = outline.get(++i);
                        outline2.add(++position, i3);
                        notifyItemInserted(position);

                        if (i3.open) {
                            count += i3.count;
                        }
                        else {
                            i = countClose(i3, i);
                        }
                    }
                    else {
                        TocItem i3 = outline2.get(position + 1);
                        outline2.remove(position + 1);
                        notifyItemRemoved(position + 1);

                        if (i3.open) {
                            count += i3.count;
                        }
                    }
                }
                break;
            }
        }
    }

}
