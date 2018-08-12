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
    static Double intenseRate = 0.00000039;

    Prices() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), 0, 1800000);
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            updateSumoRate();
            updateRyoRate();
            updateIntenseRate();
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
    private void updateIntenseRate() {

        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://tradeogre.com/api/v1/ticker/BTC-ITNS").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            intenseRate = jsonObject.getDouble("price");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Intense rate updated " + intenseRate);
    }

    public static double getClaimAmount(int claimsToday, String currency) {
        double amount = 0;
        if (currency.equals("sumo")) {
            Random rand = new Random();
            int value = rand.nextInt(100) + 1;
            amount = 0.00000015 / Prices.sumoRate;
            if (value <= 50) {
                amount = 0.000000075 / Prices.sumoRate;
            } else if (value <= 90) {
                amount = 0.00000015 / Prices.sumoRate;
            } else if (value <= 94) {
                amount = 0.00000250 / Prices.sumoRate;
            } else if (value <= 97) {
                amount = 0.00000540 / Prices.sumoRate;
            }

            amount = amount * 0.7;

            if (claimsToday >= 10 && claimsToday <= 15) {
                amount = amount * 0.80;
            } else if (claimsToday >= 16 && claimsToday <= 20) {
                amount = amount * 0.60;
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
            amount = amount * 0.95;
            amount = amount * 0.95; //12-08-2018

            if (claimsToday == 1) {
                amount = amount * 3;
            }
        } else if (currency.equals("ryo")){
            Random rand = new Random();
            int value = rand.nextInt(100) + 1;
            amount = 0.00000020 / Prices.ryoRate;
            if (value <= 50) {
                amount = 0.000000075 / Prices.ryoRate;
            } else if (value <= 90) {
                amount = 0.00000015 / Prices.ryoRate;
            } else if (value <= 94) {
                amount = 0.00000250 / Prices.ryoRate;
            } else if (value <= 97) {
                amount = 0.00000540 / Prices.ryoRate;
            }

            amount = amount * 0.7;

            if (claimsToday >= 10 && claimsToday <= 15) {
                amount = amount * 0.80;
            } else if (claimsToday >= 16 && claimsToday <= 20) {
                amount = amount * 0.60;
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
            amount = amount * 0.95;
            amount = amount * 0.95; //12-08-2018


            if (claimsToday == 1) {
                amount = amount * 3;
            }
        } else if (currency.equals("intense")){
            Random rand = new Random();
            int value = rand.nextInt(100) + 1;
            amount = 0.00000020 / Prices.intenseRate;
            if (value <= 50) {
                amount = 0.00000007 / Prices.intenseRate;
            } else if (value <= 90) {
                amount = 0.00000015 / Prices.intenseRate;
            } else if (value <= 94) {
                amount = 0.00000270 / Prices.intenseRate;
            } else if (value <= 97) {
                amount = 0.00000590 / Prices.intenseRate;
            }

            amount = amount * 0.7;

            if (claimsToday >= 10 && claimsToday <= 15) {
                amount = amount * 0.80;
            } else if (claimsToday >= 16 && claimsToday <= 20) {
                amount = amount * 0.60;
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
            amount = amount * 0.95;
            amount = amount * 0.95; //12-08-2018

            if (claimsToday == 1) {
                amount = amount * 3;
            }
        }

        return WithdrawHandler.round(amount, 5);
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }

}
