package com.example.busstation.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.busline.BusLineItem;
import com.amap.api.services.busline.BusLineQuery;
import com.amap.api.services.busline.BusLineResult;
import com.amap.api.services.busline.BusLineSearch;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.core.LatLonPoint;
import com.example.busstation.R;
import com.example.busstation.until.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class SearchActivity extends AppCompatActivity implements BusLineSearch.OnBusLineSearchListener {
    @BindView(R.id.activity_search_tv)
    protected TextView activity_search_tv;
    @BindView(R.id.activity_search_start)
    protected EditText activity_search_start;
    @BindView(R.id.activity_search_end)
    protected EditText activity_search_end;
    @BindView(R.id.activity_search_line1)
    protected LinearLayout activity_search_line1;
    @BindView(R.id.activity_search_line1_start)
    protected TextView activity_search_line1_start;
    @BindView(R.id.activity_search_line1_end)
    protected TextView activity_search_line1_end;
    @BindView(R.id.activity_search_line2)
    protected LinearLayout activity_search_line2;
    @BindView(R.id.activity_search_line2_start)
    protected TextView activity_search_line2_start;
    @BindView(R.id.activity_search_line2_end)
    protected TextView activity_search_line2_end;

    private String search;
    private String startStation;
    private String endStation;
    private String isWarning = "1";//0 不提醒 1提醒

    private List<BusStationItem> getBusStations;
    private BusLineSearch busLineSearch;
    private BusLineResult mBusLineResult;
    private List<BusStationItem> busStations1, busStations2;

    private int busLine = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initData();
        activity_search_line1.setSelected(true);
    }

    private void initData() {
        Intent intent = getIntent();
        search = intent.getStringExtra("search");
        String code = intent.getStringExtra("code");
        if (search != null) {
            activity_search_tv.setText(search);
        }
        BusLineQuery busLineQuery = new BusLineQuery(search, BusLineQuery.SearchType.BY_LINE_NAME, code);
        busLineQuery.setPageSize(20);
        busLineQuery.setPageNumber(0);
        busLineSearch = new BusLineSearch(this, busLineQuery);
        busLineSearch.setOnBusLineSearchListener(this);
        busLineSearch.searchBusLineAsyn();
    }


    @OnClick(R.id.activity_search_search)
    protected void search() {
        startStation = activity_search_start.getText().toString();
        endStation = activity_search_end.getText().toString();
        System.out.println("=================== search()endStation " + endStation);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("search", search);
        map.put("startStation", startStation);
        map.put("endStation", endStation);
        map.put("isWarning", isWarning);
        MessageEvent messageEvent = new MessageEvent(map);
        switch (busLine) {
            case 0:
                messageEvent.setBusStations(busStations1);
                break;
            case 1:
                messageEvent.setBusStations(busStations2);
                break;
        }
        messageEvent.setBusLineResult(mBusLineResult);
        EventBus.getDefault().post(messageEvent);
        finish();
    }

    @OnCheckedChanged({R.id.activity_search_yes, R.id.activity_search_no})
    protected void isWarning(RadioButton radioButton) {
        boolean checked = radioButton.isChecked();
        switch (radioButton.getId()) {
            case R.id.activity_search_yes:
                if (checked) {
                    isWarning = "1";
                }
                break;
            case R.id.activity_search_no:
                if (checked) {
                    isWarning = "0";
                }
                break;
        }
    }

    @OnClick({R.id.activity_search_line1, R.id.activity_search_line2})
    protected void lineClick(View view) {
        switch (view.getId()) {
            case R.id.activity_search_line1:
                busLine = 0;
                activity_search_line1.setSelected(true);
                activity_search_line2.setSelected(false);
                break;
            case R.id.activity_search_line2:
                busLine = 1;
                activity_search_line2.setSelected(true);
                activity_search_line1.setSelected(false);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleData(MessageEvent messageEvent) {
        HashMap<String, String> hashMap = messageEvent.getMessage();
        search = hashMap.get("search");
        startStation = hashMap.get("startStation");
        endStation = hashMap.get("endStation");
        isWarning = hashMap.get("isWarning");
    }


    @Override
    public void onBusLineSearched(BusLineResult busLineResult, int resultCode) {
        if (resultCode == 1000) {
            mBusLineResult = busLineResult;
            List<BusLineItem> busLines = busLineResult.getBusLines();
            busStations1 = busLines.get(0).getBusStations();
            busStations2 = busLines.get(1).getBusStations();
            activity_search_line1_start.setText(busStations1.get(0).getBusStationName());
            activity_search_line1_end.setText(busStations1.get(busStations1.size() - 1).getBusStationName());
            activity_search_line2_start.setText(busStations2.get(0).getBusStationName());
            activity_search_line2_end.setText(busStations2.get(busStations2.size() - 1).getBusStationName());
        }
    }
}
