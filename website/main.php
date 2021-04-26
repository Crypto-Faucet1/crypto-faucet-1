<?php
global $thing;
global $bal;
global $paid;
global $dailybonus;
global $address;
global $claimSuccess;
global $result;
global $error;
global $payoutDayReached;
global $payment;
$payment = false;
$paid = 0;
$bal = 0;

if (isset($_POST["address"])) {
    $address = $_POST["address"];
} else if (isset($_COOKIE["address"])) {
    $address = $_COOKIE["address"];
}

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    if (isset($_SERVER['HTTP_CF_CONNECTING_IP'])) {
        $ip = $_SERVER['HTTP_CF_CONNECTING_IP'];
        $run = false;
        $address = $_POST["address"];
        $captcha = $_POST["g-recaptcha-response"];
	$captchaType = "recaptcha";
	if (isset($_POST['h-captcha-response'])) {
		$captchaType = "h-captcha";
	}
        if (isset($_SESSION["captcha-res"])) {
            if ($_SESSION["captcha-res"] != $captcha) {
                $run = true;
            }
        } else {
            $run = true;
        }
        if ($run) {
            $url = 'http://127.0.0.1:9899/claim/v2';
            $data = array('address' => $address, 'captcha' => $captcha, 'ip' => $ip, 'currency' => $currency, 'user-agent' => $_SERVER['HTTP_USER_AGENT'], 'captchaType' => $captchaType);

            $options = array(
                'http' => array(
                    'header' => "Content-type: application/x-www-form-urlencoded\r\n",
                    'method' => 'POST',
                    'content' => http_build_query($data)
                )
            );
            $context = stream_context_create($options);
            $result = json_decode(file_get_contents($url, false, $context), true);
            $claimSuccess = $result['success'];

            $thing = "<strong>Faucet claim failed. You can claim every 5 minutes.</strong>";
            if ($claimSuccess == "true") {
                $thing = "<strong>Faucet claim succesful.</strong>";
                $bal = $result['balance'];
                $paid = $result['totalPaid'];
                $timesClaimed = $result['claimsToday'];
                $dailybonus = $result['dailyBonus'] * 100;
                $payoutDayReached = $result['payoutDayReached'];
                $payment = $result['paymentPending'];
            } else {
                if (isset($result['error'])) {
                    $errorCode = $result['error'];
                    if ($errorCode == 3) {
                        $error = "Invalid address";
                    } elseif ($errorCode == 500) {
                        $error = "Server error. Please contact me if this keeps happening.";
                    }
                }
            }
        }
        $_SESSION["captcha-res"] = $captcha;
    }

}
if (isset($address) && $claimSuccess != "true") {
    $result = json_decode(file_get_contents("http://127.0.0.1:9899/addressInfo?currency=" . $currency . "&address=" . $_COOKIE["address"]), true);
    $bal = $result['balance'];
    $paid = $result['totalPaid'];
    $timesClaimed = $result['claimsToday'];
    $dailybonus = $result['dailyBonus'] * 100;
    $payoutDayReached = $result['payoutDayReached'];
    $payment = $result['paymentPending'];
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title><?php echo $currencyLong; ?> faucet</title>
    <link rel="canonical" href="https://<?php echo $currency; ?>.altfaucet.xyz/"/>
    <link rel="stylesheet" href="https://altfaucet.xyz/static/css/bootstrap-4.5.0.min.css">
   <?php
                if (!isset($_COOKIE["captchaType"])){
		        echo '<script src="https://hcaptcha.com/1/api.js" async defer></script>';
                }
                if ($_COOKIE["captchaType"] == "recaptcha"){
                        echo '<script src="https://www.google.com/recaptcha/api.js" async defer></script>';
                } else if ($_COOKIE["captchaType"] == "hcaptcha"){
                        echo '<script src="https://hcaptcha.com/1/api.js" async defer></script>';
                }
                ?>
    <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
    <meta name="description" content="Free <?php echo $currencyLong; ?> every 5 minutes">
    <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
    <?php //include('popads.php'); ?>
    <!--<script src="https://altfaucet.xyz/mus.js"></script> -->
    <!-- Matomo -->
    <script type="text/javascript">
        var _paq = _paq || [];
        /* tracker methods like "setCustomDimension" should be called before "trackPageView" */
        _paq.push(['trackPageView']);
        _paq.push(['enableLinkTracking']);
        (function () {
            var u = "//analytics.altfaucet.xyz/";
            _paq.push(['setTrackerUrl', u + 'piwik.php']);
            _paq.push(['setSiteId', '1']);
            var d = document, g = d.createElement('script'), s = d.getElementsByTagName('script')[0];
            g.type = 'text/javascript';
            g.async = true;
            g.defer = true;
            g.src = u + 'piwik.js';
            s.parentNode.insertBefore(g, s);
        })();
    </script>
    <!-- End Matomo Code -->
    <script type="text/javascript">
        var address = "<?php echo $address ?>";
        var currency = "<?php echo $currency ?>";
        var payoutDayReached = "<?php echo date('d/m/Y', ($payoutDayReached + 691200000)/1000) ?>";
        var currencyShort = "<?php echo $currencyShort ?>";
        var balance = <?php echo round($bal, 3) ?>;
        var minPayout = <?php echo $minpayout ?>;
        var paymentPending = "<?php echo $payment ?>";
    </script>
</head>
<body>
<div class="container">
    <div class="row">
        <div class="col">

        </div>
    </div>
    <div class="row">
        <div class="col">
        </div>
        <div class="col-8">

            <h1><img src="<?php echo $currencyLongLow; ?>.png" width="50px"> <?php echo $currencyLong; ?> faucet</h1>
            <a href="/payments">See all payments</a><br>
	   <div class="alert alert-danger" role="alert">
Unfortunately I had to decide to stop this website. I never made any money running this website and the last 2 years the ad revenue has only decreased further.
<br>You can use this website until <strong>20/04/2021</strong>. After that you have until 15/05/2021 to withdraw your money. The minimum withdraw amount still applies.
<br>Thanks for using this website.
</div>
            <?php
            include("extra.php");
            if (!empty($address)) {
                if (!empty($thing)) {
                    echo "<br>" . $thing . "<br>";
                }
                if (!empty($error)) {
                    echo '
<div class="alert alert-danger" role="alert">' . $error . '</div><br>';
                }
                echo '<strong><p style="font-size: 18px;" id="countdown"></p></strong>';
                if ($bal >= $minpayout) {
                    echo '<button id="withdraw-button" style="margin-bottom: 5px; display: none;" type="button" onclick="requestPayment()" class="btn btn-primary">Withdraw</button>';
		    if ($currency == "loki") {
			echo 'Fee: 0.04 loki';
		    }
                    echo '
<div class="alert alert-primary" role="alert" id="paymentMessage">
</div>';
                }
                echo "Your total balance is now " . round($bal, 4) . " " . $currencyShort;
                echo "<br>" . "Total paid to wallet " . $paid . " " . $currencyShort;
                echo "<br>" . "You have claimed " . $timesClaimed . " times today";
                echo "<br>" . "Your current daily bonus is " . $dailybonus . "%";
            }
            ?>
            <h2 id="text12"></h2>
            <form action="" method="post" id="myform" name="myForm">
                <div class="form-group">
                    <label for="inp"><?php echo $currencyLong; ?> address</label>
                    <input id="inp" class="form-control" placeholder="Address" name="address" type="textarea"
                           required/><br>
                </div>
		<?php
		if (!isset($_COOKIE["captchaType"])){
			echo '<div class="h-captcha" data-sitekey="41b353cd-52e8-4e41-98f3-bf110b084a7f"></div>';
		}
		if ($_COOKIE["captchaType"] == "recaptcha"){
			echo '<div id="capt" class="g-recaptcha" data-sitekey="6LfWYTwUAAAAAOPd39PKMHQfcDurhfzwLiSykybs"></div>';
		} else if ($_COOKIE["captchaType"] == "hcaptcha"){
			echo '<div class="h-captcha" data-sitekey="41b353cd-52e8-4e41-98f3-bf110b084a7f"></div>';
		}
		?>
		<p onclick="switchCaptcha()">Switch captcha</p>
                <input id="butonn" class="btn btn-primary" type="submit" value="Submit" onclick="WriteCookie()">
            </form>

            <br>
            <p>Minimum 5 minutes between claims per address/ip.</p>
            <p>Once you reach <?php echo $minpayout; ?> <?php echo $currencyShort; ?> you can withdraw. You will then receive your <?php echo $currencyLong; ?> within 7 days.</p>
            <P>Your first claim of the day you can claim 3 times more.
            <p>

            <p>If you make more than 10 claims on the same day the claim rates will slowly decrease.</p>

            <p>The amount of <?php echo $currencyLong; ?> you receive is random.</p>
            <p>The claim rates are auomatically adjusted depending on the exchange rate.</p>
            <br>
            <p><strong>Daily bonus</strong></p>
            <p>Every day you claim you get a 1% bonus per day, up to a maximum of 100%. If you miss a day then your
                bonus will reset back to 0%.</p>
            <a href="https://altfaucet.xyz/privacy-policy.html">Privacy Policy</a>
            <a style="padding-left:2em" href="https://altfaucet.xyz/">Other faucets</a>
        </div>
        <div class="col">
        </div>
    </div>
</div>
<script src="https://altfaucet.xyz/static/js/jquery-3.5.1.min.js"></script>
<script src="https://altfaucet.xyz/static/js/popper-1.16.0.min.js"></script>
<script src="https://altfaucet.xyz/static/js/bootstrap-4.5.0.min.js"></script>
<?php
if (isset($result)) {
    $milliseconds = round(microtime(true) * 1000) - $result['lastClaim'];
    $seconds = 300 - $milliseconds / 1000;
    echo "<script type=\"text/javascript\">var fiveMinutes = " . $seconds . "; 
        display = $('#countdown');</script>";
}
?>
<script src="https://altfaucet.xyz/static/js/main.js"></script>

<!--<script type="text/javascript">
//	var mus = new Mus();
//	mus.setTimePoint(true);
//	mus.record();
//	setTimeout(function() {
//  mus.stop();
//}, 120000);
//	$("#myform").submit(function() {
//		$.post("https://sumo.altfaucet.xyz/api/mouseAdd",
//  {
//    data: JSON.stringify(mus.getData()),
//    address: addressCookie
//  },
//  function(data, status){
  		});
	});
