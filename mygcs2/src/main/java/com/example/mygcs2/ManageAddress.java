package com.example.mygcs2;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.naver.maps.geometry.LatLng;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ManageAddress extends AsyncTask<LatLng, String, String> {
    private StringBuilder mUrlBuilder;
    private URL mUrl;
    private HttpURLConnection mConn;
    private Callback m_callback;

    interface Callback { // 인터페이스는 외부에 구현해도 상관 없습니다.
        void callbackMethod(String str);
    }

    public ManageAddress() {
        m_callback = null;
    }

    public void setCallback(Callback callback) {
        this.m_callback = callback;
    }


    @Override
    protected String doInBackground(LatLng... latLngs) {
        String strCoord = String.valueOf(latLngs[0].longitude) + "," + String.valueOf(latLngs[0].latitude);
        StringBuilder sb = new StringBuilder();

        mUrlBuilder = new StringBuilder("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords=" +strCoord+ "&sourcecrs=epsg:4326&output=json&orders=addr"); /* URL */
        try {
            mUrl = new URL(mUrlBuilder.toString());

            mConn = (HttpURLConnection) mUrl.openConnection();
            mConn.setRequestMethod("GET");
            mConn.setRequestProperty("Content-type", "application/json");
            mConn.setRequestProperty("X-NCP-APIGW-API-KEY-ID","d6nvpc15uv");
            mConn.setRequestProperty("X-NCP-APIGW-API-KEY","fzGdGvib2YvAL8I3KAnS4PGCubwhLNsq8yHIO8o5");

            BufferedReader rd;
            if(mConn.getResponseCode() >= 200 && mConn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(mConn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(mConn.getErrorStream()));
            }
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            mConn.disconnect();

        } catch (Exception e) {
            return null;
        }
        return sb.toString();
    }

    @Override
    protected void onPostExecute(String jsonStr) {
        super.onPostExecute(jsonStr);

        String pnu = getCurrentLocateion(jsonStr);
        if(m_callback != null) {
            m_callback.callbackMethod(pnu);
        }
    }

    private String getCurrentLocateion(String jsonStr){
        JsonParser jsonParser = new JsonParser();
        String currentLocation;
        JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        JsonArray jsonArray2 = (JsonArray) jsonObj.get("results");
        jsonObj = (JsonObject) jsonArray2.get(0);
        jsonObj = (JsonObject) jsonObj.get("region");
        jsonObj = (JsonObject) jsonObj.get("area1");
        currentLocation = jsonObj.get("name").getAsString();

        jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        jsonObj = (JsonObject) jsonArray2.get(0);
        jsonObj = (JsonObject) jsonObj.get("region");
        jsonObj = (JsonObject) jsonObj.get("area2");
        currentLocation = currentLocation.concat(" " + jsonObj.get("name").getAsString());

        jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        jsonObj = (JsonObject) jsonArray2.get(0);
        jsonObj = (JsonObject) jsonObj.get("region");
        jsonObj = (JsonObject) jsonObj.get("area3");
        currentLocation = currentLocation.concat(" " + jsonObj.get("name").getAsString());

        Log.d("myLog","[getCurrentLocation]" + currentLocation);
        return currentLocation;
    }

    private String getPnu(String jsonStr) {
        JsonParser jsonParser = new JsonParser();

        JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        JsonArray jsonArray = (JsonArray) jsonObj.get("results");
        jsonObj = (JsonObject) jsonArray.get(0);
        jsonObj = (JsonObject) jsonObj.get("code");
        String pnu = jsonObj.get("id").getAsString();

        jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        jsonArray = (JsonArray) jsonObj.get("results");
        jsonObj = (JsonObject) jsonArray.get(0);
        jsonObj = (JsonObject) jsonObj.get("land");
        pnu = pnu + jsonObj.get("type").getAsString();
        String number1 = jsonObj.get("number1").getAsString();
        String number2 = jsonObj.get("number2").getAsString();
        pnu = pnu + makeStringNum(number1) + makeStringNum(number2);
        return pnu;
    }

    private String makeStringNum(String number) {
        String strNum="";
        for (int i=0; i<4-number.length(); i++) {
            strNum = strNum + "0";
        }
        strNum=strNum+number;
        return strNum;
    }


}
