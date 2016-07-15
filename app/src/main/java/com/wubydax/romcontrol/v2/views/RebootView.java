package com.wubydax.romcontrol.v2.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.wubydax.romcontrol.v2.R;

/*      Created by Roberto Mariani and Anna Berkovitch, 2015-2016
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

public class RebootView extends FrameLayout {
    public RebootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RebootView);
        String text = typedArray.getString(R.styleable.RebootView_rebootText);
        int color = typedArray.getColor(R.styleable.RebootView_rebootColor, Color.WHITE);
        init(text, color);
        typedArray.recycle();
    }

    private void init(String text, int color) {
        inflate(getContext(), R.layout.reboot_item, this);
        ((TextView) findViewById(R.id.rebootText)).setText(text);
        findViewById(R.id.rebootFab).setBackgroundTintList(ColorStateList.valueOf(color));

    }


}
