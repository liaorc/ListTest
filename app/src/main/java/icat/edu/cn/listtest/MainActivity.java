package icat.edu.cn.listtest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.handmark.pulltorefresh.library.Gps;
import com.handmark.pulltorefresh.library.PositionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "main_activity";
    // 百度地图控件
    private MapView mMapView = null;
    // 百度地图对象
    private BaiduMap mBaiduMap;
    private GeoCoder mSearch;
    private boolean mMenuClosed = true;

    // 定位相关声明
    public LocationClient mLocationClient = null;

    boolean isFirstLoc = true;

    private FrameLayout mLinearLayout;
    private MenuItem mMenuButton;
    private Animation mHide;

    private float mY1, mY2;
    static final int MIN_DISTANCE = 150;

    private EditText mSearchLocationEditText;
    private Button mSearchLocationButton;
    private String mCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        SDKInitializer.initialize(getApplicationContext());
        //SDKInitializer.initialize(this);
        setContentView(R.layout.activity_main);
        init();


        //定义Maker坐标点
        Gps gps = PositionUtil.gcj02_To_Bd09(31.032611, 121.442344);
        addMarker(gps.getWgLat(), gps.getWgLon());
        //addMarker(31.03939935229, 121.44827284306);
        CoordinateConverter converter = new CoordinateConverter();
        LatLng p1 = new LatLng(31.032611, 121.442344);
        converter.coord(p1);
        converter.from(CoordinateConverter.CoordType.COMMON);
        addMarker(converter.convert().latitude, converter.convert().longitude);

        mSearch = GeoCoder.newInstance();
        OnGetGeoCoderResultListener listener = new OnGetGeoCoderResultListener() {
            public void onGetGeoCodeResult(GeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有检索到结果
                    if(result.error == SearchResult.ERRORNO.PERMISSION_UNFINISHED) {
                        mSearch.geocode(new GeoCodeOption()
                                .city("上海")
                                .address("东川路800号"));
                    }
                    Log.d(TAG, "没有结果");
                    Log.d(TAG, "error code: " + result.error);
                    return;
                }
                //获取地理编码结果
                addMarker(result.getLocation().latitude, result.getLocation().longitude);

            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有找到检索结果
                }
                //获取反向地理编码结果
            }
        };
        mSearch.setOnGetGeoCodeResultListener(listener);
        mSearch.geocode(new GeoCodeOption()
                .city("上海市")
                .address("上海市闵行区东川路800号"));

        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                showLocation(marker);
                return false;
            }
        });

        mHide = AnimationUtils.loadAnimation(getApplication(), R.anim.hide_menu);
        mHide.setFillAfter(true);
        mLinearLayout = (FrameLayout)findViewById(R.id.nothing_layout);
        mLinearLayout.setFocusable(true);
        mLinearLayout.setClickable(true);

        //mLinearLayout.startAnimation(mHide);
        mLinearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN :
                        mY1 = event.getY();
                        break;
                    case MotionEvent.ACTION_UP :
                        mY2 = event.getY();
                        float deltaY = mY1 - mY2;
                        if(deltaY > MIN_DISTANCE) {
                            toggleMenu();
                        }
                        break;

                }
                return false;
            }
        });

        mSearchLocationEditText = (EditText)findViewById(R.id.search_locationEditText);
        mSearchLocationButton = (Button)findViewById(R.id.search_locationButton);
        mSearchLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mSearch = GeoCoder.newInstance();
                OnGetGeoCoderResultListener listener = new OnGetGeoCoderResultListener() {
                    public void onGetGeoCodeResult(GeoCodeResult result) {
                        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                            //没有检索到结果
                            if(result.error == SearchResult.ERRORNO.PERMISSION_UNFINISHED) {
                                mSearch.geocode(new GeoCodeOption()
                                        .city(mCity)
                                        .address(mSearchLocationEditText.getText().toString()));
                            }
                            Log.d(TAG, "没有结果");
                            Log.d(TAG, "error code: " + result.error);
                            return;
                        }
                        //获取地理编码结果
                        addMarker(result.getLocation().latitude, result.getLocation().longitude);
                        MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(result.getLocation(), 16);   //设置地图中心点以及缩放级别
                        mBaiduMap.animateMapStatus(u);

                    }

                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                            //没有找到检索结果
                        }
                        //获取反向地理编码结果
                    }
                };
                mSearch.setOnGetGeoCodeResultListener(listener);
                mSearch.geocode(new GeoCodeOption()
                        .city(mCity)
                        .address(mSearchLocationEditText.getText().toString()));
                Log.d(TAG, "addr: " + mSearchLocationEditText.getText().toString());
                Log.d(TAG, "city: " + mCity);


                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchLocationEditText.getWindowToken(), 0);
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        mLinearLayout.setVisibility(View.GONE);
    }

    private void toggleMenu() {
        Animation rotation;
        Animation menuAnime;
        if(mMenuClosed) {
            rotation = AnimationUtils.loadAnimation(getApplication(), R.anim.rotate_test);
            menuAnime = AnimationUtils.loadAnimation(getApplication(), R.anim.show_menu);
            mLinearLayout.setVisibility(View.VISIBLE);
        } else {
            rotation = AnimationUtils.loadAnimation(getApplication(), R.anim.rotate_test2);
            menuAnime = AnimationUtils.loadAnimation(getApplication(), R.anim.hide_menu);
            menuAnime.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    //
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mLinearLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    //
                }
            });
        }
        mMenuClosed = !mMenuClosed;
        rotation.setFillAfter(true);
        mMenuButton.getActionView().startAnimation(rotation);
        mLinearLayout.startAnimation(menuAnime);
    }

    private void hideMenu() {
        mLinearLayout.setAnimation(mHide);
    }

    private void showLocation(Marker marker) {
        // 创建InfoWindow展示的view

        LatLng pt = null;
        double latitude, longitude;
        latitude = marker.getPosition().latitude;
        longitude = marker.getPosition().longitude;

        View view = LayoutInflater.from(this).inflate(R.layout.map_item, null); //自定义气泡形状
        TextView textView = (TextView) view.findViewById(R.id.my_position);
        pt = new LatLng(latitude, longitude);
        textView.setText(marker.getTitle());

        // 定义用于显示该InfoWindow的坐标点
        // 创建InfoWindow的点击事件监听者
        InfoWindow.OnInfoWindowClickListener listener = new InfoWindow.OnInfoWindowClickListener() {
            public void onInfoWindowClick() {
                mBaiduMap.hideInfoWindow();//影藏气泡

            }
        };

        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromView(view);
        // 创建InfoWindow
        //InfoWindow infoWindow = new InfoWindow(view, pt, -10);
        InfoWindow infoWindow = new InfoWindow(bitmap, pt, -100, listener);
        mBaiduMap.showInfoWindow(infoWindow); //显示气泡
    }

    private void showPopupWindow() {

        // 一个自定义的布局，作为显示的内容
        View contentView = getLayoutInflater().inflate(
                R.layout.pop_window, null);

        final PopupWindow popupWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,true);

        popupWindow.setTouchable(true);

        popupWindow.setTouchInterceptor(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Log.i("mengdd", "onTouch : ");

                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });

        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
        // 我觉得这里是API的一个bug
