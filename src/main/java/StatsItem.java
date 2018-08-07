public class StatsItem {
    private String date;
    private int sumoClaimsToday;
    private int ryoClaimsToday;
    private int intenseClaimsToday;

    public StatsItem(String date, int sumoClaimsToday, int ryoClaimsToday, int intenseClaimsToday) {
        this.date = date;
        this.sumoClaimsToday = sumoClaimsToday;
        this.ryoClaimsToday = ryoClaimsToday;
        this.intenseClaimsToday = intenseClaimsToday;
    }

    public String getDate() {
        return date;
    }

    public int getSumoClaimsToday() {
        return sumoClaimsToday;
    }

    public int getRyoClaimsToday() {
        return ryoClaimsToday;
    }

    public int getIntenseClaimsToday() {
        return intenseClaimsToday;
    }
}
