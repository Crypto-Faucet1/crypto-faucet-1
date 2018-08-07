import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Stats {
    public Stats() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new update(), millisToNextDay(Calendar.getInstance()) - 120000, 86400000);//1 day
    }

    public class update extends TimerTask {
        @Override
        public void run() {
            String time = getStartOfDay();
            int sumo = timesClaimedToday("sumo", time);
            int ryo = timesClaimedToday("ryo", time);
            int intense = timesClaimedToday("intense", time);
            StatsItem item = new StatsItem(getCurrentTime(), sumo, ryo, intense);
            insertInDb(item);
        }
    }

    private void insertInDb(StatsItem item) {
        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            stmt.executeUpdate("INSERT INTO stats VALUES ('" + item.getDate() + "', '" + item.getSumoClaimsToday()
                    + "', '" + item.getRyoClaimsToday() + "', '" + item.getIntenseClaimsToday() + "')");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int timesClaimedToday(String table, String time) {
        int amount = 0;

        try {
            Statement stmt = ClaimHandler.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from " + table + " WHERE lastClaim > '" + time + "'");
            while (rs.next()) {
                amount = amount + rs.getInt("claimsToday");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println(table + ": " + amount);
        return amount;
    }

    private String getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        java.util.Date dt = cal.getTime();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(dt);
        return time;
    }

    private String getCurrentTime(){
        java.util.Date dt = new Date();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(dt);
        return time;
    }

    private static long millisToNextDay(Calendar calendar) {
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        int hoursToNextDay = 23 - hours;
        int minutesToNextHour = 60 - minutes;
        int secondsToNextHour = 60 - seconds;
        int millisToNextHour = 1000 - millis;
        return hoursToNextDay * 60 * 60 * 1000 + minutesToNextHour * 60 * 1000 + secondsToNextHour * 1000 + millisToNextHour;
    }
}
