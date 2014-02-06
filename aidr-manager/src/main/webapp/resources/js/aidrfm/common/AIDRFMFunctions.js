Ext.define('AIDRFM.common.AIDRFMFunctions', {

    initMessageContainer: function (){
        msgCt = Ext.DomHelper.insertFirst(document.body, {id:'msg-div'}, true);
        msgCt.setStyle('position', 'absolute');
        msgCt.setStyle('z-index', 99999);
        msgCt.setWidth(300);
    },

    setAlert: function (status, msg) {
        var message = '<ul>';
        if (Ext.isArray(msg)) {
            Ext.each(msg, function (ms) {
                message += '<li>' + ms + '</li>';
            })
        } else {
            message = '<li>' + msg + '</li>';
        }
        message += '</ul>';

        // add some smarts to msg's duration (div by 13.3 between 3 & 9 seconds)
        var delay = msg.length / 13.3;
        if (delay < 3) {
            delay = 3;
        }
        else if (delay > 9) {
            delay = 9;
        }
        delay = delay * 1000;

        msgCt.alignTo(document, 't-t');
        Ext.DomHelper.append(msgCt, {html: this.buildMessageBox(status, message)}, true).slideIn('t').ghost("t", {delay: delay, remove: true});
    },

    buildMessageBox : function(title, msg) {
        return [
            '<div class="app-msg">',
            '<div class="x-box-tl"><div class="x-box-tr"><div class="x-box-tc"></div></div></div>',
            '<div class="x-box-ml"><div class="x-box-mr"><div class="x-box-mc"><h3 class="x-icon-text icon-status-' + title + '">', title, '</h3>', msg, '</div></div></div>',
            '<div class="x-box-bl"><div class="x-box-br"><div class="x-box-bc"></div></div></div>',
            '</div>'
        ].join('');
    },

    getMask: function (showMessage, msg) {
        if (showMessage) {
            if (!msg) {
                msg = 'Loading ...';
            }
        }
        if (this.maskScreen == null) {
            this.maskScreen = new Ext.LoadMask(Ext.getBody(), {msg: msg});
        } else {
            this.maskScreen.msg = msg;
        }
        return this.maskScreen;
    },

    mandatoryFieldsEntered: function () {
        var me = this;

        var isValid = true;
        var form = Ext.getCmp('collectionForm').getForm();
        if (!form.findField('code').getValue()) {
            form.findField('code').markInvalid(['Collection Code is mandatory']);
            AIDRFMFunctions.setAlert('Error', 'Collection Code is mandatory');
            isValid = false;
        }
        if (form.findField('code').getValue() && form.findField('code').getValue().length > 64) {
            form.findField('code').markInvalid(['The maximum length for Collection Code field is 64']);
            AIDRFMFunctions.setAlert('Error', 'The maximum length for Collection Code field is 64');
            isValid = false;
        }
        if (!form.findField('name').getValue()) {
            form.findField('name').markInvalid(['Collection Name is mandatory']);
            AIDRFMFunctions.setAlert('Error', 'Collection Name is mandatory');
            isValid = false;
        }
        if (!(form.findField('track').getValue() || form.findField('geo').getValue() || form.findField('follow').getValue())) {
            AIDRFMFunctions.setAlert('Error', 'One of Keywords, Geo or Follow field is mandatory');
            isValid = false;
        }
        if (form.findField('track').getValue()) {
            var value = form.findField('track').getValue(),
                keywords = value.split(","),
                errors = [],
                isKeywordValid = true;
            Ext.Array.each(keywords, function(v) {
                if (v.length > 60) {
                    if (isKeywordValid){
                        errors.push('Each keywords should not exceed 60 chars.');
                    }
                    errors.push('<b>' + v + '</b> has more than 60 chars');
                    isKeywordValid = false;
                }
            });
            if (!isKeywordValid) {
                isValid = isKeywordValid;
                AIDRFMFunctions.setAlert('Error', errors);
            }
        }
        return isValid;
    },

    getQueryParam: function (name) {
        name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
        var regexS = "[\\?&]" + name + "=([^&#]*)";
        var regex = new RegExp(regexS);
        var results = regex.exec(window.location.href);
        if (results == null)
            return null;
        else
            return results[1];
    },

    getStatusWithStyle: function(raw) {
        var statusText = '';
        if (raw == 'RUNNING') {
            statusText = "<b class='greenInfo'> RUNNING </b>";
        } else if (raw == 'INITIALIZING') {
            statusText = "<b class='blueInfo'> INITIALIZING </b>";
        } else if (raw == 'STOPPED' || raw == 'FATAL-ERROR') {
            statusText = "<b class='redInfo'>" + raw + " </b>";
        }  else if (raw == 'NOT_RUNNING') {
            statusText = "<b class='warningFont'>" + raw + " </b>" + ' (Click on "Start" to start this collection.)';
        } else {
            statusText = "<b class='warningFont'>" + raw + " </b>";
        }
        return statusText;
    },

    getAucNumberWithColors: function(r) {
        var style;
        if (r){
            if (r < 0.6){
                style = 'redInfo';
            } else if (r <= 0.8){
                style = 'warningFont';
            } else {
                style = 'greenInfo';
            }
        } else {
            r = 0;
            style = 'redInfo';
        }
        return '<span class="' + style + '">' + r.toFixed(2) + '</span>';
    }

});

AIDRFMFunctions = new AIDRFM.common.AIDRFMFunctions();

NA_CATEGORY_NAME = 'N/A: does not apply, or cannot judge';

Number.prototype.format = function(n, x) {
    var re = '(\\d)(?=(\\d{' + (x || 3) + '})+' + (n > 0 ? '\\.' : '$') + ')';
    return this.toFixed(Math.max(0, ~~n)).replace(new RegExp(re, 'g'), '$1,');
};

if(!String.linkify) {
    String.prototype.linkify = function() {

        // http://, https://, ftp://
        var urlPattern = /\b(?:https?|ftp):\/\/[a-z0-9-+&@#\/%?=~_|!:,.;]*[a-z0-9-+&@#\/%=~_|]/gim;

        // www. sans http:// or https://
        var pseudoUrlPattern = /(^|[^\/])(www\.[\S]+(\b|$))/gim;

        // Email addresses
        var emailAddressPattern = /\w+@[a-zA-Z_]+?(?:\.[a-zA-Z]{2,6})+/gim;

        return this
            .replace(urlPattern, '<a href="$&" target="_blank">$&</a>')
            .replace(pseudoUrlPattern, '$1<a href="http://$2" target="_blank">$2</a>')
            .replace(emailAddressPattern, '<a href="mailto:$&">$&</a>');
    };
}

Ext.Ajax.on('requestexception', function (conn, response, options) {
    if (response.status == 901) {
        document.location.href = BASE_URL + '/index.jsp'
    } else{
        AIDRFMFunctions.setAlert("Error", "System is down or under maintenance. For further inquiries please contact admin.");
    }
});