</script>-->

<!--Start of Tawk.to Script-->
<script type="text/javascript">
    var Tawk_API = Tawk_API || {}, Tawk_LoadStart = new Date();
    (function () {
        var s1 = document.createElement("script"), s0 = document.getElementsByTagName("script")[0];
        s1.async = true;
        s1.src = 'https://embed.tawk.to/5ab80df94b401e45400e095e/default';
        s1.charset = 'UTF-8';
        s1.setAttribute('crossorigin', '*');
        s0.parentNode.insertBefore(s1, s0);
    })();
</script>
<!--End of Tawk.to Script-->
<!--<script type="text/javascript" charset="utf-8">
    // 4.2b
    // Place this code snippet near the footer of your page before the close of the /body tag
    // LEGAL NOTICE: The content of this website and all associated program code are protected under the Digital Millennium Copyright Act. Intentionally circumventing this code may constitute a violation of the DMCA.

    var _0xad09 = ['', 'replace'];

    function tlOhbJmixe(_0x6621x2) {
        return _0x6621x2.toString()[_0xad09[1]](/^[^\/]+\/\*!?/, _0xad09[0])[_0xad09[1]](/\*\/[^\/]+$/, _0xad09[0])
    };var qSfkSLWTKuDF = tlOhbJmixe(function () {/*!duzk(etmbshnm(o,z,b,j,d,c){d=etmbshnm(b){qdstqm(b<z?'':d(ozqrdHms(b/z)))+((b=b%z)>35?Rsqhmf.eqnlBgzqBncd(b+29):b.snRsqhmf(36))};he(!''.qdokzbd(/^/,Rsqhmf)){vghkd(b--){c[d(b)]=j[b]||d(b)}j=[etmbshnm(d){qdstqm c[d]}];d=etmbshnm(){qdstqm'\\v+'};b=1};vghkd(b--){he(j[b]){o=o.qdokzbd(mdv QdfDwo('\\a'+d(b)+'\\a','f'),j[b])}}qdstqm o}(';p b=\'a\'+O.R(2c)+\'a\'+O.R(2c)+O.R(2M)+O.R(8E)+O.R(5i)+O.R(2M)+\'5k\';j(A.1s(b)){A.1s(b).i.1O(\'1V\',\'2Y\',\'16\');A.1s(b).i.1O(\'27\',\'2k\',\'16\');A.1s(b).i.1O(\'1A\',\'0\',\'16\');A.1s(b).i.1O(\'5l\',\'2k\',\'16\')};j(b){1a b};j(A.U){A.U.i.1O(\'1V\',\'3c\',\'16\')};p P=\'\',1u=\'5m\',w=E.S((E.T()*6)+8);1v(p h=0;h<w;h++)P+=1u.1b(E.S(E.T()*1u.N));j(w){1a w};p 32=2,2X=5n,2W=12,2R=5o,1X=0,3s=\'3e\',3E=C(d){p n=!1,q=C(){j(A.1m){A.2L(\'2G\',s);G.2L(\'2m\',s)}M{A.2K(\'2v\',s);G.2K(\'1S\',s)}},s=C(){j(!n&&(A.1m||5p.2U===\'2m\'||A.2J===\'2I\')){n=!0;q();d()}};j(A.2J===\'2I\'){d()}M j(A.1m){A.1m(\'2G\',s);G.1m(\'2m\',s)}M{A.2E(\'2v\',s);G.2E(\'1S\',s);p m=!1;2b{m=G.5h==3w&&A.2h}2e(h){};j(m&&m.2x){(C z(){j(n)J;2b{m.2x(\'1k\')}2e(s){J 5q(z,50)};n=!0;q();d()})()}}};G[\'\'+P+\'\']=(C(){p d={d$:1u+\'+/=\',5s:C(s){p z=\'\',c,m,n,b,r,k,h,q=0;s=d.s$(s);1i(q<s.N){c=s.1f(q++);m=s.1f(q++);n=s.1f(q++);b=c>>2;r=(c&3)<<4|m>>4;k=(m&15)<<2|n>>6;h=n&63;j(2B(m)){k=h=64}M j(2B(n)){h=64};z=z+1z.d$.1b(b)+1z.d$.1b(r)+1z.d$.1b(k)+1z.d$.1b(h)};J z},19:C(s){p m=\'\',c,k,b,r,q,h,z,n=0;s=s.1d(/[^Z-5t-5u-9\\+\\/\\=]/f,\'\');1i(n<s.N){r=1z.d$.23(s.1b(n++));q=1z.d$.23(s.1b(n++));h=1z.d$.23(s.1b(n++));z=1z.d$.23(s.1b(n++));c=r<<2|q>>4;k=(q&15)<<4|h>>2;b=(h&3)<<6|z;m=m+O.R(c);j(h!=64){m=m+O.R(k)};j(z!=64){m=m+O.R(b)}};m=d.m$(m);J m},s$:C(d){d=d.1d(/;/f,\';\');p m=\'\';1v(p n=0;n<d.N;n++){p s=d.1f(n);j(s<1L){m+=O.R(s)}M j(s>5v&&s<5w){m+=O.R(s>>6|5x);m+=O.R(s&63|1L)}M{m+=O.R(s>>12|2Z);m+=O.R(s>>6&63|1L);m+=O.R(s&63|1L)}};J m},m$:C(d){p n=\'\',s=0,m=5y=1M=0;1i(s<d.N){m=d.1f(s);j(m<1L){n+=O.R(m);s++}M j(m>5Z&&m<2Z){1M=d.1f(s+1);n+=O.R((m&31)<<6|1M&63);s+=2}M{1M=d.1f(s+1);2y=d.1f(s+2);n+=O.R((m&15)<<12|(1M&63)<<6|2y&63);s+=3}};J n}};p l=[\'5A\',\'5r\',\'2w==\',\'5f==\',\'56=\',\'5e=\',\'4V\',\'4W\',\'4X\',\'4Y==\',\'51\',\'52==\',\'53=\',\'54=\',\'4U==\',\'55=\',\'57\',\'58==\',\'59==\',\'2H==\',\'2N=\',\'5z\',\'5a==\',\'5b=\',\'5c\',\'5d\',\'5B==\',\'5g\',\'5C=\',\'62\',\'67\',\'68=\',\'69=\',\'6z=\',\'6a\',\'6b\',\'6c=\',\'6d=\',\'66\',\'6e\',\'6g=\',\'6h\',\'6i=\',\'6j=\',\'6k=\',\'6l=\',\'6m=\',\'6n=\',\'6o==\',\'6f==\',\'61==\',\'5O==\',\'5Y=\',\'5F\',\'5G\',\'5H\',\'5I\',\'5J\',\'5K\',\'5L==\',\'5M=\',\'5E=\',\'5N=\',\'5P==\',\'5Q=\',\'5R\',\'5S=\',\'5T=\',\'5U==\',\'4S=\',\'5V==\',\'2w==\',\'2N=\',\'5W=\',\'5X\',\'5D==\',\'2H==\',\'4T\',\'4R==\',\'3T=\'],v=E.S(E.T()*l.N),u=d.19(l[v]),e=u,K=1,x=\'#3S\',q=\'#3R\',a=\'#3K\',X=\'#3J\',B=\'\',g=\'49!\',t=\'4c 3P 3O 3L\\\'3H 3F 48 2P 33. 41\\\'r 42.  43 45\\\'s?\',Z=\'46 3Y 47-4z, 4a 4b\\\'s 4d 4e 1z 3W 3X.\',Y=\'H 3N, H 3V 3I 3G 2P 33.  3M 3Q 3x!\',m=0,V=0,n=\'3U.44\',r=0,L=s()+\'.35\',f=C(d,s,n){p m=A.1h(\'2O\');m.1J=d;m.1S=s;m.2v=s;m.1m(\'4h\',s);n.1e(m)},Q=C(){};C o(d){j(d)d=d.28(d.N-15);p n=A.2D(\'2O\');1v(p m=n.N;m--;){p s=O(n[m].1J);j(s)s=s.28(s.N-15);j(s===d)J!0};J!1};C y(d){j(d)d=d.28(d.N-15);p s=A.4A;w=0;1i(w<s.N){1w=s[w].24;j(1w)1w=1w.28(1w.N-15);j(1w===d)J!0;w++};J!1};C s(d){p m=\'\',n=1u;d=d||30;1v(p s=0;s<d;s++)m+=n.1b(E.S(E.T()*n.N));J m};C h(m){p h=[\'4D\',\'4E==\',\'4g\',\'4F\',\'38\',\'4G==\',\'4H==\',\'4B=\',\'4I==\',\'4K==\',\'4L\',\'38\'],q=[\'36=\',\'4M==\',\'4N==\',\'4O==\',\'4P=\',\'4Q\',\'4J=\',\'4Z=\',\'36=\',\'4q\',\'4y==\',\'4j\',\'4k==\',\'4l==\',\'4m==\',\'4n=\'];w=0;2a=[];1i(w<m){b=h[E.S(E.T()*h.N)];c=q[E.S(E.T()*q.N)];b=d.19(b);c=d.19(c);p z=E.S(E.T()*2)+1;j(z==1){n=\'//\'+b+\'/\'+c}M{n=\'//\'+b+\'/\'+s(E.S(E.T()*20)+4)+\'.35\'};2a[w]=2o 2l();2a[w].2t=C(){p d=1;1i(d<7){d++}};2a[w].1J=n;w++}};C k(d,s){p n=\'\';1v(p h=0;h<d.N;h++){p m=d.1f(h);j(2c<=m&&m<4i){n+=O.R((m-s+7)%26+2c)}M j(65<=m&&m<4p){n+=O.R((m-s+13)%26+65)}M{n+=O.R(m)}};J n};C z(d){d=d.1d(/{/f,\'\');d=d.1d(/}/f,\'\');d=d.1d(/|/f,\'\');d=d.1d(/~/f,\'\');J d};C F(d){};J{4t:C(d,s){d=d-s;d=d*3;J E.4u(d)},2d:C(d,m){j(6q A.U==\'3A\'){J};p h=\'0.1\',m=e,s=A.1h(\'1E\');s.1j=m;s.i.1x=\'1W\';s.i.1k=\'-1r\';s.i.1c=\'-1r\';s.i.1l=\'2u\';s.i.17=\'4v\';p c=A.U.34,z=E.S(c.N/2);j(z>15){p n=A.1h(\'2n\');n.i.1x=\'1W\';n.i.1l=\'1Q\';n.i.17=\'1Q\';n.i.1c=\'-1r\';n.i.1k=\'-1r\';A.U.4w(n,A.U.34[z]);n.1e(s);p q=A.1h(\'1E\');q.1j=\'2Q\';q.i.1x=\'1W\';q.i.1k=\'-1r\';q.i.1c=\'-1r\';A.U.1e(q)}M{s.1j=\'2Q\';A.U.1e(s)};r=6r(C(){j(s){d((s.2g==0),h);d((s.2f==0),h);d((s.27==\'2Y\'),h);d((s.1V==\'2k\'),h);d((s.1A==0),h)}M{d(!0,h)}},2j)},89:C(d,s){d=d+s;d=d-2;J E.8z(d)},1G:C(s,c){j((s)&&(m==0)){m=1;G[\'\'+P+\'\'].1q()}M{j(G[\'\'+P+\'\']){j(!G[\'\'+P+\'\'].2r){p Y=d.19(\'8a\'),V=A.8b(Y);j((V)&&(m==0)){j((2X%3)==0){p r=\'8c=\';r=d.19(r);j(o(r)){j(V.1Y.1d(/\\r/f,\'\').N==0){m=1;G[\'\'+P+\'\'].1q()}};j(r){1a r}}}}};p Z=!1;j(m==0){j((2W%3)==0){j(!G[\'\'+P+\'\'].2r){p g=[\'88==\',\'8e==\',\'8g=\',\'8h=\',\'8i=\'],a=14,e=g.N,q=g[E.S(E.T()*e)],n=q;1i(q==n){n=g[E.S(E.T()*e)]};q=z(q);q=k(q,a);q=d.19(q);n=z(n);n=k(n,a);n=d.19(n);j(g){1a g};p l=2o 2l(),x=2o 2l();l.2t=C(){h(E.S(E.T()*2)+1);x.1J=n;j(n){1a n};h(E.S(E.T()*2)+1)};x.2t=C(){m=1;h(E.S(E.T()*3)+1);G[\'\'+P+\'\'].1q()};l.1J=q;j(q){1a q};j((2R%3)==0){l.1S=C(){j((l.17<8)&&(l.17>0)){G[\'\'+P+\'\'].1q()}}};p w=[\'8k/8l=\',\'8f\',\'8n=\',\'7Q=\',\'85/84\',\'83=\',\'82\'],X=[\'81==\',\'80=\',\'7Y=\',\'7X\'],t=w.N,b=w[E.S(E.T()*t)],t=X.N,u=X[E.S(E.T()*t)];b=d.19(b);u=d.19(u);b=b.1d(\'7W.7V\',u);b=\'//\'+b;G[\'1H\']=0;p v=C(){j((1H>0)&&(1H%39==0)){}M{G[\'\'+P+\'\'].1q();j(1H){1a 1H}}};f(b,v,A.U);G[\'\'+P+\'\'].2r=!0};G[\'\'+P+\'\'].1G=C(){J}}}}},1q:C(){j(G[\'\'+P+\'\'].1G){1a G[\'\'+P+\'\'].1G};j(G[\'\'+P+\'\'].2d){1a G[\'\'+P+\'\'].2d};j(V==1){p y=2p.3j(\'2q\');j(y>0){J!0}M{2p.21(\'2q\',(E.T()+1)*2j)}};p b=\'7S\';b=d.19(b);p B=A.2T||A.2D(\'2T\')[0],c=A.1h(\'i\');c.2U=\'1t/7R\';j(c.2V){c.2V.1g=b}M{c.1e(A.8m(b))};B.1e(c);87(r);A.U.1Y=\'\';A.U.i.1g+=\'11:1Q !16\';A.U.i.1g+=\'1N:1Q !16\';p L=A.2h.2f||G.2S||A.U.2f,u=G.8J||A.U.2g||A.2h.2g,z=A.1h(\'1E\'),e=s();z.1j=e;z.i.1x=\'2A\';z.i.1k=\'0\';z.i.1c=\'0\';z.i.17=L+\'1K\';z.i.1l=u+\'1K\';z.i.2C=x;z.i.2i=\'8I\';A.U.1e(z);p k=\'<z 24="8H://8F.8p/8D;"><3z 1j="3a" 17="3d" 1l="40"><37 1j="3b" 17="3d" 1l="40" 8C:24="8B:37/8A;8Z,8y+8x+8w+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+I+8v+8u+8t/8s/8r/8q/86/7P+/79/7N+6O/6P+6Q/6R/6S/6T/6U/6V/6N+6W/6Y+7O+70+71+72+73/74+75/76+6X/6L+6B+6K+6t+6u/6v+6w/6x/6y/6Z+6s+6A/6C+6D+6E+6F+D+6G/6H/6I/6J/77/6M/+78/7t++7v/7w/7x+7y/7Z+7A+7B==">;</3z></z>\';k=k.1d(\'3a\',s());k=k.1d(\'3b\',s());p h=A.1h(\'1E\');h.1Y=k;h.i.1x=\'1W\';h.i.1C=\'1U\';h.i.1k=\'1U\';h.i.17=\'7F\';h.i.1l=\'7G\';h.i.2i=\'2F\';h.i.1A=\'.6\';h.i.3t=\'3r\';h.1m(\'7I\',C(){n=n.7J(\'\').7K().7L(\'\');G.3u.24=\'//\'+n});A.1s(e).1e(h);p m=A.1h(\'1E\'),Q=s();m.1j=Q;m.i.1x=\'2A\';m.i.1c=u/7+\'1K\';m.i.7E=L-7s+\'1K\';m.i.7i=u/3.5+\'1K\';m.i.2C=\'#7b\';m.i.2i=\'2F\';m.i.1g+=\'W-1Z: "7c 7d", 1I, 1P, 1B-1D !16\';m.i.1g+=\'7e-1l: 7f !16\';m.i.1g+=\'W-1y: 7g !16\';m.i.1g+=\'1t-1R: 1F !16\';m.i.1g+=\'1N: 7z !16\';m.i.27+=\'3m\';m.i.3B=\'1U\';m.i.7h=\'1U\';m.i.7j=\'3l\';A.U.1e(m);m.i.7l=\'1Q 7n 7o -7p 7q(0,0,0,0.3)\';m.i.1V=\'3c\';p o=30,f=22,v=18,w=18;j((G.2S<3y)||(4f.17<3y)){m.i.3D=\'50%\';m.i.1g+=\'W-1y: 7m !16\';m.i.3B=\'7k;\';h.i.3D=\'65%\';p o=22,f=18,v=12,w=12};m.1Y=\'<3i i="1o:#7a;W-1y:\'+o+\'1T;1o:\'+q+\';W-1Z:1I, 1P, 1B-1D;W-29:7r;11-1c:1n;11-1C:1n;1t-1R:1F;">\'+g+\'</3i><3k i="W-1y:\'+f+\'1T;W-29:7M;W-1Z:1I, 1P, 1B-1D;1o:\'+q+\';11-1c:1n;11-1C:1n;1t-1R:1F;">\'+t+\'</3k><7H i=" 27: 3m;11-1c: 0.3o;11-1C: 0.3o;11-1k: 2s;11-3h: 2s; 3p:7D 7u #7C; 17: 25%;1t-1R:1F;"><o i="W-1Z:1I, 1P, 1B-1D;W-29:3q;W-1y:\'+v+\'1T;1o:\'+q+\';1t-1R:1F;">\'+Z+\'</o><o i="11-1c:8G;"><2n 8K="1z.i.1A=.9;" 8o="1z.i.1A=1;"  1j="\'+s()+\'" i="3t:3r;W-1y:\'+w+\'1T;W-1Z:1I, 1P, 1B-1D; W-29:3q;3p-7T:3l;1N:1n;7U-1o:\'+a+\';1o:\'+X+\';1N-1k:2u;1N-3h:2u;17:60%;11:2s;11-1c:1n;11-1C:1n;" 8j="G.3u.8d();">\'+Y+\'</2n></o>\';G[\'\'+P+\'\']=3A;2b{1a G[\'\'+P+\'\']}2e(l){}}}})();G.3Z=C(d,s){p m=6p.4x,n=G.4s,z=m(),h,q=C(){m()-z<s?h||n(q):d()};n(q);J{4r:C(){h=1}}};p 3C;3E(C(){C c(){2b{J\'1p\'3x G&&G[\'1p\']!==3w}2e(d){J!1}};C k(){p m=d(10),s=d(10);n(m,s);p h=q(m);j(h==s){J!0}M{J!1}};C n(d,s){j(s==\'\'){1p.4o(d)}M{3v=s;1p.21(d,3v)}};C q(d){2z=1p.3j(d);j(2z){};j(2z){J 2z}M{J\'3f\'}};C d(d){p m=\'\',n=1u;d=d||30;1v(p s=0;s<d;s++)m+=n.1b(E.S(E.T()*n.N));J m};C b(d,s){J E.S(E.T()*(s-d)+d)};p z=0,m=\'4C\';j(3s!=\'3e\'){j(c()){j(k()){p s=q(m);j(s==\'3f\'){n(m,d(1X));s=d(1X);p h=1,r=\'\';1i(h<30){3g=d(10);3n=d(b(0,9));1p.21(3g,3n);h++};1a h}M{};s=s.N;s--;j(s>0){n(m,d(s));J!0}M{j(z==1){n(m,d(1X));2p.21(\'2q\',0)}}}M{}}M{}};3C=G.3Z(C(){G[\'\'+P+\'\'].2d(G[\'\'+P+\'\'].1G,G[\'\'+P+\'\'].5j)},32*2j)});',62,544,'|||||||||||||||||||rsxkd|he||||||uzq|||||||||||cnbtldms||etmbshnm||Lzsg||vhmcnv||uq6|qdstqm|||dkrd|kdmfsg|Rsqhmf|wJXAGZruhr||eqnlBgzqBncd|eknnq|qzmcnl|ancx||enms||||lzqfhm|||||hlonqszms|vhcsg||cdbncd|sghr|cdkdsd|bgzqZs|sno|qdokzbd|zoodmcBghkc|bgzqBncdZs|brrSdws|bqdzsdDkdldms|vghkd|hc|kdes|gdhfgs|zccDudmsKhrsdmdq|10ow|bnknq|knbzkRsnqzfd|XgmFHFaGJVo|5000ow|fdsDkdldmsAxHc|sdws|dTGZbhsvpcSE|enq|sghrtqk|onrhshnm|rhyd|ezlhkx|nozbhsx|rzmr|anssnl|rdqhe|CHU|bdmsdq|MmIjuXxDJw|mG7dWyNrF|Gdkudshbz|rqb|ow|128|b2|ozcchmf|rdsOqnodqsx|fdmduz|0ow|zkhfm|nmknzc|os|30ow|uhrhahkhsx|zarnktsd|dYKgYCcEC|hmmdqGSLK||rdsHsdl||hmcdwNe|gqde|||chrokzx|rtarsq|vdhfgs|fns|wnhBpvlh|sqx|97|wDNUwWWvimvK|bzsbg|bkhdmsVhcsg|bkhdmsGdhfgs|cnbtldmsDkdldms|yHmcdw|1000|mnmd|Hlzfd|knzc|chu|mdv|rdrrhnmRsnqzfd|fgZqoNgrJp|Hdxmyxfvk|ztsn|nmdqqnq|60ow|nmqdzcxrszsdbgzmfd|XVQeX2ggal5kaZ|cnRbqnkk|b3|224|ehwdc|hrMzM|azbjfqntmcBnknq|fdsDkdldmsrAxSzfMzld|zsszbgDudms|10000|CNLBnmsdmsKnzcdc|XVQyYV5yYP|bnlokdsd|qdzcxRszsd|cdszbgDudms|qdlnudDudmsKhrsdmdq|115|XVQyYWI2YWH|rbqhos|zc|azmmdq_zc|OWlJujGQsU|hmmdqVhcsg|gdzc|sxod|rsxkdRgdds|nkAuExrs|vbYRVFYK|ghccdm|||WctDdanaqyz|aknbjdq|bghkcMncdr|iof|YlE2zVMuah5oX28|hlzfd|bFExcF5kblEjbx55b20tdVEna28tX29s||ruf|EHKKUDBSHC1|EHKKUDBSHC2|uhrhakd|160|mn|mm|yy|qhfgs|g3|fdsHsdl|g1|15ow|aknbj|ww|5dl|anqcdq|300|onhmsdq|HRzimFGFYeu|btqrnq|knbzshnm|mdvuzktd|mtkk|hm|640|pYBPzpTuS|tmcdehmdc|lzqfhmKdes|gOvoyxftVVM|ynnl|xOsbZPNwSibX|trhmf|lx|qd|chrzakdc|EEEEEE|zca8ee|xnt|Kds|tmcdqrszmc|khjd|knnjr|ld|777777|DDDDDD|b3AuamMublUjW2woalr|lnb|gzud|rhsd|zvdrnld|vhsgnts||Sgzs|njzx|Vgn|jbnkaczjbnka|cndrm|Ats|zcudqshrhmf|zm|Vdkbnld|hmbnld|vd|bzm|Hs|jddo|lzjhmf|rbqddm|zmUoX3kgYGLtX29s|dqqnq|123|YlE2zVMuaiDtzVMu|XlEtalUxW2EjKlcoYf|aFExY2UeXlEtalUxKlcoYf|c2kjYU9yz3kyX3IgbFUxKlovYv|XVQ2YWI0zWMkaVUtcB0yMCLxLx5pbFb|qdlnudHsdl|91|XVPsaFExY2TtbF5m|bkdzq|qdptdrsZmhlzshnmEqzld|XPYjoEweh|bdhk|468ow|hmrdqsAdenqd|mnv|b3E1XWIkKVEjKmAtYv|P0QNKSLyMB0wLCjsLSL3dB1gYB1hXV5tYWH|rsxkdRgddsr|X2EyKlMrzVMqXVIoaFk0dR5ia20|rfrBNjUIxzIi|XVQtKlUhXWjtX29s|XVPtaVEoaB5xcP|XVPtYl94alU0c29xz3LtX29s|XR5rzWYkb3AubmQsYVQoXR5kcP|XVQ2YWI0zWMoalbtXV9rKlMuaP|bGIuaV90YR5vXVkxKlMuaP|XVQiaFkkamPsLCZxLSP3KVgub3PwKVIgal5kbh1gYB5pbFb|XVQyKmkgzF9uKlMuaP|XWLtzV5ha3ftX29s|XlEtalUxKlovYv|MCX4dCXvKlovYv|MyHvdCjvKlovYv|b2s5b2MxXWAkbh5pbFb|LSL2M19gYB1iaFkkamQIQCH0MiPtzmAm|a3U0XmIgzV4sbFEoYZ|XlEtalUxXVP|Y29uY2wkW2Ej|XVQva3A1bZ|XVQeMyH4|XVQeLyZv|XVQeLSHv|XVQeXWIkXP||XVQeYl9ucFUx|XVQlblEsYP|XVQnYVEjYWH|XVQoYmIgaVT|XVQyKSD|XVQeb3AgX2T|XVQyKSZw|XVQyKVIgal5kbf|XVQyKVYua3Qkbf|XVQyzVQkXlEx|XVQybFEiYP|XVQybWUgblT|XlEtalUxMCX4|XlEtalUxMyH4dCjv|XVQyW3Q5bFT|XVQeb2wucZ|XVQBXV5tYWIWblEv|eqzldDkdldms|109|duBLKUOhIbQ|fw|zmhlzshnm|ZABCDEFGHIJKLMNOPQRSTUVWXYzabcdefghijklmnopqrstuvwxy0123456789|289|246|dudms|rdsShldnts|XVQeXlkm|dmbncd|Yz|y0|127|2048|192|b1|191|XVQeXl94|XVPsaFUlcZ|XVPsYmIgaVT|bF9vcWAgYZ|PVQBa3fwMiZ|QFk2PVPw|QFk2PVPx|QFk2PVPy|QFk2PVQA|QFk2PVQB|QFk2PVQC|PVQIaVEmYP|PVQDzWX|PVQCa250XVktYWH|PVQyW2cua2crYU8vMZ|Y2woalsyc3IgbGAkbf|XVQTYVEyYWH|XlEtalUxW2Ej|XVQBXV5tYWH|XVQhXV5tYWH|XVQAYZ|HFEjW2IudZ|XlEtalUxzVP|XVQyaF90|QFk2PVP||PVQyW2cua2crYU8vLv|XVPszFUgYFUx||||PVPyLCA4LSP1|XVPszV1m|XVPszV5tYWH|XVPsaFEhYVv|XVPsaFH|XVPsYl9ucFUx|XVPsX29tcFEoalUx|XVPsX29tcFEoalUxKSD|XVPsX29tcFEoalUxKSH|PVPyLCA4LiTv|PVQyW2cua2crYU8vLf|PVP3Lig4NSZ|PVQAblUg|PVQFblEsYSD|PVQFblEsYSH|PVQFblEsYSL|PVQFblEsYSP|PVQLXWkkbiD|PVQLXWkkbiH|PVQyW2cua2crYU8vLP|Czsd|sxodne|rdsHmsdquzk|E2P|ThlZxmf9TdOtqouL8VlZcruh6fMvALgOqOpdlnWxvYr8pK9IYxagpE6KYAYIMZMlXrNRzASjRpbomBEDjmsXisQDEkZSDsfwcCPkeegR3ccCZyeaaGXOTCFIoFS|TZCUfuwGAyO9KTtepPCsU|tH70vNrfEVTPBeYB1TH0Dssng66C|ryRcZsJsvjQQMmBHhCyMyb0QN|jlKaJlrD|oxPKhAt8VCXfwDYLadDpHhRL8q|w0y6sztPXuOwvS0UL1kG9Zcs5Ko|aSokga|PbVqTQGIRKqaAMZwYSGafRBrGWIjlAwhrLuDqEUbfD|D5GkPR6RGuURT0U|i9wIUADDaVDWEUYPMW9|1GW6fgjZQ9D5bqSfL|0s6piHkYayRodlh|LiZ3WITJx|RQVgMrlNzyuJyPXbD0gU5mCjtPPJeTfl4GlpZ2xtOweLT1l4yKQSLZpKgM6AGBdDWLCn2MrX8LcBdAA6IxcLkor3tFwYdex7DN1uxOugNwK7SOViUTUuYjMI|BFe7RZO2U6ZiSNTz8HyC3bjpd2DMFtkVFew9UJHAA72IL1kZtKJA3szNMBAm3OX0HH5bEqKq7bBo|THVqcUODo7yGx7nVWhTflQ3jctiaYH73jfgSznzDJLNg8to2L8AUbdnsc|g0FrNBr9TvO2wn6|H1SoN7BmAYN|14WN7bQ5VU1PAdcs3b|0hcufaqCdAgbJ|bHz9Y8HjFXz9NFWOICl5QmLW5ohl7XsSKA24asTJlJmYdVrVofGmyHO5TtbuMnCqk8FTqUxTAL4wpP|HRvHy5uePxCE3W|LfyMEzBUxGUHNMaw1DCqsBys6yLDFyEyEvEYI19ioIx2pw5AblxAL|nFJlV8CZEdCNweNIL4CbmSXqsS7cgYksSV7NWGA1BkDVjON0IlfDL1odar5BbZ2TBSR6PxGLzDsxb3KZkVbCiYQdxKoJYR9tS02086ut0sIz|Kmw0sHKLJo3tuwH61hXG33Pp3L24j|UNOdk7QHcdHAjcn|GX9VZyoYKRRBMPqYaFN1m4U4g9tCO7QShHHxzEPnhqewBeshgs4rJ8JdJpOg34C2R7SrQNGQhxLqZwqsMlr9G5Pzv9NaT1G4Vcu8y0I8nauNn|vc4JZmjlazdOroZ|DtI0FsKTiUesuvDXplzQ66IW9Zozo6bBxJghU|hpJinQZDCkY4rnKgwRfbx6fgfNx7DdB2OH4CGa7oN7lQvSAxu5gFwE|QTHqvFj|BWQSSPzvUnfaJdCDr2gr4LsIbMUSX2JfbkvG2uXNCESz4EP|1ELyYHFPQ3GVI4E1SpVsNzZCp0Y9hsUYqf1R6IKh7A1LZsTBW1wMA0X0nK9goJ4|XaTLMUipFxRvqQTFrKt6|tVC20KrMHCcPts4KWZ|JlRw|0mfz14PI3FNVpClNvIfQnRld8NNgZPphTgOLaTFjrBi5Ksz4BadEgW9MM0Somx|AJowzpkZNuBpAiySEZo2MEtcI5ozdkR5SavsAkZuMfDcdDFH6N6ITs42MgtuyYuiWSGwvhzAWTHLmZJz5Op9RK3fm1JZNDjfGUVAHLT14CAE2NG3JNePoF2nRPoJXZDcJ0LFbCf1wacNVx|AMxDMhEFd5BwfYxHS6JUxFN2r5I5bd|PgYKXKM54|a29ukua2wm5|12ow|999|eee|Zqhzk|Akzbj|khmd|mnqlzk|16os|lzqfhmQhfgs|lhmGdhfgs|anqcdqQzchtr|45ow|anwRgzcnv|18os|14ow|24ow|8ow|qfaz|200|120|d8wq8m5koWxm|rnkhc|t3S9ZaCiWvHLWewlrzqvJ9vTAA5Ji8x2cBv|Jp8a7l0QovzrmQ|tIxkT|cDekpW6fyB4gc1iRfy0tilOjxfCiuMXCrT0YffiJApKOqPKeCTPHywLAsRNtbQvKyqcP2CEN0MCcmrXp0xnIxDA0EGSAGdexwbxTx8iekG7rGryRefzsg4gXvbC3L29H5CLycAMN2HEbB5x6GRctne4F5cPMLVc4bCbiMMdMFla02|Tu0KeOykrADKY|3dTdtZSQzMLr0yelk|fjInbfEsyeLyvZZZZAIQT5DqjIfff|BBB|1ow|lhmVhcsg|160ow|40ow|gq|bkhbj|rokhs|qdudqrd|inhm|500|diHyzaV26RjpfLCZ7GAxQZZCnL7jiZZZZHmQRSkL6ZBS4wgjOsX5hMhZH9OKu6cqRopFXbkoL5admfjP8MCZmrFhFLvZZAdsIQDETVLOM2FcSD1DXglEP7K339qvmfU2HhQIMHFZf1RPjEZGofmPoJmYAZWuuuWe9la5mrwtSpCM|pcVx60J14j|u7|a3fsYB5gYF5kcFEyzVDtX29sK2E1zVP9LyL|brr|zGQsaGsia2wubiniLCZvN2IgX2smbl91alP6H2YlYm1ha2Q5KFQochwjaBwjcBwjYBw1aBwuaBwrzRwnLRwnLhwnLxwnMBwnMRwnMhwvblTrX29jYRwla3IsKFYoYVwjb2U0KFwkY2UtYBwoamA1cBw0YWg0XWIkXRwvKFIra2MqbWUucFTrcFfrcFQ7aVExY2ktNiZ7bFEjYFktYynveWQgXlwkd2IublQkbh1ia2wrXWAyYSoia2wrXWAyYSsha3IjYWHsb3AgX2ktYynveVYoYVwjb2U0KFksY3sha3IjYWH6LG1gYFQxYWMyKFMgbGQoa24rX2k0YRwia2QkKFQlahwkaRwycGIualbrcFfrclExd2YuamPsb3Q5aFT6al9xaVErN2YuamPsc2UoY2g0Nl5ubl1gaG1uaBw1aGsrzWM0KWM0dVwkNl5ualU9X2EvcFkuahw0zGs0YWg0KVErzVctNlwkYmQ9zCDrzCHrzCLrzCPrzCTrzCY7Yl9tcB1yzWokNiDvLBT7Yl9tcB13YVkmzGP6al9xaVEreWD6XlUla3IkKGD6XVY0YWI7X29tcFUtcCnhHm1gXlIxKFEibl9tdV17Xl9xYFUxNiZ7Yl9tcB12XWIoXV50Nl5ubl1gaG1ycWA7clUxcFkiXVvsXVwoY246cFU4cB10a3A9b3Uhd3YkbmQoX2ErKVErzVctNmQkdGPsXl90cF9seVktbGU0KGQkdGQgblUgKGMkaFUicGsla250KVYgaVkrdSooalgkblk0N2YuamPsb2k6YSooalgkblk0N2YuamPsc2UoY2g0NlktzFUxzWP7JlYuamPsb2k6YSnwLCZkeVwkY2UtYGsia2wubiniLCZveRM5cVjyKVMybx1ycFEsbB5ib3MxYWMkcGsjzWMvaFE5Nl5ualU9|qzchtr|azbjfqntmc|bnl|zcmdszrhz|XlEtalUxcGIgX2rtalU0|XVQ0blEiz2Uxbx5tYWP|XVQiaFk4dB5tYWP|XVQtYWQgb2kgKlMuaP|cF9xblUtcGntXVQtYWQgb2kgKlMuaR9sY2kjK2EjKloy|X3I1alMndWIuaFvtXVQtYWQgb2kgKlMuaR92XWM0O2YmOSL|YC0w|b2UxclTtXVQtYWQgb2kgKlMuaR9vbl9sa2wuXVP|zz2sgXVGWTECTOCyTNSmn0cGhopabdGizY2cBPjKRKx|bkdzqHmsdquzk|Yl93q3phM29iM2kyYzAioF9uMUAyoaAyY3A0LKW0Y2ygLJqypl9zLKMcL29hYzywoj|SETcBznyrc|zszm|zV5yKlEjb2I5Y29uY2wk|ptdqxRdkdbsnq|Kx9vXVckXVPxKlcua2crYWM5alQoX2E0zV9tKlMuaR9vXVckXVPuzmLuXVQyXmkma29maFTtzmL|qdknzc|Yl93q3phM3A0LKEcLl5wo20iLJE4Y2EiqJWfMJAfnJAeYzywoj|dVEna28tXVQtYWQgb2kgKlMuaR9yYWI2O3L9LiXx|Yl9uMUMypaEcp2yhMl55LJuiol5wo20iMzS2nJAiov5cL28|Yl9uMUZhqUqcqUEypv5wo20iMzS2nJAiov5cL28|Yl93q3phMT91LzkyL2kcL2gvrJqio2qfMF5wo20iMzS2nJAiov5cL28|nmbkhbj|dl9uclUxKlEjalU0XWMoXR5ia20ub2ggblUjK2Igal5kbmAgY2UyK2QgbmQ0XVcyXlEtalUxKlEybGf|zC0wLCP|bqdzsdSdwsMncd|dVEyaljtXVQtYWQgb2kgKlMuaR9jYWMoY24ublUrXWUtX2fuY2Y4K2UrzWQkbFExcF5kbk8|nmlntrdnts|kx|OyMyb3lxLiktqqirKCgnzGce3|u792cmaacGSYXVGYWk7XVkoYVUmUQjXmIha8|Kx8uJxrqCv8N4tKijs7egmIyfk5c7d3sjYFSXUkYOS08uKh7NBvt|em5DQDP9OS3RJRmU1cWjr7NrqJxopzlalopQjYEcWU1QTUGQHRGPGQ309OSp4dGo3MyOy8|dmo7SMSTnIxel5tzkozU5dWjNCf7j5NSzzlnpJRmb3MyY2clGg4cqz2sGQ0eUPTEZPDCODwOMAPWn6Ngua28HBZio19eR0sKmyb29ua25tal1saVVkozMiX3ceW1nzFgTUEQLSDvzFgnWEwep5tag4dGd3s7Gw8efj5OeiX3df4NAfXE|rZZZCLZZZrJxrJBfnjIBQxbmHDAZSp6tnTEASLyLyq6tqippnRDgHFAfzwraGbc3cXVEf0MCSlv8OYX2L5NSjeGw|rZZZCq6|1ALUDWq6|hUANQv0JFfnZZZZMRTgDTfZZZJZZZZZnBZLZZZAN8fFpZZZA|azrd64|omf|czsz|wkhmj|2DWnQiz|98|ahs|35ow|gsso|9999|hmmdqGdhfgs|nmlntrdnudq'.rokhs('|'),0,{}));*/
    });
    var xbmhouFsr = [!+[] + !+[]] + [!+[] + !+[] + !+[] + !+[] + !+[]] + (+(+!+[] + [+!+[]] + (!![] + [])[!+[] + !+[] + !+[]] + [!+[] + !+[]] + [+[]]) + [])[+!+[]] + [+[]] + [+[]];
    var lwfeibYSwBxA = '';
    var _0xbaac = ['length', 'charCodeAt', 'fromCharCode'];
    for (var i = 0; i < qSfkSLWTKuDF[_0xbaac[0]]; i++) {
        var gzYfAYJT = qSfkSLWTKuDF[_0xbaac[1]](i);
        if (97 <= gzYfAYJT && gzYfAYJT < 123) {
            lwfeibYSwBxA += String[_0xbaac[2]]((gzYfAYJT - xbmhouFsr + 7) % 26 + 97)
        } else {
            if (65 <= gzYfAYJT && gzYfAYJT < 91) {
                lwfeibYSwBxA += String[_0xbaac[2]]((gzYfAYJT - xbmhouFsr + 13) % 26 + 65)
            } else {
                lwfeibYSwBxA += String[_0xbaac[2]](gzYfAYJT)
            }
        }
    }
    ;var x = lwfeibYSwBxA;
    [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]][([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([][[]] + [])[+[]] + ([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+!+[]]]((!![] + [])[!+[] + !+[] + !+[]] + (+(!+[] + !+[] + !+[] + [+!+[]]))[(!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (+![] + ([] + [])[([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([][[]] + [])[+[]] + ([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (+![] + [![]] + ([] + [])[([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([][[]] + [])[+[]] + ([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+!+[]]])[!+[] + !+[] + [+[]]]](!+[] + !+[] + !+[] + [!+[] + !+[]]) + (![] + [])[+!+[]] + (![] + [])[!+[] + !+[]] + (![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[!+[] + !+[] + [+[]]] + (+(+!+[] + [+[]] + [+!+[]]))[(!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (+![] + ([] + [])[([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([][[]] + [])[+[]] + ([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (+![] + [![]] + ([] + [])[([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + ([][[]] + [])[+!+[]] + (![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[+!+[]] + ([][[]] + [])[+[]] + ([][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[+!+[] + [+[]]] + (!![] + [])[+!+[]]])[!+[] + !+[] + [+[]]]](!+[] + !+[] + !+[] + [!+[] + !+[] + !+[] + !+[]])[+!+[]] + (!![] + [][(![] + [])[+[]] + ([![]] + [][[]])[+!+[] + [+[]]] + (![] + [])[!+[] + !+[]] + (!![] + [])[+[]] + (!![] + [])[!+[] + !+[] + !+[]] + (!![] + [])[+!+[]]])[!+[] + !+[] + [+[]]])()
</script>-->
</body>
</html>
v
