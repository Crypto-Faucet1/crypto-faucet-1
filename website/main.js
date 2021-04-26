function WriteCookie()
{
    var now = new Date();
    now.setMonth( now.getMonth() + 12 );
    cookievalue = escape(document.myForm.address.value);

    document.cookie="address=" + cookievalue + ";expires=" + now.toUTCString() + ";";
}
function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}
var addressCookie = getCookie('address');
document.getElementById("inp").value = addressCookie;

function startTimer(duration, display) {
    var timer = duration, minutes, seconds;
    setInterval(function () {
        minutes = parseInt(timer / 60, 10);
        seconds = parseInt(timer % 60, 10);

        minutes = minutes < 10 ? "0" + minutes : minutes;
        seconds = seconds < 10 ? "0" + seconds : seconds;

        display.text("Time left: " + minutes + ":" + seconds);

        if (--timer < 0) {
            display.text("Time left: 00:00");
        }
    }, 1000);
}

    startTimer(fiveMinutes, display);

function switchCaptcha() {
	var captchaType = getCookie("captchaType");
	if (captchaType == "recaptcha"){
		captchaType = "hcaptcha";
	} else if (captchaType == "hcaptcha" || captchaType == null){
		captchaType = "recaptcha";
	}
	var now = new Date();
    now.setDate(now.getDate() + 7 );

    document.cookie="captchaType=" + captchaType + ";expires=" + now.toUTCString() + ";";
    location.reload();
}

var adBlockEnabled = false;
var testAd = document.createElement('div');
testAd.innerHTML = '&nbsp;';
testAd.className = 'adsbox';
document.body.appendChild(testAd);
window.setTimeout(function() {
    if (testAd.offsetHeight === 0) {
        adBlockEnabled = true;
    }
    testAd.remove();
    console.log('AdBlock Enabled? ', adBlockEnabled)
    if(adBlockEnabled){
        window.alert("You need to disable AdBlock.");
        document.getElementById("text12").textContent="Please disable AdBlock.";
        document.getElementById("text12").style.color = "red";
        document.getElementById("butonn").style.display = 'none';
        document.getElementById("capt").style.display = 'none';
        document.getElementById("inp").style.display = 'none';
    }
}, 100);

balanceDisplay = balance;
if (currency == "loki"){
  balanceDisplay = balanceDisplay - 0.04;
 balanceDisplay = balanceDisplay.toFixed(4);
}

if (balance > minPayout) {
    if (paymentPending == "1") {
        document.getElementById("paymentMessage").innerHTML = "You will receive " + balanceDisplay
            + " " + currencyShort + " before " + payoutDayReached + ".";
    } else {
        $("#withdraw-button").show();
        document.getElementById("paymentMessage").innerHTML = "You have reached the minimum of " + minPayout + " " + currencyShort + " and can now withdraw. Withdrawals are processed within 7 days.";
    }
}

function requestPayment() {
    $.post("https://" + currency + ".altfaucet.xyz/api/request?currency=" + currency + "&address=" + address, function( data ) {
        document.getElementById("paymentMessage").innerHTML = "You will receive " + balanceDisplay
            + " " + currencyShort + " within 7 days.";
        $("#withdraw-button").hide();
    });
}
