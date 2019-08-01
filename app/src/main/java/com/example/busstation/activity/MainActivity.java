package com.example.busstation.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.busstation.R;
import com.example.busstation.fragment.MapFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zinc.libpermission.callback.IPermission;
import com.zinc.libpermission.utils.JPermissionHelper;
import com.zinc.libpermission.utils.JPermissionUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    @BindView(R.id.main_activity_nav_view)
    protected BottomNavigationView main_activity_nav_view;
    private int WRITE_COARSE_LOCATION_REQUEST_CODE = 0;
    //Fragment
    private MapFragment mapFragment;
    FragmentTransaction fragmentTransaction;//事务

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        main_activity_nav_view.setOnNavigationItemSelectedListener(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getPermission();
        init();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        hideTransaction(menuItem.getItemId());
        return true;
    }

    private void init() {
        hideTransaction(R.id.navigation_home);
    }


    //自定义隐藏Fragment
    private void hideTransaction(int viewId) {
        Log.i("=======funtion========", "hideTransaction");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (mapFragment != null) {
            fragmentTransaction.hide(mapFragment);
        }
        switch (viewId) {
            case R.id.navigation_home:
                if (mapFragment == null) {
                    mapFragment = new MapFragment();
                    fragmentTransaction.add(R.id.main_activity_fragment, mapFragment);
                }
                fragmentTransaction.show(mapFragment);
                break;
            case R.id.navigation_dashboard:
                break;
            case R.id.navigation_notifications:
                break;
        }

        if (mapFragment != null) {
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);//设置动画效果
            fragmentTransaction.commit();
        }

    }


    private void getPermission(){
        //需要回调监听
        JPermissionUtil.requestAllPermission(this, new IPermission() {
            @Override
            public void ganted() {
                Log.i(JPermissionHelper.TAG, "ganted====》申请manifest的全部");
                mapFragment.showMyLocation();
            }

            @Override
            public void denied(int requestCode, List<String> denyList) {
                Log.i(JPermissionHelper.TAG, "denied====》申请manifest的全部{code=" + requestCode + ";denyList=" + denyList + "}");
            }

            @Override
            public void canceled(int requestCode) {
                Log.i(JPermissionHelper.TAG, "canceled===》申请manifest的全部{code= " + requestCode + "}");
            }
        });
    }

}
