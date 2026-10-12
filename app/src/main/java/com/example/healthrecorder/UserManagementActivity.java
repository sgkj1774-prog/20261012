package com.example.healthrecorder;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    private LinearLayout layoutUserList;
    private HealthRepository repository;
    private int[] colors = { Color.rgb(76,175,80), Color.rgb(33,150,243),
            Color.rgb(255,152,0), Color.rgb(156,39,176), Color.rgb(244,67,54) };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        layoutUserList = findViewById(R.id.layoutUserList);
        repository = HealthRepository.getInstance(getApplicationContext());
        findViewById(R.id.btnAddUser).setOnClickListener(v -> showAddUserDialog());
        loadUsers();
    }

    private void loadUsers() {
        layoutUserList.removeAllViews();
        List<User> users = repository.getAllUsers();
        for (User u : users) {
            View item = createUserItem(u);
            layoutUserList.addView(item);
        }
    }

    private View createUserItem(User u) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(24, 20, 24, 20);

        View dot = new View(this);
        int size = (int) (14 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
        dotParams.setMargins(0, 0, 16, 0);
        dot.setLayoutParams(dotParams);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(colors[u.getColor() % colors.length]);
        dot.setBackground(gd);

        TextView name = new TextView(this);
        name.setText(u.getName());
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        name.setLayoutParams(nameParams);

        Button editBtn = new Button(this, null, android.R.attr.buttonStyleSmall);
        editBtn.setText("编辑");
        editBtn.setTextSize(12);
        editBtn.setOnClickListener(v -> showEditUserDialog(u));

        Button delBtn = new Button(this, null, android.R.attr.buttonStyleSmall);
        delBtn.setText("删除");
        delBtn.setTextSize(12);
        delBtn.setTextColor(Color.RED);
        delBtn.setOnClickListener(v -> {
            if (u.getId() == 1) {
                Toast.makeText(this, "默认用户不能删除", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("删除成员")
                    .setMessage("确定删除「" + u.getName() + "」？其记录将转移给「本人」")
                    .setPositiveButton("删除", (d, w) -> {
                        repository.deleteUser(u.getId());
                        loadUsers();
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        row.addView(dot);
        row.addView(name);
        row.addView(editBtn);
        row.addView(delBtn);
        return row;
    }

    private void showAddUserDialog() {
        final EditText editName = new EditText(this);
        editName.setHint("成员姓名");
        editName.setPadding(48, 32, 48, 32);

        final int[] selectedColor = {0};
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colors.length; i++) {
            final int ci = i;
            final int colorIdx = i;
            View colorView = new View(this);
            int sz = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(sz, sz);
            p.setMargins(8, 0, 8, 0);
            colorView.setLayoutParams(p);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(colors[i]);
            colorView.setBackground(g);
            colorView.setOnClickListener(v -> {
                selectedColor[0] = ci;
                Toast.makeText(this, "已选择颜色 " + (colorIdx+1), Toast.LENGTH_SHORT).show();
            });
            colorRow.addView(colorView);
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);
        container.addView(editName);
        container.addView(colorRow);

        new AlertDialog.Builder(this)
                .setTitle("添加成员")
                .setView(container)
                .setPositiveButton("添加", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "姓名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.addUser(new User(name, selectedColor[0]));
                    loadUsers();
                    Toast.makeText(this, "已添加 " + name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditUserDialog(User u) {
        final EditText editName = new EditText(this);
        editName.setText(u.getName());
        editName.setPadding(48, 32, 48, 32);
        final int[] selectedColor = {u.getColor()};

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colors.length; i++) {
            final int ci = i;
            final int colorIdx = i;
            View colorView = new View(this);
            int sz = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(sz, sz);
            p.setMargins(8, 0, 8, 0);
            colorView.setLayoutParams(p);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(colors[i]);
            if (i == u.getColor()) {
                g.setStroke(4, Color.BLACK);
            }
            colorView.setBackground(g);
            colorView.setOnClickListener(v -> {
                selectedColor[0] = ci;
                Toast.makeText(this, "已选择颜色 " + (colorIdx+1), Toast.LENGTH_SHORT).show();
            });
            colorRow.addView(colorView);
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);
        container.addView(editName);
        container.addView(colorRow);

        new AlertDialog.Builder(this)
                .setTitle("编辑成员")
                .setView(container)
                .setPositiveButton("保存", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "姓名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    u.setName(name);
                    u.setColor(selectedColor[0]);
                    repository.updateUser(u);
                    loadUsers();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
