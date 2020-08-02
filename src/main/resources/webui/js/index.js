let rlmmVersion = null;
let currentContentDiv = 'contentHome';

$(function () {
    // onclick handler for navbar
    $('#navBar .navList div.anchor').on('click', function () {
        const $this = $(this);
        if ($this.hasClass('current')) {
            return;
        }
        currentContentDiv = $this.attr('data-divID');
        const $contentDiv = $('#' + currentContentDiv);

        $('#navBar .navList div.anchor.current, div.content.current').removeClass('current');
        $this.addClass('current');
        $contentDiv.addClass('current');

        updateSetupHints();
    });

    // clock
    setInterval(function () {
        const now = new Date();

        let i = now.getHours();
        let s = (i >= 10 ? i : '0' + i) + ":";
        i = now.getMinutes();
        s += (i >= 10 ? i : '0' + i) + ":";
        i = now.getSeconds();
        s += i >= 10 ? i : '0' + i;

        $('#localTime').html(s);
    }, 1000);

    // fetch version
    makeRequest('/api/getVersion', null, function(version) {
        rlmmVersion = version;
        $('#versionDiv').html('Version: ' + version);
    });
});

function makeRequest(url, body, successCallback, errorCallback, timeoutMillis) {
    let request = new XMLHttpRequest();
    request.onload = function() {
        if(200 <= this.status && this.status <= 299) {
            if(successCallback) {
                successCallback(this.responseText);
            }
        } else {
            if(errorCallback) {
                errorCallback();
            }
        }
    };
    request.onerror = errorCallback;
    request.ontimeout = errorCallback;
    request.open(body ? "POST" : "GET", url, true);
    request.timeout = timeoutMillis ? timeoutMillis : 1000;
    if(body) {
        request.send(body);
    } else {
        request.send();
    }
}

/**
 * @param a {*}
 * @param b {*}
 * @returns {*} the first argument if it is truthy, or else the second argument.
 */
function coalesce(a, b) {
    return a ? a : b;
}