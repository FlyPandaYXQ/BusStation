package com.example.busstation.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MultiPointItem;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.busline.BusLineItem;
import com.amap.api.services.busline.BusLineQuery;
import com.amap.api.services.busline.BusLineResult;
import com.amap.api.services.busline.BusLineSearch;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteBusLineItem;
import com.amap.api.services.route.RouteBusWalkItem;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.route.WalkStep;
import com.example.busstation.R;
import com.example.busstation.activity.SearchActivity;
import com.example.busstation.until.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class MapFragment extends Fragment implements AMapLocationListener, RouteSearch.OnRouteSearchListener, LocationSource {
    @BindView(R.id.fragment_map)
    protected MapView fragment_map;
    @BindView(R.id.fragment_map_et)
    protected EditText fragment_map_et;
    @BindView(R.id.fragment_map_delete)
    protected LinearLayout fragment_map_delete;

    private AMap aMap;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;

    private LatLng myLatLocation;
    private float distance;
    private boolean isFirstLoc = true;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    //城市code
    private String code;
    //搜索的车站

    private String search;
    private String startStation;
    private String endStation;
    private String isWarning = "1";//0 不提醒 1提醒

    private String stationNameLast = "";
    private BusLineResult mBusLineResult;
    private List<BusStationItem> busStations;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("=======funtion========", "MapFragment onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        ButterKnife.bind(this, view);
        EventBus.getDefault().register(this);
        //创建地图
        fragment_map.onCreate(savedInstanceState);
        initMap();
        if (search != null) {
            fragment_map_et.setText(search);
        }
        Log.i("=======funtion========", "MapFragment onCreateView");
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        fragment_map.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragment_map.onDestroy();
    }

    private void initMap() {
        Log.i("=======funtion========", "MapFragment initMap");
        if (aMap == null) {
            aMap = fragment_map.getMap();
        }
        aMap.setTrafficEnabled(true);// 显示实时交通状况
        aMap.setLocationSource(this);//通过aMap对象设置定位数据源的监听
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(true); //显示默认的定位按钮
        aMap.setMyLocationEnabled(true);// 可触发定位并显示当前位置
        location();
        showMyLocation();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleData(MessageEvent messageEvent) {
        aMap.clear();
        List<LatLng> latlngList = new ArrayList<>();
        HashMap<String, String> hashMap = messageEvent.getMessage();
        search = hashMap.get("search");
        startStation = hashMap.get("startStation");
        endStation = hashMap.get("endStation");
        isWarning = hashMap.get("isWarning");
        if (search != null) {
            fragment_map_et.setText(search);
        }
        mBusLineResult = messageEvent.getBusLineResult();
        busStations = messageEvent.getBusStations();
        for (BusStationItem stationItem : busStations) {
            LatLonPoint point = stationItem.getLatLonPoint();
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            latlngList.add(latLng);
        }
        showLineOnMap(latlngList); // 显示Line
        showMarkerOnMap(busStations); // 显示Marker
    }

    public void showMyLocation() {
        //自定义地图
        // CustomMapStyleOptions customMapStyleOptions = new CustomMapStyleOptions();
        // customMapStyleOptions.setStyleId("bdbf7cc2c940cec372d24fb1565f53b0");
        // customMapStyleOptions.setEnable(true);
        // aMap.setCustomMapStyle(customMapStyleOptions);
        //实现定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE);//连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.setLocationSource(this);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        //定位
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        // 设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
    }

    public void location() {
        //初始化定位
        aMapLocationClient = new AMapLocationClient(getContext());
        //设置定位回调监听
        aMapLocationClient.setLocationListener(this);
        //初始化定位参数
        aMapLocationClientOption = new AMapLocationClientOption();
        //设置定位模式为Hight_Accuracy高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        aMapLocationClientOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        aMapLocationClientOption.setOnceLocation(false);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        aMapLocationClientOption.setOnceLocationLatest(true);
        //设置是否强制刷新WIFI，默认为强制刷新
        aMapLocationClientOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        aMapLocationClientOption.setMockEnable(false);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        aMapLocationClientOption.setHttpTimeOut(3000);
        //设置定位间隔,单位毫秒,默认为2000ms
        aMapLocationClientOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        //启动定位
        aMapLocationClient.startLocation();
    }


    @OnTextChanged(R.id.fragment_map_et)
    protected void textChange() {
        if (fragment_map_et.getText().length() == 0) {
            fragment_map_delete.setVisibility(View.GONE);
        } else {
            fragment_map_delete.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.fragment_map_et)
    protected void toSearchActivity() {
//        fragment_map_et.setKeyListener(null);
//        fragment_map_et.setInputType(InputType.TYPE_NULL);
//        fragment_map_et.setFocusable(false);
//        fragment_map_et.clearFocus();
//        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(fragment_map_et.getWindowToken(), 0);
    }


    @OnClick(R.id.fragment_map_delete)
    protected void deleteText() {
        aMap.clear();
        search = "";
        fragment_map_et.setText("");
    }

    @OnClick(R.id.fragment_map_search)
    protected void search() {
//        aMap.clear();
//        showMyLocation();
        String search = fragment_map_et.getText().toString();
        Intent intent = new Intent(getActivity(), SearchActivity.class);
        intent.putExtra("search", search);
        intent.putExtra("code", code);
        startActivity(intent);

    }

    //定位监听
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        Log.i("=======funtion========", "onLocationChanged");
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation != null
                    && aMapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                //定位成功回调信息，设置相关消息
                aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                double la = aMapLocation.getLatitude();//获取纬度
                double lo = aMapLocation.getLongitude();//获取经度
                myLatLocation = new LatLng(la, lo);
                System.out.println("=================== myLatLocation " + myLatLocation);
                aMapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//定位时间
                aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                aMapLocation.getCountry();//国家信息
                aMapLocation.getProvince();//省信息
                aMapLocation.getCity();//城市信息
                aMapLocation.getDistrict();//城区信息
                aMapLocation.getStreet();//街道信息
                aMapLocation.getStreetNum();//街道门牌号信息
                code = aMapLocation.getCityCode();//城市编码
                aMapLocation.getAdCode();//地区编码
                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    //设置缩放级别
                    //将地图移动到定位点
                    //点击定位按钮 能够将地图的中心移动到定位点
                    //添加图钉
                    //  aMap.addMarker(getMarkerOptions(amapLocation));
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(aMapLocation.getCountry() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getCity() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getDistrict() + ""
                            + aMapLocation.getStreet() + ""
                            + aMapLocation.getStreetNum());

                    isFirstLoc = false;
                }

                calculateDistance();
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
                Toast.makeText(getActivity(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }


    // 显示Line
    private void showLineOnMap(List<LatLng> list) {
        Log.i("=======funtion========", "showLineOnMap");
        RouteSearch routeSearch = new RouteSearch(getActivity());
        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(new LatLonPoint(list.get(0).latitude, list.get(0).longitude), new LatLonPoint(list.get(list.size() - 1).latitude, list.get(list.size() - 1).longitude));
        RouteSearch.BusRouteQuery query = new RouteSearch.BusRouteQuery(fromAndTo, RouteSearch.BusLeaseWalk, code, 0);
        routeSearch.calculateBusRouteAsyn(query);//开始规划路径
        routeSearch.setRouteSearchListener(this);

    }

    // 显示Marker
    private void showMarkerOnMap(List<BusStationItem> busStations) {
        Log.i("=======funtion========", "showMarkerOnMap");
        int startIndex = 0;
        int endIndex = 0;
        MarkerOptions markerOption = new MarkerOptions();
        List<MultiPointItem> list = new ArrayList<>();
        for (int i = 0; i < busStations.size(); i++) {
            BusStationItem stationItem = busStations.get(i);
            LatLonPoint point = stationItem.getLatLonPoint();
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            list.add(new MultiPointItem(latLng));
            if (stationItem.getBusStationName().equals(startStation)) {
                startIndex = i;
            }
            if (stationItem.getBusStationName().equals(endStation)) {
                endIndex = i;
            }
        }
        markerOption.draggable(true);//设置Marker可拖动
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        markerOption.setFlat(false);//设置marker平贴地图效果
        //设置起点和终点，其中起点支持多个
        for (int i = 0; i < list.size(); i++) {
            MultiPointItem multiPointItem = list.get(i);
            if (i == startIndex) {
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_start));
            } else if (i == endIndex) {
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_end));
            } else {
                markerOption.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_station));
            }
            markerOption.position(multiPointItem.getLatLng());
            aMap.addMarker(markerOption);
        }
        calculateDistance();
    }


    //计算距离
    private void calculateDistance() {
        Log.i("=======funtion========", "calculateDistance");
        float d = 0;
        BusStationItem b = null;
        String stationName = null;
        for (BusStationItem stationItem : busStations) {
            System.out.println("=================== stationItem " + stationItem);
            LatLonPoint point = stationItem.getLatLonPoint();
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            distance = AMapUtils.calculateLineDistance(myLatLocation, latLng);
            if (d == 0) {
                d = distance;
                b = stationItem;
                stationName = stationItem.getBusStationName();
            } else {
                if (d > distance) {
                    d = distance;
                    b = stationItem;
                    stationName = stationItem.getBusStationName();
                }
            }
        }
        System.out.println("=================== d " + d);
        System.out.println("=================== stationName " + stationName);
        System.out.println("=================== stationNameLast " + stationNameLast);
        System.out.println("=================== isWarning " + isWarning);
        System.out.println("=================== endStation " + endStation);
        if ("1".equals(isWarning)){
            if (d < 100 && d > 0) {
                if (!stationNameLast.equals(stationName)) {
                    Toast.makeText(getActivity(), stationName + "快到了", Toast.LENGTH_LONG).show();
                }
                stationNameLast = stationName;
            }
        } else if ("0".equals(isWarning)){
            if (endStation.equals(stationName)&& d < 100 && d > 0) {
                Toast.makeText(getActivity(), stationName + "快到了", Toast.LENGTH_LONG).show();
                endStation = "";
            }
        }
    }

    //规划路线
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {
        Log.e("CF", "onBusRouteSearched: " + i);
        //几种公交路线
        List<BusPath> busPathList = busRouteResult.getPaths();
        //选择第一条
        List<BusStep> busSteps = busPathList.get(0).getSteps();
        for (BusStep bs : busSteps) {
            //获取该条路线某段公交路程步行的点
            RouteBusWalkItem routeBusWalkItem = bs.getWalk();
            if (routeBusWalkItem != null) {
                List<WalkStep> wsList = routeBusWalkItem.getSteps();
                ArrayList<LatLng> walkPoint = new ArrayList<>();
                for (WalkStep ws : wsList) {
                    List<LatLonPoint> points = ws.getPolyline();
                    for (LatLonPoint lp : points) {
                        walkPoint.add(new LatLng(lp.getLatitude(), lp.getLongitude()));
                    }
                }
                //添加步行点
                aMap.addPolyline(new PolylineOptions()
                        .addAll(walkPoint)
                        .width(20)
                        //绘制成大地线
                        .geodesic(false)
                        //设置画线的颜色
                        .color(Color.argb(200, 0, 255, 0)));
            }
            //获取该条路线某段公交路路程的点
            List<RouteBusLineItem> rbli = bs.getBusLines();
            ArrayList<LatLng> busPoint = new ArrayList<>();
            for (RouteBusLineItem one : rbli) {
                List<LatLonPoint> points = one.getPolyline();
                for (LatLonPoint lp : points) {
                    busPoint.add(new LatLng(lp.getLatitude(), lp.getLongitude()));
                }
            }
            //添加公交路线点
            aMap.addPolyline(new PolylineOptions()
                    .addAll(busPoint)
                    .width(20)
                    //绘制成大地线
                    .geodesic(false)
                    //设置画线的颜色
                    .color(Color.argb(200, 0, 255, 0)));
        }
    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        Log.i("=======funtion========", "activate");
        mListener = onLocationChangedListener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(getActivity());
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }
}
