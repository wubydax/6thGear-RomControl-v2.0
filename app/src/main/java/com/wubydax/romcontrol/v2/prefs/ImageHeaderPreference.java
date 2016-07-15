package com.wubydax.romcontrol.v2.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wubydax.romcontrol.v2.R;

/*      Created by Roberto Mariani and Anna Berkovitch, 10/07/2016
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

public class ImageHeaderPreference extends Preference {
    private int mResId;

    public ImageHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImageHeaderPreference);
        mResId = typedArray.getResourceId(R.styleable.ImageHeaderPreference_imageSource, -1);
        typedArray.recycle();
        setLayoutResource(R.layout.preference_header_general);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ((ImageView) view).setImageDrawable(getContext().getDrawable(mResId));
        return view;
    }
}
