package com.naver.maps.map.sdktest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GetData {

    private static String NAVER_CLIENT_ID = "j4db6gdfgj";
    private static String NAVER_CLIENT_SECRET = "DsnDx3loKKIaStGY33VWsbt0WuKnhTRbhpuQgr2R";
    private static String NAVER_REVERSE_GEOCODING_REQUEST_URL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?";
//    private static String NAVER_DIRECTIONS_15_REQUEST_URL = "https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/driving?"; // driver 15 api
    private static String NAVER_DIRECTIONS_5_REQUEST_URL = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?"; // driver 5 api

    private static String NAVER_WEATHER_REQUEST_URL = "https://weather.naver.com/today/";

    private static String AIRMAP_ELEVATION_REQUEST_URL = "https://api.airmap.com/elevation/v1/ele/?points=";

    // get current location address from naver reverse geocoding api
    public static class GetAddressFromNaver extends AsyncTask<Void, Void, String> {

        Double latitude, longitude;
        public GetAddressFromNaver(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = null;

            try {
                String lat = Double.toString(latitude);
                String log = Double.toString(longitude);
                String latlng = log + "," + lat;
                String apiURL = NAVER_REVERSE_GEOCODING_REQUEST_URL +
                        "request=coordsToaddr&" +
                        "coords=" + latlng + "&" +
                        "sourcecrs=epsg:4326&" +
                        "orders=admcode,legalcode,addr,roadaddr&" +
                        "output=json";
                URL url = new URL(apiURL);

                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", NAVER_CLIENT_ID);
                con.setRequestProperty("X-NCP-APIGW-API-KEY", NAVER_CLIENT_SECRET);
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    Log.d("Error Code", String.valueOf(responseCode));
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                result = response.toString();
            } catch (ConnectException e) {
                Log.e("Error", "ConnectException");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String jsonObject) {
            super.onPostExecute(jsonObject);
        }
    }

    // convert json object to string address
    public static String getJsonToAddr(JSONObject jsonObject) throws JSONException {
        JSONArray results = jsonObject.getJSONArray("results");
        JSONObject roadArr = results.getJSONObject(results.length() - 1);
        JSONObject region = roadArr.getJSONObject("region");
        JSONObject land = roadArr.getJSONObject("land");

        String area1Name = region.getJSONObject("area1").getString("name");
        String area2Name = region.getJSONObject("area2").getString("name");
        String area3Name = region.getJSONObject("area3").getString("name");
        String area4Name = region.getJSONObject("area4").getString("name");

        String frontAddr = "";
        if (area4Name.equals("")) { frontAddr = area1Name + " " + area2Name + " " + area3Name; }
        else { frontAddr = area1Name + " " + area2Name + " " + area3Name + " " + area4Name; }

        String name = "";
        try {
            name = land.getString("name"); // 상세 명칭
        } catch (JSONException e) {
            name = "";
        }

        String num1 = land.getString("number1"); // 토지 본번호
        String num2 = land.getString("number2"); // 토지 부번호
        String building = "", backAddr = "";

        for (int i = 0; i <= 4; i++) {
            JSONObject obj = land.getJSONObject("addition" + i);
            if (obj.getString("type").equals("building")) {
                building = obj.getString("value");
            }
        }

        if (name.equals("")) { backAddr = num2.equals("") ? num1 + " " + building : num1 + " " + num2 + " " + building; }
        else { backAddr = num2.equals("") ? name + " " + num1 + " " + building : name + " " + num1 + " " + num2 + " " + building; }

        return frontAddr + " " + backAddr;
    }

    // get location code from api result (json object)
    public static String getLocationCode(JSONObject jsonObject) throws JSONException {
        JSONArray results = jsonObject.getJSONArray("results");
        JSONObject admcode = results.getJSONObject(0);
        JSONObject code = admcode.getJSONObject("code");
        String codeStr = code.getString("mappingId");

        return codeStr;
    }

    // get weather info & sun set/rise info from naver weather homepage
    public static class GetLocationWeather extends AsyncTask<Void, Void, String> {

        String weatherSelector = "#content > div > div.card.card_today > div.today_weather > div.weather_area > p > span.weather.before_slash";
        String svgSelector = "#content > div > div.card.card_today > div.today_weather > i";
        String temperatureSelector = "#content > div > div.card.card_today > div.today_weather > div.weather_area > strong.current";
        String sunRiseSelectorAtDay = "#sunRiseSet > div > div > div > div.sun_info > dl > dd:nth-child(2)";
        String sunSetSelectorAtDay = "#sunRiseSet > div > div > div > div.sun_info > dl > dd:nth-child(4)";
        String sunRiseSelectorAtNight = "#sunRiseSet > div > div > table > tbody > tr:nth-child(1) > td:nth-child(2) > .sun_time";
        String sunSetSelectorAtNight = "#sunRiseSet > div > div > table > tbody > tr:nth-child(1) > td:nth-child(3) > .sun_time";

        String svgUrl = "https://ssl.pstatic.net/static/weather/image/icon_weather/";

        Context context;
        String locationCode;
        public GetLocationWeather(Context context, String locationCode) {
            this.context = context;
            this.locationCode = locationCode;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String returnStr = "";

            try {
                Document document = Jsoup.connect(NAVER_WEATHER_REQUEST_URL + locationCode).get();
                Elements weatherEl = document.select(weatherSelector);
                Elements svgEl = document.select(svgSelector);
                Elements temperatureEl = document.select(temperatureSelector);

                Elements sunRiseEl = null;
                Elements sunSetEl = null;

                try {
                    sunRiseEl = document.select(sunRiseSelectorAtDay);
                    sunSetEl = document.select(sunSetSelectorAtDay);
                } catch (Exception e) {}

                // naver weather page at night
                if (sunRiseEl.toString().equals("") && sunSetEl.toString().equals("")) {
                    sunRiseEl = document.select(sunRiseSelectorAtNight);
                    sunSetEl = document.select(sunSetSelectorAtNight);
                }

                svgUrl = svgUrl + svgEl.attr("data-ico") + ".svg";
                String weather = weatherEl.text();
                String temperature = temperatureEl.text().replace("현재 온도", "");
                String sunRiseTime = sunRiseEl.text();
                String sunSetTime = sunSetEl.text();

                returnStr = weather + "`" + temperature + "`" + sunRiseTime + "`" + sunSetTime + "`" + svgUrl;

            } catch (IOException e) {
                e.printStackTrace();
                returnStr = "`날씨정보 없음";
            }

            return returnStr;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    // get distance json object from naver direction 15 api
    public static class GetDirection extends AsyncTask<Void, Void, String> {

        String beforeLocation, currentLocation;
        public GetDirection(String beforeLocation, String currentLocation) {
            this.beforeLocation = beforeLocation;
            this.currentLocation = currentLocation;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = null;

            try {
                String apiURL = NAVER_DIRECTIONS_5_REQUEST_URL +
                        "start=" + beforeLocation + "&" +
                        "goal=" + currentLocation + "&" +
                        "option=trafast";
                URL url = new URL(apiURL);

                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", NAVER_CLIENT_ID);
                con.setRequestProperty("X-NCP-APIGW-API-KEY", NAVER_CLIENT_SECRET);
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    Log.d("Error Code", String.valueOf(responseCode));
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                result = response.toString();
            } catch (ConnectException e) {
                Log.e("Error", "ConnectException");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String jsonObject) {
            super.onPostExecute(jsonObject);
        }
    }

    // get string distance info from distance json object
    public static String getJsonToDistance(JSONObject jsonObject) throws JSONException {
        JSONObject route = jsonObject.getJSONObject("route");
        JSONArray trafast = route.getJSONArray("trafast");
        JSONObject summary = trafast.getJSONObject(0).getJSONObject("summary");

        String currentDateTime = jsonObject.getString("currentDateTime"); // 2020-07-02T14:45:34
        int distance = Integer.parseInt(summary.getString("distance")); // meters

        return currentDateTime + " " + distance;
    }

    // get elevation info from airmap api
    public static class GetElevationFromAPI extends AsyncTask<Void, Void, String> {

        String currentCoord = "";
        public GetElevationFromAPI(String currentCoord) {
            this.currentCoord = currentCoord;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = "";
            try {
                String line, newjson = "";
                URL urls = new URL(AIRMAP_ELEVATION_REQUEST_URL + currentCoord);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(urls.openStream(), "UTF-8"))) {
                    while ((line = reader.readLine()) != null) {
                        newjson += line;
                    }
                    JSONObject jsonObject = new JSONObject(newjson);
                    result = jsonObject.getJSONArray("data").getString(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}
