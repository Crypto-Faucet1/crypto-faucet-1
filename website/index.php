<?php
header('Cache-Control: no cache');
session_cache_limiter('private_no_expire');
session_start();

global $minpayout;
global $currencyShort;
global $currencyLong;
global $currency;
global $currencyLongLow;
global $adCode1;
global $adCode2;
$minpayout = 0.10;
$currency = "sumo";
$currencyShort = "sumo";
$currencyLong = "Sumokoin";
$currencyLongLow = "sumokoin";
$adCode2 = "<iframe data-aa='927561' src='//ad.a-ads.com/927561?size=120x600' scrolling='no'
                    style='width:120px; height:600px; border:0px; padding:0;overflow:hidden'
                    allowtransparency='true'></iframe>";
$adCode1 = "<iframe data-aa='1048744' src='//ad.a-ads.com/1048744?size=468x60' scrolling='no'
                            style='width:468px; height:60px; border:0px; padding:0;overflow:hidden'
                            allowtransparency='true'></iframe>";

include('/var/www/altfaucet.xyz/www/main.php');
