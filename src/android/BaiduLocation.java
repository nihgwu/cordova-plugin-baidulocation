package org.nihgwu.cordova.plugin.baidulocation;


import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
//import com.baidu.location.BDNotifyListener;//假如用到位置提醒功能，需要import该类
import com.baidu.location.Poi;

public class BaiduLocation extends CordovaPlugin {

	private static final String STOP_ACTION = "stop";
	private static final String GET_ACTION = "getCurrentPosition";

	public LocationClient locationClient = null;
	public BDLocationListener myListener = null;

	public JSONObject jsonObj = new JSONObject();
	public boolean result = false;
	public CallbackContext callbackContext;


	private static final Map<Integer, String> ERROR_MESSAGE_MAP = new HashMap<Integer, String>();
	private static final String DEFAULT_ERROR_MESSAGE = "服务端定位失败";

	static {
		ERROR_MESSAGE_MAP.put(61, "GPS定位结果");
		ERROR_MESSAGE_MAP.put(62, "无法获取有效定位依据，定位失败，请检查运营商网络或者wifi网络是否正常开启，尝试重新请求定位。");
		ERROR_MESSAGE_MAP.put(63, "网络异常，没有成功向服务器发起请求，请确认当前测试手机网络是否通畅，尝试重新请求定位");
		ERROR_MESSAGE_MAP.put(65, "定位缓存的结果");
		ERROR_MESSAGE_MAP.put(66, "离线定位结果。通过requestOfflineLocaiton调用时对应的返回结果");
		ERROR_MESSAGE_MAP.put(67, "离线定位失败。通过requestOfflineLocaiton调用时对应的返回结果");
		ERROR_MESSAGE_MAP.put(68, "网络连接失败时，查找本地离线定位时对应的返回结果。");
		ERROR_MESSAGE_MAP.put(161, "网络定位定位成功");
	};

	public String getErrorMessage(int locationType) {
		String result = ERROR_MESSAGE_MAP.get(locationType);
		if (result == null) {
			result = DEFAULT_ERROR_MESSAGE;
		}
		return result;
	}

	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) {
		setCallbackContext(callbackContext);
		if (GET_ACTION.equals(action)) {
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {

					locationClient = new LocationClient(cordova.getActivity().getApplicationContext());
					myListener = new MyLocationListener();
					locationClient.registerLocationListener( myListener );    //注册监听函数

	        LocationClientOption option = new LocationClientOption();
	        //option.setLocationMode(LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
	        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
	        option.setTimeOut(10000);
	        int span=1000;
	        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
	        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
	        option.setOpenGps(true);//可选，默认false,设置是否使用gps
	        //option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
	        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
	        //option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
					//option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
	        //option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
					//option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
					locationClient.setLocOption(option);

					locationClient.start();
					//locationClient.requestLocation();
				}

			});
			return true;
		} else if (STOP_ACTION.equals(action)) {
			locationClient.stop();
			callbackContext.success(200);
			return true;
		} else {
			callbackContext
					.error(PluginResult.Status.INVALID_ACTION.toString());
		}

		while (result == false) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public class MyLocationListener implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return;
			try {
				JSONObject coords = new JSONObject();
				jsonObj.put("time", location.getTime());
				coords.put("latitude", location.getLatitude());
				coords.put("longitude", location.getLongitude());
				coords.put("radius", location.getRadius());

				jsonObj.put("coords", coords);

				int locationType = location.getLocType();

				jsonObj.put("locationType", locationType);
				jsonObj.put("code", locationType);
				jsonObj.put("message", getErrorMessage(locationType));
				jsonObj.put("address", location.getAddrStr());

				switch (location.getLocType()) {

				case BDLocation.TypeGpsLocation:
					coords.put("speed", location.getSpeed());
					coords.put("altitude", location.getAltitude());
					break;

				case BDLocation.TypeNetWorkLocation:
					/*java.util.List<Poi> list = location.getPoiList();
					if(list != null){
						JSONArray array = new JSONArray();
						for(Poi p:list){
							array.put(p.getName());
						}
						coords.put("pois", array);
					}*/
					jsonObj.put("describe", location.getLocationDescribe());
					jsonObj.put("street", location.getStreet());
					break;
				}

				Log.d("BaiduLocationPlugin", "run: " + jsonObj.toString());
				callbackContext.success(jsonObj);
				result = true;
			} catch (JSONException e) {
				callbackContext.error(e.getMessage());
				result = true;
			}

		}

		public void onReceivePoi(BDLocation poiLocation) {
			// TODO Auto-generated method stub
		}
	}

	@Override
	public void onDestroy() {
		if (locationClient != null && locationClient.isStarted()) {
			locationClient.stop();
			locationClient = null;
		}
		super.onDestroy();
	}

	private void logMsg(String s) {
		System.out.println(s);
	}

	public CallbackContext getCallbackContext() {
		return callbackContext;
	}

	public void setCallbackContext(CallbackContext callbackContext) {
		this.callbackContext = callbackContext;
	}
}