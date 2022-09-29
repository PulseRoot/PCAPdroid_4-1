/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.fragments.AppOverview;
import com.emanuelef.remote_capture.fragments.ConnectionsFragment;
import com.emanuelef.remote_capture.model.FilterDescriptor;
import com.google.android.material.tabs.TabLayoutMediator;

public class AppDetailsActivity extends BaseActivity {
    private static final String TAG = "AppDetailsActivity";
    public static final String APP_UID_EXTRA = "app_uid";
    private ViewPager2 mPager;
    private int mUid;

    private static final int POS_OVERVIEW = 0;
    private static final int POS_CONNECTIONS = 1;
    private static final int POS_TOTAL = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_details);
        displayBackAction();
        setContentView(R.layout.tabs_activity);

        mUid = getIntent().getIntExtra(APP_UID_EXTRA, Utils.UID_UNKNOWN);
        setupUidFilter();

        mPager = findViewById(R.id.pager);
        setupTabs();
    }

    private void setupUidFilter() {
        Intent intent = getIntent();
        if(intent == null) {
            intent = new Intent();
            setIntent(intent);
        }

        FilterDescriptor filter = new FilterDescriptor();
        filter.uid = mUid;
        intent.putExtra(ConnectionsFragment.FILTER_EXTRA, filter);
    }

    private class StateAdapter extends FragmentStateAdapter {
        StateAdapter(final FragmentActivity fa) { super(fa); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            //Log.d(TAG, "createFragment");
            switch (position) {
                case POS_CONNECTIONS:
                    // APP_UID_EXTRA is passed (see setupUidFilter)
                    return new ConnectionsFragment();
                case POS_OVERVIEW:
                default:
                    return AppOverview.newInstance(mUid);
            }
        }

        @Override
        public int getItemCount() {
            return POS_TOTAL;
        }

        public int getPageTitle(final int position) {
            switch(position) {
                case POS_CONNECTIONS:
                    return R.string.connections_view;
                case POS_OVERVIEW:
                default:
                    return R.string.overview;
            }
        }
    }

    private void setupTabs() {
        StateAdapter pageAdapter = new StateAdapter(this);
        mPager.setAdapter(pageAdapter);

        new TabLayoutMediator(findViewById(R.id.tablayout), mPager, (tab, position) ->
                tab.setText(getString(pageAdapter.getPageTitle(position)))
        ).attach();
    }
}
