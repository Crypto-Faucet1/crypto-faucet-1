import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class ClaimHandler {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
    public static Connection conn;
    private JSONArray jsonArrayIpSumo = new JSONArray();
    private JSONArray jsonArrayIpRyo = new JSONArray();
    private JSONArray jsonArrayIpIntense = new JSONArray();
    private JSONArray jsonArrayIpMasari = new JSONArray();
    private JSONArray jsonArrayIpLoki = new JSONArray();

    ClaimHandler(ConfigItem configItem) {
        try {
            conn = DriverManager.getConnection("jdbc:mariadb://" + configItem.getMysqlHost() + "/" + configItem.getMysqlDb(),
                    configItem.getMysqlUsername(), configItem.getMysqlPassword());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Timer updateTimer2 = new Timer();
        updateTimer2.scheduleAtFixedRate(new UpdateTask2(), 0, 6000);
    }

    private class UpdateTask2 extends TimerTask {
        @Override
        public void run() {
            try {
                if (!conn.isValid(2500)) {
                    conn.close();
                    conn = DriverManager.getConnection("jdbc:mariadb://" + x.configItem.getMysqlHost() + "/" + x.configItem.getMysqlDb(),
                            x.configItem.getMysqlUsername(), x.configItem.getMysqlPassword());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    String claim(Request request, Response response) {
        String captcha = request.queryParams("captcha");
        String address = request.queryParams("address").replaceAll("\\s+", "");
        String ip = request.queryParams("ip");
        String currency = request.queryParams("currency");
        String userAgent = request.queryParamOrDefault("user-agent", "no");
        String captchaType = request.queryParamOrDefault("captchaType", "recaptcha");
        return claim(address, captcha, ip, currency, userAgent, captchaType).toString();
    }

    static String getTable(String currency) {
        String table = "";
        if (currency.equals("sumo")) {
            table = "sumo";
        } else if (currency.equals("ryo")) {
            table = "ryo";
        } else if (currency.equals("intense")) {
            table = "intense";
        } else if (currency.equals("masari")) {
            table = "masari";
        } else if (currency.equals("loki")) {
            table = "loki";
        }
        return table;
    }

    private JSONObject claim(String address, String captcha, String ip, String currency, String userAgent, String captchaType) {
        String table = getTable(currency);
        boolean comp = true;
        int fraudScore = -1;
        double balance = 0.0;
        double totalPaid = 0.0;
        long lastClaim = 0;
        double dailyBonus = 0;
        long dailyLastClaim = 0;
        int claims = 0;
        int claimsToday = 0;
        int lastClaimDay = 0;
        int lastBonusDay = 0;
        long payoutDayReached = 0;
        int error = 0;
        boolean paymentPending = false;

        if (checkCaptcha(captcha, ip, currency, captchaType) && !address.equals("")) {
            if (!userAgent.equals("no")) {
                try {
                    fraudScore = IpHub.getIpq(ip, userAgent).getFraudScore();//get fraudscore
                    System.out.println("Fraud score: " + fraudScore);
                } catch (Exception e) {
                    e.printStackTrace();
                    IpHub.ipqList = new ArrayList<>();
                }
            }

            boolean addressExists = false;

            String queryCheck = "SELECT * from " + table + " WHERE address = ?";
            try {
                PreparedStatement ps = conn.prepareStatement(queryCheck);
                ps.setString(1, address);
                final ResultSet resultSet = ps.executeQuery();
                ps.close();

                if (resultSet.next()) {
                    balance = resultSet.getDouble(2);
                    lastClaim = resultSet.getTimestamp(3).getTime();
                    dailyLastClaim = resultSet.getTimestamp(4).getTime();
                    dailyBonus = resultSet.getDouble(5);
                    claims = resultSet.getInt(6);
                    totalPaid = resultSet.getDouble(7);
                    lastClaimDay = resultSet.getInt(8);
                    claimsToday = resultSet.getInt(9);
                    lastBonusDay = resultSet.getInt(10);
                    payoutDayReached = resultSet.getTimestamp(11).getTime();
                    paymentPending = resultSet.getBoolean("payment");
                    addressExists = true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Calendar cal = Calendar.getInstance();
            int day = cal.get(Calendar.DAY_OF_MONTH);

            double claimAmount = 0;
            boolean payout = false;
            Date date = new Date();
            long dif = date.getTime() - lastClaim;
            if (dif >= 300000) {
                if (dailyLastClaim == 0) {
                    dailyLastClaim = date.getTime();
                }
                long dif2 = date.getTime() - dailyLastClaim;
                if (dif2 >= 43200000 && dif2 <= 172800000 && lastBonusDay != day) {
                    if (dailyBonus < 0.99) {
                        dailyBonus = dailyBonus + 0.01;
                    }
                    lastBonusDay = day;
                    dailyLastClaim = date.getTime();
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

                if (balance > WithdrawHandler.getWithdrawLimit(currency)) {
                    payout = true;
                }
                claimAmount = Prices.getClaimAmount(claimsToday, currency);
                if (currency.equals("sumo") || currency.equals("ryo") || currency.equals("masari") || currency.equals("loki")) {
                    if (balance >= 0.3) {
                        claimAmount = claimAmount * 0.75;
                    } else if (balance > 0.12) {
                        claimAmount = claimAmount * 0.95;
                    }
                } else if (currency.equals("intense")) {
                    if (balance > 45) {
                        claimAmount = claimAmount * 0.7;
                    } else if (balance > 15) {
                        claimAmount = claimAmount * 0.9;
                    }
                }
                if (fraudScore == 100) {
                    claimAmount = claimAmount * 0.68;
                } else if (fraudScore >= 75) {
                    claimAmount = claimAmount * 0.78;
                }

                claimAmount = claimAmount * (1 + dailyBonus);

                balance = balance + claimAmount;
                claims = claims + 1;
                lastClaim = date.getTime();
                if (!payout && balance > WithdrawHandler.getWithdrawLimit(currency)) {
                    payoutDayReached = date.getTime();
                }
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                boolean validAddress = true;
                if (!addressExists) {
                    /*if (currency.equals("ryo")) {
                        String result = ExecuteShellCommand.executeCommand("/home/pi/ryo-wallet/ryo-address-validator " + address);
                        JSONObject jsonObject = new JSONArray(result).getJSONObject(0);
                        validAddress = jsonObject.getBoolean("valid");
                    }*/
                    /*if (currency.equals("sumo")) {
                        String address4Ch = address.substring(0, 4);
                        if (!address4Ch.equals("Sumi") && !address4Ch.equals("Sumo") && !address4Ch.equals("Subo")) {
                            validAddress = false;
                        }
                    }*/
                    validAddress = ValidateAddress.validateAddress(address, currency);
                }
                if (validAddress) {
                    JSONObject newItem = new JSONObject();
                    newItem.put("ip", ip);
                    newItem.put("lastClaim", date.getTime());
                    getJsonarrayIp(currency).put(newItem);//add claim date to ip array

                    if (addressExists) {
                        java.util.Date dt = new java.util.Date(lastClaim);
                        java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                        String currentTime = sdf.format(dt);
                        String currentTime2 = sdf.format(dt2);

                        String insert = "UPDATE " + table + " SET balance='" + round(balance, 6) + "', lastClaim='" + currentTime
                                + "', dailyLastClaim='" + currentTime2 + "', dailyBonus='" + round(dailyBonus, 2) + "', claims='" + claims + "', totalPaid='" + totalPaid +
                                "', lastClaimDay='" + lastClaimDay + "', claimsToday='" + claimsToday + "', lastBonusDay='" + lastBonusDay + "', payoutDayReached='"
                                + Payments.getTimeString(payoutDayReached) + "', ip=? WHERE address=?";
                        try {
                            PreparedStatement ps = conn.prepareStatement(insert);
                            ps.setString(1, address);
                            ps.setString(2, ip);
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            comp = false;
                            error = 500;
                        }
                    } else {
                        java.util.Date dt = new java.util.Date(lastClaim);
                        java.util.Date dt2 = new java.util.Date(dailyLastClaim);
                        String currentTime = sdf.format(dt);
                        String currentTime2 = sdf.format(dt2);

                        String insert = "INSERT INTO " + table + " VALUES (?, '" + round(balance, 6) + "', '" + currentTime
                                + "', '" + currentTime2 + "', '" + dailyBonus + "', '" + claims + "', '" + totalPaid + "', '" + lastClaimDay + "', '" + claimsToday
                                + "', '" + lastBonusDay + "', '" + Payments.getTimeString(payoutDayReached) + "', ?, 0)";
                        try {
                            PreparedStatement ps = conn.prepareStatement(insert);
                            ps.setString(1, address);
                            ps.setString(2, ip);
                            ps.executeQuery();
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            error = 500;
                            comp = false;
                        }
                    }
                    String insert2 = "INSERT INTO claims VALUES (NULL, '" + sdf.format(new Date()) + "', ?, '" + claimAmount + "', ?, '" + currency + "')";
                    try {
                        PreparedStatement ps = conn.prepareStatement(insert2);
                        ps.setString(1, address);
                        ps.setString(2, ip);
                        ps.executeQuery();
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    System.out.println(getTime() + "Faucet claim " + comp + " balance: " + WithdrawHandler.round(balance, 5) + currency
                            + " Claimed:" + claimAmount + " Ip: " + ip + " Address: " + address);
                } else {
                    comp = false;
                    error = 3;//invalid address
                }
            } else {
                comp = false;
                error = 1;//5 minutes not over
            }
        } else {
            comp = false;
            error = 2;//Wrong captcha / ip 5 minutes not over
        }
        JSONObject claimItem = new JSONObject();
        claimItem.put("success", comp);
        try {
            claimItem.put("balance", balance);
            claimItem.put("dailyBonus", dailyBonus);
            claimItem.put("totalPaid", totalPaid);
            claimItem.put("lastClaim", lastClaim);
            claimItem.put("claimsToday", claimsToday);
            claimItem.put("payoutDayReached", payoutDayReached);
            claimItem.put("paymentPending", paymentPending);
        } catch (Exception ignored) {

        }
        if (error != 0) {
            claimItem.put("error", error);
        }

        return claimItem;
    }



    private boolean checkCaptcha(String captchaStr, String ip, String currency, String captchaType) {
        boolean comp = true;
        if (captchaType.equals("recaptcha")){
            comp = captcha.checkRecaptcha(captchaStr, ip, currency);
        } else {
            comp = captcha.checkHCaptcha(captchaStr, ip, currency);
        }

        JSONArray jsonArrayIp = getJsonarrayIp(currency);
        ///Double ip check
        if (comp) {
            int num = -1;
            long lastClaim = 0;
            for (int i = 0; i < jsonArrayIp.length(); i++) {
                JSONObject addr = jsonArrayIp.getJSONObject(i);
                try {
                    if (addr.getString("ip").equals(ip)) {
                        lastClaim = addr.getLong("lastClaim");
                        num = i;
                    }
                } catch (Exception ignored) {
                }
            }
            Date date = new Date();
            long dif = date.getTime() - lastClaim;
            if (dif <= 300000) {
                System.out.println(getTime() + "Ip double claim " + ip);
                comp = false;
            } else {
                if (num != -1) {
                    jsonArrayIp.remove(num);
                }
            }
            if (currency.equals("sumo")) {
                jsonArrayIpSumo = jsonArrayIp;
            } else if (currency.equals("ryo")) {
                jsonArrayIpRyo = jsonArrayIp;
            } else if (currency.equals("intense")) {
                jsonArrayIpIntense = jsonArrayIp;
            } else if (currency.equals("masari")) {
                jsonArrayIpMasari = jsonArrayIp;
            } else if (currency.equals("loki")) {
                jsonArrayIpLoki = jsonArrayIp;
            }
            /*
            try {
                if(IpHub.checkIp(ip) != 0) {
                    System.out.println("Iphub not 0");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }
        return comp;
    }

    private static String getTime() {
        return SIMPLE_DATE_FORMAT.format(new Date()) + " - ";
    }

    private JSONArray getJsonarrayIp(String currency) {
        JSONArray jsonArrayIp = new JSONArray();

        if (currency.equals("sumo")) {
            jsonArrayIp = jsonArrayIpSumo;
        } else if (currency.equals("ryo")) {
            jsonArrayIp = jsonArrayIpRyo;
        } else if (currency.equals("intense")) {
            jsonArrayIp = jsonArrayIpIntense;
        } else if (currency.equals("masari")) {
            jsonArrayIp = jsonArrayIpMasari;
        } else if (currency.equals("loki")) {
            jsonArrayIp = jsonArrayIpLoki;
        }
        return jsonArrayIp;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