//        popupWindow.setBackgroundDrawable(getResources().getDrawable(
//                R.drawable.selectmenu_bg_downward));

        popupWindow.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.pop_window_bg));

        // 设置好参数之后再show
        Window window = getWindow();
        View v = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", "android");
        popupWindow.showAsDropDown(v.findViewById(resId));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        mMenuButton = menu.findItem(R.id.menu_item_qr_scan);
        mMenuButton.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_qr_scan:
                Animation rotation = AnimationUtils.loadAnimation(getApplication(), R.anim.rotate_test);

                final MenuItem item1 = item;
                item.setActionView(R.layout.action_view);
                View itemView = item.getActionView();
                rotation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        item1.setActionView(null);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                itemView.startAnimation(rotation);
                //item.getIcon().

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void addMarker(Double lat, Double lon) {
        LatLng point = new LatLng(lat, lon);
        Log.d(TAG, "得到位置: " + lon + "," + lat  );
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_marka);
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap);
        //在地图上添加Marker，并显示
        //mBaiduMap.addOverlay(option);
        Marker marker = (Marker) mBaiduMap.addOverlay(option);
        marker.setTitle("卧槽我要写长一点才知道呢");
        Bundle bundle = new Bundle();

        bundle.putSerializable("recore", "ddd");
        marker.setExtraInfo(bundle);
    }

    public BDLocationListener myListener = new BDLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                Log.d(TAG, "return without view");
                return;
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                            // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);    //设置定位数据


            if (isFirstLoc) {
                isFirstLoc = false;
                mCity = location.getCity();
                //Log.d(TAG, "get city: " + mCity);

                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 16);   //设置地图中心点以及缩放级别
//              MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }
        }
    };

    /**
     * 初始化方法
     */
    private void init() {
        mMapView = (MapView) findViewById(R.id.bmapview);
        mMapView.showZoomControls(false);  // disable zoom buttons
        mBaiduMap = mMapView.getMap();

        mBaiduMap.setMyLocationEnabled(true);
        mLocationClient = new LocationClient(getApplicationContext()); // 实例化LocationClient类
        setLocationOption();
        mLocationClient.registerLocationListener(myListener); // 注册监听函数
        //mLocationClient.requestLocation();
        mLocationClient.start();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        mLocationClient.requestLocation();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }
    @Override
    protected void onDestroy() {
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        mMapView.onDestroy();
        mMapView = null;
        mSearch.destroy();
        super.onDestroy();
    }

    private void setLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开GPS
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        option.setCoorType("bd09ll"); // 返回的定位结果是百度经纬度,默认值gcj02
        option.setScanSpan(1000); // 设置发起定位请求的间隔时间为5000ms
        option.setIsNeedAddress(true); // 返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true); // 返回的定位结果包含手机机头的方向

        mLocationClient.setLocOption(option);
    }

}