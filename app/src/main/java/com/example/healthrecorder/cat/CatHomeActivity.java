package com.example.healthrecorder.cat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.healthrecorder.R;
import com.example.healthrecorder.teeth.TeethRecordActivity;
import com.example.healthrecorder.teeth.TeethRecordListActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * 猫咪主页：TabHost 切换「猫拉屎」和「猫刷牙」两个功能页
 */
public class CatHomeActivity extends FragmentActivity {

    private TabLayout tabLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;
    private int currentTab = 0; // 0=拉屎, 1=刷牙

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                // 刷新当前 Tab 对应的列表
                refreshCurrentTab();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_home);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // 配置 ViewPager 适配器
        viewPager.setAdapter(new CatHomePagerAdapter(this));
        viewPager.setUserInputEnabled(true); // 允许滑动切换

        // TabLayout 与 ViewPager 绑定
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("💩 猫拉屎");
            } else {
                tab.setText("🦷 猫刷牙");
            }
        }).attach();

        // Tab 切换监听（用于更新右侧添加按钮的文字）
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateAddButtonText();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 统计按钮：Tab0→CatStatsActivity，Tab1→TeethRecordListActivity（刷牙没有统计页，用列表代替）
        findViewById(R.id.btnStats).setOnClickListener(v -> {
            if (currentTab == 0) {
                startActivity(new Intent(this, CatStatsActivity.class));
            } else {
                startActivity(new Intent(this, com.example.healthrecorder.teeth.TeethRecordListActivity.class));
            }
        });

        // 添加按钮：Tab0→CatRecordActivity，Tab1→TeethRecordActivity
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            if (currentTab == 0) {
                addLauncher.launch(new Intent(this, CatRecordActivity.class));
            } else {
                addLauncher.launch(new Intent(this, TeethRecordActivity.class));
            }
        });

        updateAddButtonText();
    }

    private void updateAddButtonText() {
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnStats = findViewById(R.id.btnStats);
        if (currentTab == 0) {
            btnAdd.setText("💩 记一笔");
            btnStats.setText("📊 统计");
        } else {
            btnAdd.setText("🦷 记一笔");
            btnStats.setText("📋 列表");
        }
    }

    private void refreshCurrentTab() {
        // 通知当前 Fragment 刷新数据
        androidx.fragment.app.Fragment f = getSupportFragmentManager()
                .findFragmentByTag("f" + currentTab);
        if (f instanceof CatHomeListFragment) {
            ((CatHomeListFragment) f).refresh();
        }
    }

    /**
     * 刷新所有 Tab（从 CatRecordListActivity/TeethRecordListActivity 返回时调用）
     */
    public void refreshAll() {
        for (int i = 0; i < 2; i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag("f" + i);
            if (f instanceof CatHomeListFragment) {
                ((CatHomeListFragment) f).refresh();
            }
        }
    }

    // ─── ViewPager 适配器 ─────────────────────────────────────────────────

    private static class CatHomePagerAdapter extends FragmentStateAdapter {

        CatHomePagerAdapter(FragmentActivity fa) { super(fa); }

        @Override
        public Fragment createFragment(int position) {
            return CatHomeListFragment.newInstance(position);
        }

        @Override
        public int getItemCount() { return 2; }
    }

    // ─── 内嵌列表 Fragment ───────────────────────────────────────────────

    public static class CatHomeListFragment extends Fragment {
        private static final String ARG_TAB = "tab";
        private int tabIndex;
        private android.view.View rootView;
        private androidx.recyclerview.widget.RecyclerView recyclerView;
        private TextView tvEmpty;
        private android.widget.LinearLayout btnEditToggle;

        public static CatHomeListFragment newInstance(int tabIndex) {
            CatHomeListFragment f = new CatHomeListFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_TAB, tabIndex);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) tabIndex = getArguments().getInt(ARG_TAB, 0);
        }

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater,
                                              android.view.ViewGroup container,
                                              Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_cat_home_list, container, false);
            recyclerView = rootView.findViewById(R.id.recyclerView);
            tvEmpty = rootView.findViewById(R.id.tvEmpty);
            btnEditToggle = rootView.findViewById(R.id.btnEditToggle);

            recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
            // 设置 RecyclerView 底部留白，便于 FAB 滚动
            recyclerView.setClipToPadding(false);
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics())
            );

            // 编辑/删除切换按钮（复用已有布局逻辑）
            if (btnEditToggle != null) {
                btnEditToggle.setVisibility(View.GONE);
            }

            loadData();
            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            loadData();
        }

        public void refresh() {
            loadData();
        }

        private void loadData() {
            if (tabIndex == 0) {
                loadCatRecords();
            } else {
                loadTeethRecords();
            }
        }

        private void loadCatRecords() {
            java.util.List<CatRecord> recordList = new java.util.ArrayList<>();
            try {
                com.example.healthrecorder.HealthRepository repository =
                        com.example.healthrecorder.HealthRepository.getInstance(requireContext().getApplicationContext());
                android.database.sqlite.SQLiteDatabase db = repository.getReadableDatabase();
                android.database.Cursor c = db.rawQuery(
                        "SELECT c.*, u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_NAME + " AS un, "
                        + "u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_COLOR + " AS uc "
                        + "FROM cat_records c LEFT JOIN users u ON c.user_id=u."
                        + com.example.healthrecorder.DatabaseHelper.COLUMN_ID
                        + " ORDER BY c.date DESC", null);
                if (c.moveToFirst()) {
                    do {
                        CatRecord r = new CatRecord();
                        r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                        r.setDate(new java.util.Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                        r.setCatName(c.getString(c.getColumnIndexOrThrow("cat_name")));
                        r.setStoolType(c.getInt(c.getColumnIndexOrThrow("stool_type")));
                        int scIdx = c.getColumnIndex("stool_count");
                        r.setStoolCount(scIdx >= 0 ? c.getInt(scIdx) : 8);
                        r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                        r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                        int unIdx = c.getColumnIndex("un");
                        if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                        int ucIdx = c.getColumnIndex("uc");
                        if (ucIdx >= 0 && !c.isNull(ucIdx)) r.setUserColor(c.getInt(ucIdx));
                        recordList.add(r);
                    } while (c.moveToNext());
                }
                c.close();
                db.close();
            } catch (Exception e) { e.printStackTrace(); }

            CatRecordAdapter adapter = new CatRecordAdapter();
            adapter.setList(recordList);
            recyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener(r -> {
                Intent intent = new Intent(requireContext(), CatRecordActivity.class);
                intent.putExtra("edit_id", r.getId());
                startActivity(intent);
            });

            updateEmptyState(recordList.isEmpty());
        }

        private void loadTeethRecords() {
            java.util.List<com.example.healthrecorder.teeth.TeethRecord> recordList = new java.util.ArrayList<>();
            try {
                com.example.healthrecorder.HealthRepository repository =
                        com.example.healthrecorder.HealthRepository.getInstance(requireContext().getApplicationContext());
                android.database.sqlite.SQLiteDatabase db = repository.getReadableDatabase();
                android.database.Cursor c = db.rawQuery(
                        "SELECT t.*, u." + com.example.healthrecorder.DatabaseHelper.COLUMN_USER_NAME + " AS un "
                        + "FROM teeth_records t LEFT JOIN users u ON t.user_id=u."
                        + com.example.healthrecorder.DatabaseHelper.COLUMN_ID
                        + " ORDER BY t.date DESC", null);
                if (c.moveToFirst()) {
                    do {
                        com.example.healthrecorder.teeth.TeethRecord r = new com.example.healthrecorder.teeth.TeethRecord();
                        r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                        r.setDate(new java.util.Date(c.getLong(c.getColumnIndexOrThrow("date"))));
                        r.setPeriod(c.getInt(c.getColumnIndexOrThrow("period")));
                        r.setDuration(c.getInt(c.getColumnIndexOrThrow("duration")));
                        r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                        r.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
                        int unIdx = c.getColumnIndex("un");
                        if (unIdx >= 0) r.setUserName(c.getString(unIdx));
                        recordList.add(r);
                    } while (c.moveToNext());
                }
                c.close();
                db.close();
            } catch (Exception e) { e.printStackTrace(); }

            // 复用 TeethRecordAdapter
            com.example.healthrecorder.teeth.TeethRecordAdapter.Adapter adapter =
                    new com.example.healthrecorder.teeth.TeethRecordAdapter.Adapter();
            adapter.setList(recordList);
            recyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener(r -> {
                Intent intent = new Intent(requireContext(), TeethRecordActivity.class);
                intent.putExtra("edit_id", r.getId());
                startActivity(intent);
            });

            updateEmptyState(recordList.isEmpty());
        }

        private void updateEmptyState(boolean isEmpty) {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}
