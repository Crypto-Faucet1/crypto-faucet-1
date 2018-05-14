import spark.Request;
import spark.Response;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SetBonusHandler {
    String setBonus(Request request, Response response) {
        String res = "done";
        String address = request.queryParams("address");
        double bonus = Double.parseDouble(request.queryParams("bonus"));


        try {
            PreparedStatement ps2 = ClaimHandler.conn.prepareStatement("UPDATE Addresses SET dailyBonus=" + bonus + " WHERE address = ?");
            ps2.setString(1, address);
            ps2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            res = "Error";
        }

        return res;
    }
}
