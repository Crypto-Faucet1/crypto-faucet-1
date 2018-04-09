import com.google.common.base.Charsets;
import com.google.common.io.Files;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClaimHandler {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
    public static Double sumoRate = 0.00013826;
    public static Double bitcoinRate = 0.0;

    ClaimHandler() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), 0, 1800000);
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            updateRate();
            updateBitcoinRate();
        }
    }

    String claimSumo(Request request, Response response) {
        String captcha = request.queryParams("captcha");
        String address = request.queryParams("address").replaceAll("\\s+", "");
        String ip = request.queryParams("ip");
        return claim(address, captcha, ip) + "";
    }

    private boolean claim(String address, String captcha, String ip) {
        boolean comp = true;

        if (checkCaptcha(captcha, ip) && !address.equals("")) {
            String result = "[]";
            double balance = 0.0;
            double totalPaid = 0.0;
            long lastClaim = 0;
            double dailyBonus = 0;
            long dailyLastClaim = 0;
            int claims = 0;
            int claimsToday = 0;
            int lastClaimDay = 0;
            int lastBonusDay = 0;
            int num = -1;
            try {
                File file1 = new File("addresses.json");
                result = Files.asCharSource(file1, Charsets.UTF_8).read();
            } catch (Exception e) {
                System.out.println(getTime() + " Failed to read addresses file.");
            }
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject addr = jsonArray.getJSONObject(i);
                if (addr.getString("address").equals(address)) {
                    try {

                        balance = addr.getDouble("balance");
                        lastClaim = addr.getLong("lastClaim");
                        dailyBonus = addr.getDouble("dailyBonus");
                        dailyLastClaim = addr.getLong("dailyLastClaim");
                        claims = addr.getInt("claims");
                        num = i;
                    } catch (Exception e) {
                        System.out.println(getTime() + " New address added");
                    }
                    try {
                        totalPaid = addr.getDouble("totalPaid");
                    } catch (Exception ignored) {
                    }
                    try {
                        claimsToday = addr.getInt("claimsToday");
                        lastClaimDay = addr.getInt("lastClaimDay");
                    } catch (Exception ignored) {
                    }
                    try {
                        lastBonusDay = addr.getInt("lastBonusDay");
                    } catch (Exception ignored) {

                    }
                }
            }
            Calendar cal = Calendar.getInstance();
            int day = cal.get(Calendar.DAY_OF_MONTH);

            double claimAmount = 0;
            Date date = new Date();
            long dif = date.getTime() - lastClaim;
            if (dif >= 300000) {
                if(dailyLastClaim == 0){
                    dailyLastClaim = date.getTime();
                }
                long dif2 = date.getTime() - dailyLastClaim;
                if (dif2 >= 43200000 && dif2 <= 172800000 && lastBonusDay != day) {
                    if (dailyBonus < 0.99) {
                        dailyBonus = dailyBonus + 0.01;
                        lastBonusDay = day;
                        dailyLastClaim = date.getTime();
                    }
                } else if (dif2 >= 172800000 || dif2 < -1) {
                    dailyBonus = 0;
                    dailyLastClaim = date.getTime();
                }

                if (day == lastClaimDay) {
                    claimsToday = claimsToday + 1;
                    lastClaimDay = day;
                } else {
                    claimsToday = 1;
                    lastClaimDay = day;
                }

                double keerDing = 1 + dailyBonus;
                claimAmount = getClaimAmount(claimsToday);

                balance = balance + claimAmount * keerDing;
                claims = claims + 1;
                lastClaim = date.getTime();
                if (num != -1) {
                    jsonArray.remove(num);
                }

                JSONObject newItem = new JSONObject();
                newItem.put("address", address);
                newItem.put("balance", balance);
                newItem.put("lastClaim", lastClaim);
                newItem.put("dailyLastClaim", dailyLastClaim);
                newItem.put("dailyBonus", dailyBonus);
                newItem.put("claims", claims);
                newItem.put("totalPaid", totalPaid);
                newItem.put("lastClaimDay", lastClaimDay);
                newItem.put("claimsToday", claimsToday);
                newItem.put("lastBonusDay", lastBonusDay);
                jsonArray.put(newItem);
            }

            try {
                File fileS = new File("addresses.json");
                Files.asCharSink(fileS, Charsets.UTF_8).write(jsonArray.toString());
                System.out.println(getTime() + " Faucet claim successful balance: " + WithdrawHandler.round(balance, 5) + " Claimed:" + claimAmount + " Ip: " + ip + " Address: " + address);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(getTime() + " Failed to write addresses file.");
                comp = false;
            }
        } else {
            comp = false;
        }

        return comp;
    }

    private boolean checkCaptcha(String captcha, String ip) {
        boolean comp = true;
        ///recaptcha
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.google.com/recaptcha/api/siteverify").newBuilder();
        urlBuilder.addQueryParameter("secret", "6LfWYTwUAAAAAFcjH1RNrVV8WkW8rSy3nwzPSVZn");
        urlBuilder.addQueryParameter("response", captcha);
        urlBuilder.addQueryParameter("remoteip", ip);
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            comp = jsonObject.getBoolean("success");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ///Double ip check
        String result = "[]";
        try {
            File file1 = new File("ip.json");
            result = Files.asCharSource(file1, Charsets.UTF_8).read();
        } catch (Exception e) {
            System.out.println(getTime() + " Failed to read ip file.");
        }
        int num = -1;
        long lastClaim = 0;
        JSONArray jsonArray = new JSONArray(result);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject addr = jsonArray.getJSONObject(i);
            try {
                if (addr.getString("ip").equals(ip)) {
                    lastClaim = addr.getLong("lastClaim");
                    num = i;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                //System.out.println(getTime() + " New ip added " + ip);
            }
        }
        Date date = new Date();
        long dif = date.getTime() - lastClaim;
        if (dif >= 300000) {
            if (num != -1) {
                jsonArray.remove(num);
            }
            lastClaim = date.getTime();
            JSONObject newItem = new JSONObject();
            newItem.put("ip", ip);
            newItem.put("lastClaim", lastClaim);
            jsonArray.put(newItem);
        } else {
            comp = false;
            System.out.println(getTime() + "Ip double claim " + ip);
        }

        try {
            File fileS = new File("ip.json");
            Files.asCharSink(fileS, Charsets.UTF_8).write(jsonArray.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(getTime() + " Failed to write ip file.");
            comp = false;
        }

        return comp;
    }

    private double getClaimAmount(int claimsToday) {
        Random rand = new Random();
        int value = rand.nextInt(100) + 1;
        double amount = 0.00000020 / sumoRate;
        if (value <= 50) {
            amount = 0.00000006 / sumoRate;
        } else if (value <= 90) {
            amount = 0.00000015 / sumoRate;
        } else if (value <= 94) {
            amount = 0.00000280 / sumoRate;
        } else if (value <= 97) {
            amount = 0.00000638 / sumoRate;
        }
        if (bitcoinRate <= 11000) {
            amount = amount * 0.7;
        } else if (bitcoinRate <= 13000) {
            amount = amount * 0.81;
        } else if (bitcoinRate <= 14000) {
            amount = amount * 0.85;
        } else if (bitcoinRate <= 15000) {
            amount = amount * 0.9;
        } else if (bitcoinRate <= 16000) {

        } else if (bitcoinRate <= 20000) {
            amount = amount * 1.05;
        }

        if (claimsToday >= 10 && claimsToday <= 15) {
            amount = amount * 0.86;
        } else if (claimsToday >= 16 && claimsToday <= 20) {
            amount = amount * 0.79;
        } else if (claimsToday >= 21 && claimsToday <= 26) {
            amount = amount * 0.70;
        } else if (claimsToday >= 27 && claimsToday <= 35) {
            amount = amount * 0.45;
        } else if (claimsToday >= 36 && claimsToday <= 45) {
            amount = amount * 0.3;
        } else if (claimsToday >= 46 && claimsToday <= 100) {
            amount = amount * 0.2;
        }

        amount = amount * 0.89;
        amount = amount * 0.6;
        amount = amount * 0.75;

        if(claimsToday == 1){
            amount = amount * 3;
        }

        return WithdrawHandler.round(amount, 5);
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }

    private void updateRate() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.coinmarketcap.com/v1/ticker/sumokoin").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONArray jsonArray = new JSONArray(response.body().string());
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            sumoRate = jsonObject.getDouble("price_btc");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Sumo rate updated " + sumoRate);
    }

    private void updateBitcoinRate() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.coinmarketcap.com/v1/ticker/bitcoin").newBuilder();
        String url = urlBuilder.build().toString();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            JSONArray jsonArray = new JSONArray(response.body().string());
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            bitcoinRate = jsonObject.getDouble("price_usd");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(getTime() + "Bitcoin rate updated " + bitcoinRate);
    }
}
