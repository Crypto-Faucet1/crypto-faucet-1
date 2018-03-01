import spark.Request;
import spark.Response;

public class RateHandler {

    public static String getClaimRate(Request request, Response response) {
        String claimRate = "";
        double bitKeer = 1;
        if(ClaimHandler.bitcoinRate <= 11000){
            bitKeer = bitKeer * 0.7;
        } else if(ClaimHandler.bitcoinRate <= 13000){
            bitKeer = bitKeer * 0.8;
        } else if(ClaimHandler.bitcoinRate <= 14000){
            bitKeer = bitKeer * 0.85;
        } else if (ClaimHandler.bitcoinRate <= 15000){
            bitKeer = bitKeer * 0.9;
        } else if (ClaimHandler.bitcoinRate <= 16000){

        } else if (ClaimHandler.bitcoinRate <= 20000) {
            bitKeer = bitKeer * 1.05;
        }

        claimRate = claimRate + 0.00000006 / ClaimHandler.sumoRate * bitKeer * 0.5 + ";";
        claimRate = claimRate + 0.00000015 / ClaimHandler.sumoRate * bitKeer * 0.5 + ";";
        claimRate = claimRate + 0.00000280 / ClaimHandler.sumoRate * bitKeer * 0.5 + ";";
        claimRate = claimRate + 0.00000638 / ClaimHandler.sumoRate * bitKeer * 0.5;

        return claimRate;
    }
}
