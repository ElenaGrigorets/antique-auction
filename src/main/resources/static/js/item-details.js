$(document).ready(function () {
    if (dateStringValue.length !== 0) {
        let countDownDate = new Date(dateStringValue).getTime();
        let x = setInterval(function () {
            let now = new Date().getTime();
            let distance = countDownDate - now;
            let days = Math.floor(distance / (1000 * 60 * 60 * 24));
            let hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            let minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
            let seconds = Math.floor((distance % (1000 * 60)) / 1000);
            let countDownTimer = document.getElementById("countDownTimer");
            countDownTimer.innerHTML = days + "d " + hours + "h "
                + minutes + "m " + seconds + "s ";

            $.ajax({
                type: 'GET',
                url: '/item/status/get/' + itemId,

                success: function (data) {
                    $('#showCurrentPrice').html(data.currentPrice);
                    if (currentPrice != data.currentPrice) {
                        currentPrice = data.currentPrice;
                        $('#currentPrice').val(data.currentPrice);
                    }
                    let html = '';
                    $.each(data.bidPrices, function (index, value) {
                        html = html + '<div><div>' + value + '</div></div>';
                    });
                    $('.bidHistory').html(html);
                    $('#itemName').html(data.itemName);
                    $('#description-field').html(data.itemDescription);
                    dateStringValue = data.dateStringValue;
                    countDownDate = new Date(dateStringValue).getTime()
                },
            });
            if (distance < 0) {
                clearInterval(x);

                $.ajax({
                    type: 'GET',
                    url: '/item/status/get/' + itemId,

                    success: function (data) {
                        $('#bidSection').html('<p class="won">Item was awarded to <span>'
                            + data.finalPriceUserName + '</span> with final price: <span>'
                            + data.finalPrice + '</span></p>');
                    },
                });
                countDownTimer.className = "error-msg";
                countDownTimer.innerHTML = "Auction is over!";
            }
        }, 1000);
    }
});
