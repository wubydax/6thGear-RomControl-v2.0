package com.wubydax.romcontrol.v2.utils;
/*
Created by Roberto Mariani & Anna Berkovitch on 27-Jul-17.

This file is part of 6thGear-RomControl-v2.0

        6thGear-RomControl-v2.0 is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        6thGear-RomControl-v2.0 is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with 6thGear-RomControl-v2.0.  If not, see <http://www.gnu.org/licenses/>.
*/


import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.wubydax.romcontrol.v2.R;

import java.util.ArrayList;

public class MultiSelectAdapter extends RecyclerView.Adapter<MultiSelectAdapter.MultiSelectViewHolder> {
    private ArrayList<SelectionItem> mItemsList;
    private OnItemSelectedListener mOnItemSelectedListener;

    public MultiSelectAdapter(ArrayList<SelectionItem> list, OnItemSelectedListener listener) {

        mItemsList = list;
        mOnItemSelectedListener = listener;

    }

    public String getSelectedItems() {
        StringBuilder stringBuilder = new StringBuilder();
        for (SelectionItem selectionItem : mItemsList) {
            if (selectionItem.isSelected) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append(selectionItem.value);
            }
        }
        return stringBuilder.toString();
    }

    public void setSelectedItems(String selectedItems) {
        if (!TextUtils.isEmpty(selectedItems)) {
            String[] items = selectedItems.split(",");
            if (items.length > 0) {
                for (String item : items) {
                    for (SelectionItem selectionItem : mItemsList) {
                        if (selectionItem.value.equals(item)) {
                            selectionItem.isSelected = true;
                            mOnItemSelectedListener.onItemSelected(true);
                        }
                    }
                }
                notifyDataSetChanged();
            }
        }
    }


    public void selectAll(boolean select) {
        for (SelectionItem selectionItem : mItemsList) {
            selectionItem.isSelected = select;
        }
        notifyDataSetChanged();
    }


    @Override
    public MultiSelectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MultiSelectViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.multi_select_preference_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(MultiSelectViewHolder holder, int position) {
        holder.bindViews(mItemsList.get(position));
    }

    @Override
    public int getItemCount() {
        return mItemsList != null ? mItemsList.size() : 0;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(boolean isSelected);
    }

    class MultiSelectViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textView;

        MultiSelectViewHolder(View itemView) {
            super(itemView);
            checkBox = (CheckBox) itemView.findViewById(R.id.multi_select_checkbox);
            textView = (TextView) itemView.findViewById(R.id.multi_select_entry_text);

        }

        void bindViews(SelectionItem selectionItem) {
            textView.setText(selectionItem.entry);
            checkBox.setChecked(selectionItem.isSelected);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkBox.setChecked(!checkBox.isChecked());
                    mItemsList.get(getAdapterPosition()).isSelected = checkBox.isChecked();
                    mOnItemSelectedListener.onItemSelected(checkBox.isChecked());
                }
            });
        }
    }


}
