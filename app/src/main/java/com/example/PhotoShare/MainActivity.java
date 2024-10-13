package com.example.PhotoShare;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import Fragment.AddFragment;
import Fragment.HomeFragment;
import Fragment.MineFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView main_bottom_nv;
    private HomeFragment mHomeFragment;
    private AddFragment mAddFragment;
    private MineFragment mMineFragment;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化控件
        main_bottom_nv = findViewById(R.id.main_bottom_nv);

        //main_bottom_nv tab点击切换
        main_bottom_nv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (item.getItemId() == R.id.home) {
                    selectedFragment(0);
                } else if (item.getItemId() == R.id.add) {
                    selectedFragment(1);
                } else if (item.getItemId() == R.id.mine) {
                    selectedFragment(2);
                }

                return true;
            }
        });

        //默认首页选中
        selectedFragment(0);

    }



    public void selectedFragment(int position) {
        //获取fragmentManager管理器
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        hideFragment(transaction);
        if (position == 0) {
            if (mHomeFragment == null) {
                mHomeFragment = new HomeFragment();
                transaction.add(R.id.content, mHomeFragment);
            } else {
                transaction.show(mHomeFragment);
            }
        } else if (position == 1) {
            if (mAddFragment == null) {
                mAddFragment = new AddFragment();
                transaction.add(R.id.content, mAddFragment);
            } else {
                transaction.show(mAddFragment);

            }
        }  else {
            if (mMineFragment == null) {
                mMineFragment = new MineFragment();
                transaction.add(R.id.content, mMineFragment);
            } else {
                transaction.show(mMineFragment);
            }
        }

        transaction.commit();

    }


    private void hideFragment(FragmentTransaction transaction) {
        if (mHomeFragment != null) {
            transaction.hide(mHomeFragment);
        }
        if (mMineFragment != null) {
            transaction.hide(mMineFragment);
        }
        if (mAddFragment != null) {
            transaction.hide(mAddFragment);
        }

    }
}


