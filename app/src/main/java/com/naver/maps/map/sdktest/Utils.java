package com.naver.maps.map.sdktest;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static void displayMessage(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT);
    }

    public static JSONObject dataToJson(LocationVO locationVO) {
        String JSON_STRING = "{\"results\":{\"Date\":\"\",\"Time\":\"\",\"Weather\":\"\",\"RoadSlope\":\"\",\"GPSCoordinate\":{\"latitude\":\"\",\"longitude\":\"\"},\"Address\":\"\",\"DrivingSpeed\":\"\"}}";
        JSONObject results = null;

        LocationVO locationVOEng = korToEng(locationVO);

        try {
            JSONObject jsonObject = new JSONObject(JSON_STRING);
            results = jsonObject.getJSONObject("results");
            String date = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date());

            results.put("Date", date);
            results.put("Time", locationVOEng.getTime());
            results.put("Weather", locationVOEng.getWeather());
            results.put("RoadSlope", locationVOEng.getSlope());
            results.getJSONObject("GPSCoordinate").put("latitude", locationVOEng.getCoord().latitude);
            results.getJSONObject("GPSCoordinate").put("longitude", locationVOEng.getCoord().longitude);
            results.put("Address", locationVOEng.getAddress());
            results.put("DrivingSpeed", locationVOEng.getSpeed());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return results;
    }

    private static LocationVO korToEng(LocationVO locationVO) {
        String time, weather, slope, speed;

        switch (locationVO.getTime()) {
            case "주간":
                time = "Day";
                break;
            case "야간":
                time = "Night";
                break;
            default:
                time = "";
                break;
        }

        switch (locationVO.getWeather()) {
            case "맑음":
                weather = "Sunny";
                break;
            case "구름많음":
                weather = "Cloudy";
                break;
            case "비":
                weather = "Rain";
                break;
            case "눈":
                weather = "Snow";
                break;
            case "안개":
                weather = "Fog";
                break;
            default:
                weather = "";
                break;
        }

        switch (locationVO.getSlope()) {
            case "평지":
                slope = "Flat";
                break;
            case "오르막길":
                slope = "Uphill";
                break;
            case "내리막길":
                slope = "Downhill";
                break;
            default:
                slope = "";
                break;
        }

        switch (locationVO.getSpeed()) {
            case "고속":
                speed = "High";
                break;
            case "저속":
                speed = "MidLow";
                break;
            default:
                speed = "";
                break;
        }

        LocationVO locationVOEng = new LocationVO();
        locationVOEng.setTime(time);
        locationVOEng.setWeather(weather);
        locationVOEng.setCoord(locationVO.getCoord());
        locationVOEng.setAddress(locationVO.getAddress());
        locationVOEng.setSlope(slope);
        locationVOEng.setSpeed(speed);

        return locationVOEng;
    }

    public static void saveJson(JSONObject jsonObject) {
        Log.d("result", jsonObject.toString());

        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String currentTime = new SimpleDateFormat("HHmmss").format(new Date());
        String filename = currentDate + "_" + currentTime + ".json";

        try {
            String save_root = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/" + Environment.DIRECTORY_DOCUMENTS
                    + "/" + currentDate;
            File file = new File(save_root);
            if (!file.exists()) { file.mkdir(); }

            FileWriter fileWriter = new FileWriter(file.getAbsolutePath() + "/" + filename);
            fileWriter.write(jsonObject.toString(4));
            fileWriter.flush();
            fileWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}