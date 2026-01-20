package net.timelegend.chaka.viewer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView killView;
        public TextView titleView;
        public TextView editView;
        public TextView pageView;

        public ViewHolder(View itemView) {
            super(itemView);
            killView = (TextView)itemView.findViewById(R.id.bmkill);
            titleView = (TextView)itemView.findViewById(R.id.bmtitle);
            editView = (TextView)itemView.findViewById(R.id.bmedit);
            pageView = (TextView)itemView.findViewById(R.id.bmpage);
            itemView.setOnClickListener(this);

            killView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAbsoluteAdapterPosition();

                    if (position != RecyclerView.NO_POSITION) {
                        BookmarkItem item = bookmarks.remove(position);
                        notifyItemRemoved(position);
                        BookmarkRepository.getInstance().remove(item);
                    }
                }
            });

            editView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAbsoluteAdapterPosition();

                    if (position != RecyclerView.NO_POSITION) {
                        editBookmark(position);
                    }
                }
            });
        }

        @Override
        public void onClick(View view) {
            int position = getAbsoluteAdapterPosition();

            if (position != RecyclerView.NO_POSITION) {
                BookmarkItem item = bookmarks.get(position);
                Intent data = new Intent();
                data.putExtra(Tool.PAGETOGO, Activity.RESULT_FIRST_USER + item.page);
                getActivity().setResult(Activity.RESULT_OK, data);
		        getActivity().finish();
            }
        }
    }

    public void editBookmark(int position) {
        BookmarkItem item = bookmarks.get(position);
        LayoutInflater inflater = LayoutInflater.from(activity);
        View editBookmarkLayout = inflater.inflate(R.layout.edit_bookmark, null);
        EditText input = editBookmarkLayout.findViewById(R.id.editText);
        input.setText(item.title);

        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                }
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.MyDialog2)
            .setTitle(Tool.getResourceString(R.string.edit_bookmark))
            .setMessage(Tool.getResourceString(R.string.edit_bookmark_message))
            .setView(editBookmarkLayout)
            .setPositiveButton(Tool.getResourceString(R.string.okay), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newTitle = input.getText().toString().trim();

                    if (!"".equals(newTitle) && !item.title.equals(newTitle)) {
                        item.title = newTitle;
                        notifyItemChanged(position);
                        BookmarkRepository.getInstance().onEdit(item);
                    }
                }
            })
            .setNegativeButton(Tool.getResourceString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).create();

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
        input.setSelection(input.getText().length());
    }

    protected Activity activity;
    protected ArrayList<BookmarkItem> bookmarks;

    public BookmarkAdapter(Activity activity) {
        this.activity = activity;
        Map<Integer, BookmarkItem> bms = BookmarkRepository.getInstance().load();

        if (bms != null && bms.size() > 0) {
            bookmarks = new ArrayList<>(bms.values());
            Collections.sort(bookmarks);
        }
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View bookmarkView = inflater.inflate(R.layout.bookmark_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(bookmarkView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BookmarkItem item = bookmarks.get(position);
        holder.killView.setText("✕");
        holder.titleView.setText(item.title);
        holder.editView.setText("✍");
        holder.pageView.setText(String.valueOf(item.page + 1));
    }

    @Override
    public int getItemCount() {
        return (bookmarks == null) ? 0 : bookmarks.size();
    }

}
