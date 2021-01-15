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
    static double sumoRate = 0.03;
    static double ryoRate = 0.03;
    static double intenseRate = 0.00078;
    static double masariRate = 0.03;
    static double lokiRate = 0.10;
    static double sumoChange7d = 0.0;
    static double ryoChange7d = 0.0;
    static double intenseChange7d = 0.0;
    static double masariChange7d = 0.0;
    static double lokiChange7d = 0.0;

    Prices() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), 0, 3600000);
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            updateRates();
        }
    }

    private void updateRates() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest" +
                "?symbol=SUMO,RYO,LTHN,MSR,LOKI&convert=EUR&CMC_PRO_API_KEY=" + x.configItem.getCmcApiKey()).newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONObject jsonObjectSumo = jsonObject.getJSONObject("data").getJSONObject("SUMO").getJSONObject("quote").getJSONObject("EUR");
            JSONObject jsonObjectRyo = jsonObject.getJSONObject("data").getJSONObject("RYO").getJSONObject("quote").getJSONObject("EUR");
            JSONObject jsonObjectIntense = jsonObject.getJSONObject("data").getJSONObject("LTHN").getJSONObject("quote").getJSONObject("EUR");
            JSONObject jsonObjectMasari = jsonObject.getJSONObject("data").getJSONObject("MSR").getJSONObject("quote").getJSONObject("EUR");
            JSONObject jsonObjectLoki = jsonObject.getJSONObject("data").getJSONObject("LOKI").getJSONObject("quote").getJSONObject("EUR");

            sumoRate = jsonObjectSumo.getDouble("price");
            sumoChange7d = jsonObjectSumo.getDouble("percent_change_7d");
            ryoRate = jsonObjectRyo.getDouble("price");
            ryoChange7d = jsonObjectRyo.getDouble("percent_change_7d");
            intenseRate = jsonObjectIntense.getDouble("price");
            intenseChange7d = jsonObjectIntense.getDouble("percent_change_7d");
            masariRate = jsonObjectMasari.getDouble("price");
            masariChange7d = jsonObjectMasari.getDouble("percent_change_7d");
            lokiRate = jsonObjectLoki.getDouble("price");
            lokiChange7d = jsonObjectLoki.getDouble("percent_change_7d");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Sumo rate updated " + sumoRate + " change7d " + sumoChange7d);
        System.out.println(getTime() + "Ryo rate updated " + ryoRate + " change7d " + ryoChange7d);
        System.out.println(getTime() + "Lethean rate updated " + intenseRate + " change7d " + intenseChange7d);
        System.out.println(getTime() + "Masari rate updated " + masariRate + " change7d " + masariChange7d);
        System.out.println(getTime() + "Loki rate updated " + lokiRate + " change7d " + lokiChange7d);
    }

    public static double getClaimAmount(int claimsToday, String currency) {
        double amount = 0;
        if (currency.equals("sumo")) {//////////////////////////////////////////////
            amount = randomAmount(sumoRate);

            amount = amount * 0.7;

            amount = removeClaimsAmount(amount, claimsToday);

            amount = amount * 0.6;
            amount = amount * 0.75;
            amount = amount * 0.73; //27-05-2018; -1,19 totaal; 0,828%
            amount = amount * 0.93; //12-08-2018
            amount = amount * 0.6; //23-09-2018
            amount = amount * 0.9;
            amount = amount * 0.8;

            amount = removeRateChangeAmount(sumoChange7d, amount);
        } else if (currency.equals("ryo")) {//////////////////////////////////////////////
            amount = randomAmount(ryoRate);

            amount = amount * 0.7;

            amount = removeClaimsAmount(amount, claimsToday);

            amount = amount * 0.6;
            amount = amount * 0.75;
            amount = amount * 0.73; //27-05-2018; -1,19 totaal; 0,828%
            amount = amount * 0.93; //16-09-2018
            amount = amount * 0.6; //23-09-2018

            amount = removeRateChangeAmount(ryoChange7d, amount);
        } else if (currency.equals("intense")) {//////////////////////////////////////////////
            amount = randomAmount(intenseRate);

            amount = amount * 0.7;

            amount = removeClaimsAmount(amount, claimsToday);

            amount = amount * 0.6;
            amount = amount * 0.6; //23-09-2018
            amount = amount * 0.65; //25-09-2018
            amount = amount * 0.70;

            amount = removeRateChangeAmount(intenseChange7d, amount);
        } else if (currency.equals("masari")) {//////////////////////////////////////////////
            amount = randomAmount(masariRate);

            amount = amount * 0.7;

            amount = removeClaimsAmount(amount, claimsToday);

            amount = amount * 0.9;
            amount = amount * 0.6; //23-09-2018
            amount = amount * 0.80;
            amount = amount * 0.77;

            amount = removeRateChangeAmount(masariChange7d, amount);
        } else if (currency.equals("loki")) {//////////////////////////////////////////////
            amount = randomAmount(lokiRate);
            amount = amount * 0.7;
            amount = removeClaimsAmount(amount, claimsToday);
            amount = amount * 0.78;
            amount = amount * 0.6;
            amount = amount * 0.77;
            amount = amount * 0.85;

            amount = removeRateChangeAmount(lokiChange7d, amount);
        }
        amount = amount * 0.90;
        if (claimsToday == 1) {
            amount = amount * 3;
        }
        amount = amount * x.configItem.getClaimPercentage();
        return WithdrawHandler.round(amount, 5);
    }

    public static double randomAmount(double rate) {
        double amount;
        Random rand = new Random();
        int value = rand.nextInt(100) + 1;
        if (value <= 50) {
            amount = 0.00035 / rate;
        } else if (value <= 91) {
            amount = 0.00075 / rate;
        } else if (value <= 98) {
            amount = 0.013 / rate;
        } else {
            amount = 0.028 / rate;
        }
        return amount;
    }

    public static double removeRateChangeAmount(double change7d, double amount) {
        if (change7d <= -40.0) {
            amount = amount * 0.5;
        } else if (change7d <= -30) {
            amount = amount * 0.6;
        } else if (change7d <= -20) {
            amount = amount * 0.7;
        } else if (change7d <= -10) {
            amount = amount * 0.85;
        }
        return amount;
    }

    public static double removeClaimsAmount(double amount, int claimsToday) {
        if (claimsToday >= 6 && claimsToday <= 11) {
            amount = amount * 0.78;
        } else if (claimsToday >= 16 && claimsToday <= 20) {
            amount = amount * 0.58;
        } else if (claimsToday >= 21 && claimsToday <= 26) {
            amount = amount * 0.48;
        } else if (claimsToday >= 27 && claimsToday <= 35) {
            amount = amount * 0.32;
        } else if (claimsToday >= 36 && claimsToday <= 45) {
            amount = amount * 0.18;
        } else if (claimsToday >= 46 && claimsToday <= 100) {
            amount = amount * 0.1;
        }
        return amount;
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }

}
