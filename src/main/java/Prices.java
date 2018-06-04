import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Prices {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
    static Double sumoRate = 0.00013826;
    static Double ryoRate = 0.0001000;


    Prices() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), 0, 1800000);
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            updateSumoRate();
            updateRyoRate();
        }
    }

    private void updateSumoRate() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.coinmarketcap.com/v2/ticker/1694/?convert=BTC").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONObject jsonObjectBtc = jsonObject.getJSONObject("data").getJSONObject("quotes").getJSONObject("BTC");
            sumoRate = jsonObjectBtc.getDouble("price");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Sumo rate updated " + sumoRate);
    }

    private void updateRyoRate() {

        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://tradeogre.com/api/v1/ticker/BTC-RYO").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            ryoRate = jsonObject.getDouble("price");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Ryo rate updated " + ryoRate);
    }

    public static double getClaimAmount(int claimsToday) {
        Random rand = new Random();
        int value = rand.nextInt(100) + 1;
        double amount = 0.00000020 / Prices.sumoRate;
        if (value <= 50) {
            amount = 0.00000006 / Prices.sumoRate;
        } else if (value <= 90) {
            amount = 0.00000015 / Prices.sumoRate;
        } else if (value <= 94) {
            amount = 0.00000270 / Prices.sumoRate;
        } else if (value <= 97) {
            amount = 0.00000600 / Prices.sumoRate;
        }

        amount = amount * 0.7;

        if (claimsToday >= 10 && claimsToday <= 15) {
            amount = amount * 0.80;
        } else if (claimsToday >= 16 && claimsToday <= 20) {
            amount = amount * 0.65;
        } else if (claimsToday >= 21 && claimsToday <= 26) {
            amount = amount * 0.48;
        } else if (claimsToday >= 27 && claimsToday <= 35) {
            amount = amount * 0.32;
        } else if (claimsToday >= 36 && claimsToday <= 45) {
            amount = amount * 0.18;
        } else if (claimsToday >= 46 && claimsToday <= 100) {
            amount = amount * 0.1;
        }

        amount = amount * 0.89;
        amount = amount * 0.6;
        amount = amount * 0.75;
        amount = amount * 0.92; //21-04-2018; -0,45 totaal ; 0,87%
        amount = amount * 0.73; //27-05-2018; -1,19 totaal; 0,828%

        if (claimsToday == 1) {
            amount = amount * 3;
        }

        return WithdrawHandler.round(amount, 5);
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }

}
