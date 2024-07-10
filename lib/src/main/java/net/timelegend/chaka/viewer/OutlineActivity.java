package net.timelegend.chaka.viewer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;

public class OutlineActivity extends Activity
{
	public static class Item implements Serializable {
		public String title;
		public int page;
        public int level;
        public int count;
        public boolean open;

		public Item(String title, int page, int level, int count) {
			this.title = title;
			this.page = page;
            this.level = level;
            this.count = count;
            this.open = false;
		}

		public String toString() {
			return title + " " + (page + 1);
		}

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Item) {
                Item item = (Item)obj;
                return item.title.equals(title) && item.page == page && item.level == level && item.count == count;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return title.hashCode() + page + level + count;
        }
	}

    public class OutlineAdapter extends RecyclerView.Adapter<OutlineAdapter.ViewHolder> {
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
                            Item item = outline2.get(position);

                            if (item.count > 0) {
                                changeOutline(item, position, outline2, getAdapter());
                            }
                            else {
		                        setResult(RESULT_FIRST_USER + item.page);
		                        finish();
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
                    Item item = outline2.get(position);
                    changeOutline(item, position, outline2, null);
		            setResult(RESULT_FIRST_USER + item.page);
		            finish();
                }
            }
        }

        protected OutlineAdapter getAdapter() {
            return this;
        }

        protected ArrayList<Item> outline2;

        public OutlineAdapter(ArrayList<Item> outline2) {
            this.outline2 = outline2;
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
            Item item = outline2.get(position);
            holder.titleView.setText(item.title);

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
    }

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.outline_activity);

        RecyclerView rvOutline = (RecyclerView)findViewById(R.id.rvOutline);
        OutlineAdapter adapter = new OutlineAdapter(getOutline());
        rvOutline.setAdapter(adapter);
        rvOutline.setLayoutManager(new LinearLayoutManager(this));

        if (found > 0)
            rvOutline.scrollToPosition(found);
    }

    protected ArrayList<Item> outline;
	protected int found = -1;

    protected ArrayList<Item> getOutline() {
        ArrayList<Item> result = new ArrayList<Item>();
		int idx = getIntent().getIntExtra("PALLETBUNDLE", -1);
		Bundle bundle = Pallet.receiveBundle(idx);

		if (bundle != null) {
			int currentPage = bundle.getInt("POSITION");
			outline = (ArrayList<Item>)bundle.getSerializable("OUTLINE");

            for (int i = 0; i < outline.size(); ++i) {
				Item item = outline.get(i);

                if (item.page >= currentPage) {
                    while (item.level > 0) {
                        Item item2 = outline.get(--i);

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
				Item item = outline.get(i);

                if (item.level == 0 || opens-- > 0) {
                    result.add(item);
                    j++;

				    if (found < 0 && item.page > currentPage)
					    found = j - 1;

                    if (item.open) {
                        opens += item.count;
                    }
                    else {
                        i += item.count;
                    }
                }
                else {
                    i += item.count;
                }
			}

			if (found < 0) found = j;
		}

        return result;
	}

    protected void changeOutline(Item item, int position, ArrayList<Item> outline2, OutlineAdapter adapter) {
        for (int i = 0; i < outline.size(); ++i) {
            Item i2 = outline.get(i);

            if (i2.equals(item)) {
                int count = item.count;
                item.open = !item.open;

                while (count-- > 0) {
                    if (adapter != null)
                        adapter.notifyItemChanged(position);

                    if (item.open) {
                        Item i3 = outline.get(++i);
                        outline2.add(++position, i3);

                        if (adapter != null)
                            adapter.notifyItemInserted(position);

                        if (i3.open) {
                            count += i3.count;
                        }
                        else {
                            i += i3.count;
                        }
                    }
                    else {
                        Item i3 = outline2.get(position + 1);
                        outline2.remove(position + 1);

                        if (adapter != null)
                            adapter.notifyItemRemoved(position + 1);
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