package com.xiaojiang.sbu.justwifi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import static android.util.Half.abs;
import static com.xiaojiang.sbu.justwifi.MapActivity.recordLocation;

public class WIFIManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final String GOOGLE_API_KEY = "AIzaSyALQ2qQRFQAuwKuYtyScpkxBFk4mrp7JhQ";
    public static final String googleUrl = "https://www.googleapis.com/geolocation/v1/geolocate?key=";
    private GoogleApiClient mGoogleApiClient;
    public static WIFIManager manager;
    private Application context;
    public String dataJson;
    public double lat;
    public double longit;
    public int accWifi;


    public static WIFIManager getInstance() {
        return manager;
    }

    /**
     * 注册gps监听服务
     * @param context
     */
    public static void onCreateGPS(final Application context) {
        if (manager != null && manager.mGoogleApiClient != null) return;
        Log.i("xiao jiang", "ready to open GPS");
        //实例化连接Google Service
        manager = new WIFIManager();
        manager.context = context;
        manager.mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(manager)
                .addOnConnectionFailedListener(manager)
                .addApi(LocationServices.API)
                .build();
        manager.mGoogleApiClient.connect();
        send();
    }

    public static void send() {
        GeoLocationAPI geoLocationAPI = null;
        geoLocationAPI = getCellInfo(manager.context);
        getWIFIInfo(manager.context, geoLocationAPI);
        final String json = geoLocationAPI.toJson();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = googleUrl + GOOGLE_API_KEY;
                manager.sendJsonByPost(json, url);
            }
        }).start();
    }


    public void sendJsonByPost(String json, String googleApiUrl) {

        Log.i("send: ", "" + json);
        this.dataJson = json;
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;
        BufferedReader bufferreader = null;
        String result = null;
        try {
            URL url = new URL(googleApiUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");


            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "curl/7.65.2");
            connection.setRequestProperty("Content-Type", "application/json");

            output = connection.getOutputStream();
            if (json != null && json.length() > 0) {
                output.write(json.getBytes());
            }
            if (connection.getResponseCode() == 200) {
                input = connection.getInputStream();

                bufferreader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

                StringBuffer sbf = new StringBuffer();
                String returnInfo = null;
                while ((returnInfo = bufferreader.readLine()) != null) {
                    sbf.append(returnInfo);
                    sbf.append("\r\n");
                }
                result = sbf.toString();
                Log.i("RETURN", "" + result);

                JSONObject returnJson = null;
                try {
                    returnJson = new JSONObject(result);
                    JSONObject location = returnJson.getJSONObject("location");
                    int accuracy = returnJson.getInt("accuracy");

                    if (location == null) {
                        Log.i("Hint:", "Cannot get the location according to the response");
                        return;
                    }
                    double latitude = location.getDouble("lat");
                    double longitute = location.getDouble("lng");
                    //double google_accuracy = accuracy.getDouble("accuracy");
                    lat = latitude;
                    longit = longitute;
                    accWifi = accuracy;
                    Log.i("Hint:", "Google's response: LAT:" + latitude + "  LONGIT:" + longitute + "  ACC:" + accuracy);
                    if (true)
                        Toast.makeText(context, "Google's response: LAT:" + latitude + "  LONGIT:" + longitute + "  ACC:" + accuracy, Toast.LENGTH_LONG).show();
                    recordLocation(context, latitude, longitute, (float) accuracy);//When cannot use the GPS or the GPS has low accuracy，USE the result of WIFI Location.
                } catch (JSONException e) {
                    Log.i("ERROR:", "Cannot get the location!", e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                /**close connection*/
                if (null != bufferreader) {
                    bufferreader.close();
                }
                if (null != input) {
                    input.close();
                }
                if (null != output) {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            connection.disconnect();
            Log.i("Hint: ", "------------------->>>>> POST request END <<<<<------------------");
        }
    }

    public void GPSlocation() {
    }

    /**
     * send post request
     */
    public static class GoogleWifiInfo {

        /**
         * {
         *   "considerIp": "false",
         *   "wifiAccessPoints": [
         *     {
         *         "macAddress": "20:C0:47:E7:DD:4C",
         *         "signalStrength": -45,
         *         "signalToNoiseRatio": 0
         *     },
         *     {
         *         "macAddress": "02:D5:9D:AB:BC:4C",
         *         "signalStrength": -58,
         *         "signalToNoiseRatio": 0
         *     },
         *     {
         *         "macAddress": "00:65:A3:CC:4C:00",
         *         "signalStrength": -63,
         *         "signalToNoiseRatio": 0
         *     }
         *   ]
         * }
         */

        public String macAddress;
        public int signalStrength;
        public int age;
        public short channel;
        public int signalToNoiseRatio;

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("signalStrength", signalStrength);
                jsonObject.put("age", age);
                jsonObject.put("macAddress", macAddress);
                jsonObject.put("channel", channel);
                jsonObject.put("signalToNoiseRatio", signalToNoiseRatio);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }


    public static class GoogleCellTower {
        /*
        GSM:
                {
          "cellTowers": [
            {
              "cellId": 42,
              "locationAreaCode": 415,
              "mobileCountryCode": 310,
              "mobileNetworkCode": 410,
              "age": 0,
              "signalStrength": -60,
              "timingAdvance": 15
            }
          ]
        }
        WCDMA
        {
          "cellTowers": [
            {
              "cellId": 21532831,
              "locationAreaCode": 2862,
              "mobileCountryCode": 214,
              "mobileNetworkCode": 7
            }
          ]
        }

         */
        //下面的是必填
        int cellId;//（必填）：小区的唯一标识符。在 GSM 上，这就是小区 ID (CID)；CDMA 网络使用的是基站 ID (BID)。WCDMA 网络使用 UTRAN/GERAN 小区标识 (UC-Id)，这是一个 32 位的值，由无线网络控制器 (RNC) 和小区 ID 连接而成。在 WCDMA 网络中，如果只指定 16 位的小区 ID 值，返回的结果可能会不准确。
        int locationAreaCode;//（必填）：GSM 和 WCDMA 网络的位置区域代码 (LAC)。CDMA 网络的网络 ID (NID)。
        int mobileCountryCode;//（必填）：移动电话基站的移动国家代码 (MCC)。
        int mobileNetworkCode;//（必填）：移动电话基站的移动网络代码。对于 GSM 和 WCDMA，这就是 MNC；CDMA 使用的是系统 ID (SID)。
        int signalStrength;//测量到的无线信号强度（以 dBm 为单位）。
        //下面的是选填
        int age;//自从此小区成为主小区后经过的毫秒数。如果 age 为 0，cellId 就表示当前的测量值。
        int timingAdvance;//时间提前值。

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cellId", cellId);
                jsonObject.put("locationAreaCode", locationAreaCode);
                jsonObject.put("mobileCountryCode", mobileCountryCode);
                jsonObject.put("mobileNetworkCode", mobileNetworkCode);
                jsonObject.put("signalStrength", signalStrength);
                jsonObject.put("age", age);
                jsonObject.put("timingAdvance", timingAdvance);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }


    /**
     * format data which will be send
     */
    public static class GeoLocationAPI {

        /**
         * homeMobileCountryCode : 310 移动国家代码；
         * homeMobileNetworkCode : 410 和基站有关
         * radioType : gsm
         * carrier : Vodafone 运营商名称
         * considerIp : true
         * cellTowers : []
         * wifiAccessPoints : []
         */

        public int homeMobileCountryCode;//(MCC)
        public int homeMobileNetworkCode;//(MNC)。
        public String radioType;//radioType： lte、gsm、cdma 和 wcdma。
        public String carrier;
        public boolean considerIp;
        public List<GoogleCellTower> cellTowers;
        public List<GoogleWifiInfo> wifiAccessPoints;

        public String toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("homeMobileCountryCode", homeMobileCountryCode);
                jsonObject.put("homeMobileNetworkCode", homeMobileNetworkCode);
                jsonObject.put("radioType", radioType);
                jsonObject.put("carrier", carrier);
                jsonObject.put("considerIp", considerIp);
                if (cellTowers != null) {
                    JSONArray jsonArray = new JSONArray();
                    for (GoogleCellTower t : cellTowers) jsonArray.put(t.toJson());
                    jsonObject.put("cellTowers", jsonArray);
                }
                if (wifiAccessPoints != null) {
                    JSONArray jsonArray = new JSONArray();
                    for (GoogleWifiInfo w : wifiAccessPoints) jsonArray.put(w.toJson());
                    jsonObject.put("wifiAccessPoints", jsonArray);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject.toString();


        }

    }

    /**
     * record wifi
     */
    public static class AlxScanWifi implements Comparable<AlxScanWifi> {
        public final int dBm;
        public final String ssid;
        public final String mac;
        public short channel;

        public AlxScanWifi(ScanResult scanresult) {
            dBm = scanresult.level;
            ssid = scanresult.SSID;
            mac = scanresult.BSSID;//BSSID就是传说中的mac
            channel = getChannelByFrequency(scanresult.frequency);
        }

        public AlxScanWifi(String s, int i, String s1, String imac) {
            dBm = i;
            ssid = s1;
            mac = imac;
        }

        /**
         * rank the wifi signal
         * @param wifiinfo
         * @return
         */
        public int compareTo(AlxScanWifi wifiinfo) {
            int i = wifiinfo.dBm;
            int j = dBm;
            return i - j;
        }

        /**
         * avoid wifi info repeat
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {
            boolean flag = false;
            if (obj == this) {
                flag = true;
                return flag;
            } else {
                if (obj instanceof AlxScanWifi) {
                    AlxScanWifi wifiinfo = (AlxScanWifi) obj;
                    int i = wifiinfo.dBm;
                    int j = dBm;
                    if (i == j) {
                        String s = wifiinfo.mac;
                        String s1 = this.mac;
                        if (s.equals(s1)) {
                            flag = true;
                            return flag;
                        }
                    }
                    flag = false;
                } else {
                    flag = false;
                }
            }
            return flag;
        }

        public int hashCode() {
            int i = dBm;
            int j = mac.hashCode();
            return i ^ j;
        }

    }

    /**
     * get signal channel according to frequency
     *
     * @param frequency
     * @return
     */
    public static short getChannelByFrequency(int frequency) {
        short channel = 0;
        switch (frequency) {
            case 2412:
                channel = 1;
                break;
            case 2401:
                channel = 1;
                break;
            case 2423:
                channel = 1;
                break;
            case 2406:
                channel = 2;
                break;
            case 2417:
                channel = 2;
            case 2428:
                channel = 2;
                break;
            case 2411:
                channel = 3;
            case 2422:
                channel = 3;
                break;
            case 2433:
                channel = 3;
                break;
            case 2416:
                channel = 4;
                break;
            case 2427:
                channel = 4;
                break;
            case 2438:
                channel = 4;
                break;
            case 2421:
                channel = 5;
                break;
            case 2432:
                channel = 5;
                break;
            case 2443:
                channel = 5;
                break;
            case 2426:
                channel = 6;
                break;
            case 2437:
                channel = 6;
                break;
            case 2448:
                channel = 6;
                break;
            case 2431:
                channel = 7;
                break;
            case 2442:
                channel = 7;
                break;
            case 2453:
                channel = 7;
                break;
            case 2436:
                channel = 8;
                break;
            case 2447:
                channel = 8;
                break;
            case 2458:
                channel = 8;
                break;
            case 2441:
                channel = 9;
            case 2452:
                channel = 9;
                break;
            case 2463:
                channel = 9;
            case 2446:
                channel = 10;
                break;
            case 2457:
                channel = 10;
                break;
            case 2468:
                channel = 10;
                break;
            case 2451:
                channel = 11;
                break;
            case 2462:
                channel = 11;
                break;
            case 2473:
                channel = 11;
                break;
            case 2456:
                channel = 12;
                break;
            case 2467:
                channel = 12;
                break;
            case 2478:
                channel = 12;
                break;
            case 2461:
                channel = 13;
                break;
            case 2472:
                channel = 13;
                break;
            case 2483:
                channel = 13;
                break;
            case 2484:
                channel = 14;
                break;
            case 2495:
                channel = 14;
                break;
            case 5180:
                channel = 30;
                break;
            case 5200:
                channel = 40;
                break;
            case 5220:
                channel = 44;
                break;
            case 5240:
                channel = 48;
                break;
            case 5260:
                channel = 52;
                break;
            case 5280:
                channel = 56;
                break;
            case 5300:
                channel = 62;
                break;
            case 5320:
                channel = 64;
                break;
            case 5745:
                channel = 149;
                break;
            case 5765:
                channel = 153;
                break;
            case 5785:
                channel = 157;
                break;
            case 5805:
                channel = 161;
                break;
            case 5825:
                channel = 165;
                break;
        }
        Log.i("Hint:", "channel is :" + channel);
        return channel;
    }


    /**
     * get base station info
     * mcc, mnc
     */
    public static GeoLocationAPI getCellInfo(Context context) {

        Log.i("Hint:", "Move in the getCellInfo");

        //通过TelephonyManager 获取lac:mcc:mnc:cell-id
        GeoLocationAPI cellInfo = new GeoLocationAPI();
        TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager == null) return cellInfo;
        // return MCC + MNC
        /*# MCC，Mobile Country Code，310-313；
         * # MNC，Mobile Network Code，；
         * # LAC，Location Area Code，;
         * # CID，Cell Identity；
         * # BSSS，Base station signal strength.
         */
        String operator = mTelephonyManager.getNetworkOperator();
        Log.i("Hint:", "The info of the BASE STATION is :" + operator);
        if (operator == null || operator.length() < 5) {
            Log.i("ERROR:", "Cannot get the base station info, check the SIM card!");
            return cellInfo;
        }
        int mcc = Integer.parseInt(operator.substring(0, 3));
        int mnc = Integer.parseInt(operator.substring(3));
        int lac;
        int cellId;
        //get the CID and LAD 获取LAC、CID的方式

        @SuppressLint("MissingPermission") CellLocation cellLocation = (CellLocation) mTelephonyManager.getCellLocation();

        if (cellLocation == null) {
            Log.i("ERROR:", "Can not get the base station info, check the SIM card! ");
            return cellInfo;
        }
        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            Log.i("Hint:", "PHONE_TYPE_GSM");
            GsmCellLocation location = (GsmCellLocation) cellLocation;
            lac = location.getLac();
            cellId = location.getCid();


            //This info is used for Cell location
            Log.i("Hint: ", " MCC(Mobile Country Code) = " + mcc + "\t MNC(Mobile Network Code) = " + mnc + "\t LAC(Location Area Code) = " + lac + "\t CID(Cell Identity) = " + cellId);
        } else if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            // Get cell info
            Log.i("Hint:", "PHONE_TYPE_CDMA");

            @SuppressLint("MissingPermission") CdmaCellLocation location1 = (CdmaCellLocation) mTelephonyManager.getCellLocation();
            lac = location1.getNetworkId();
            cellId = location1.getBaseStationId();
            cellId /= 16;
        }else {
            Log.i("Hint:","ELSE TYPE");
            return cellInfo;

        }
        cellInfo.homeMobileCountryCode = mcc;
        cellInfo.homeMobileNetworkCode = mnc;
        ArrayList<GoogleCellTower> towers = new ArrayList<>(1);
        GoogleCellTower bigTower = new GoogleCellTower();//the tower that your phone connecting. (Important)
        bigTower.cellId = cellId;
        bigTower.mobileCountryCode = mcc;
        bigTower.mobileNetworkCode = mnc;
        bigTower.locationAreaCode = lac;
        bigTower.signalStrength = 0;
        towers.add(bigTower);
        cellInfo.cellTowers = towers;

        // get all cell info that your phone can connect.
        @SuppressLint("MissingPermission") List<CellInfo> infos = mTelephonyManager.getAllCellInfo();
        if(infos!=null) {
            if(infos.size()==0)return cellInfo;
            towers = new ArrayList<>(infos.size());
            for (CellInfo i : infos) { // 根据邻区总数进行循环
                Log.i("Hint:", "The base station info nearby: " + i.toString());//这里如果出现很多cid
                GoogleCellTower tower = new GoogleCellTower();

                if(i instanceof CellInfoGsm){//信号塔的类型
                    if(((CellInfoGsm)i).isRegistered()==false){
                        continue;
                    }
                    Log.i("Hint:","GSM BASE STATION NOW.");
                    CellIdentityGsm cellIdentityGsm = ((CellInfoGsm)i).getCellIdentity();//从这个类里面可以取出好多有用的东西
                    if(cellIdentityGsm==null)continue;
                    tower.locationAreaCode = cellIdentityGsm.getLac();
                    tower.mobileCountryCode = cellIdentityGsm.getMcc();
                    tower.mobileNetworkCode = cellIdentityGsm.getMnc();
                    tower.signalStrength = ((CellSignalStrengthGsm)((CellInfoGsm)i).getCellSignalStrength()).getDbm();
                    tower.cellId = cellIdentityGsm.getCid();
                }else if(i instanceof CellInfoCdma){
                    if(((CellInfoCdma)i).isRegistered()==false){
                        continue;
                    }
                    Log.i("Hint:","CDMA BASE STATION NOW.");
                    CellIdentityCdma cellIdentityCdma = ((CellInfoCdma)i).getCellIdentity();
                    if(cellIdentityCdma==null)continue;
                    tower.locationAreaCode = lac;
                    tower.mobileCountryCode = mcc;
                    tower.mobileNetworkCode = cellIdentityCdma.getSystemId();//cdma用sid,是系统识别码，每个地级市只有一个sid，是唯一的。
                    tower.signalStrength = 0;
                    cellIdentityCdma.getNetworkId();//NID是网络识别码，由各本地网管理，也就是由地级分公司分配。每个地级市可能有1到3个nid。
                    tower.cellId = cellIdentityCdma.getBasestationId();//cdma用bid,表示的是网络中的某一个小区，可以理解为基站。
                }else if(i instanceof CellInfoLte) {
                    if(((CellInfoLte)i).isRegistered()==false){
                        continue;
                    }
                    Log.i("Hint:", "LTE BASE STATION NOW");
                    CellIdentityLte cellIdentityLte = ((CellInfoLte) i).getCellIdentity();
                    if(cellIdentityLte==null)continue;
                    tower.locationAreaCode = lac;
                    tower.mobileCountryCode = cellIdentityLte.getMcc();
                    tower.mobileNetworkCode = cellIdentityLte.getMnc();
                    tower.cellId = cellIdentityLte.getCi();
                    tower.signalStrength = ((CellSignalStrengthLte)((CellInfoLte)i).getCellSignalStrength()).getDbm();;
                }else if(i instanceof CellInfoWcdma && Build.VERSION.SDK_INT>=18){
                    if(((CellInfoWcdma)i).isRegistered()==false){
                        continue;
                    }
                    Log.i("Hint:","WCDMA BASE STATION NOW");
                    CellIdentityWcdma cellIdentityWcdma = ((CellInfoWcdma)i).getCellIdentity();
                    if(cellIdentityWcdma==null)continue;
                    tower.locationAreaCode = cellIdentityWcdma.getLac();
                    tower.mobileCountryCode = cellIdentityWcdma.getMcc();
                    tower.mobileNetworkCode = cellIdentityWcdma.getMnc();
                    tower.cellId = cellIdentityWcdma.getCid();
                    tower.signalStrength = 0;
                }else {
                    Log.i("Hint:","UNSURE BASE STATION..");
                }
                towers.add(tower);
            }
        }
        cellInfo.cellTowers = towers;
        return cellInfo;
    }


    /**
            * scan wifi
     */
    public static GeoLocationAPI getWIFIInfo(Context context, GeoLocationAPI geoLocationAPI){

        Log.i("Hint:", "Begin to get WIFIinfo");
        //get all the wifi that our phone can receive
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null)return geoLocationAPI;
        Log.i("Hint:","BEGIN TO SCAN WIFI");
        wifiManager.startScan();

        ArrayList<AlxScanWifi> lsAllWIFI = new ArrayList<AlxScanWifi>();
        List<ScanResult> lsScanResult = wifiManager.getScanResults();
        if(lsScanResult == null){
            Log.i("FAIL:","Fail to scan the wifi");
            return geoLocationAPI;
        }
        int MaxStrength = -100;
        for (ScanResult result : lsScanResult) {
            Log.i("Hint:","Find a wifi:"+result.SSID+"  MAC address:"+result.BSSID+"   Signal Strength:"+result.level+ "    Frequency:" + result.frequency);
            if(result == null)continue;
            AlxScanWifi scanWIFI = new AlxScanWifi(result);

            lsAllWIFI.add(scanWIFI);

        }

        ArrayList<GoogleWifiInfo> wifiInfos = new ArrayList<>(lsAllWIFI.size());
        for (AlxScanWifi w:lsAllWIFI){
            if(w == null)continue;
            GoogleWifiInfo wifiInfo = new GoogleWifiInfo();
            wifiInfo.macAddress = w.mac.toUpperCase();
            wifiInfo.signalStrength = w.dBm;
            wifiInfo.channel = abs(w.channel);
            wifiInfos.add(wifiInfo);
        }
        geoLocationAPI.wifiAccessPoints = wifiInfos;
        return geoLocationAPI;

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {



    }
}
