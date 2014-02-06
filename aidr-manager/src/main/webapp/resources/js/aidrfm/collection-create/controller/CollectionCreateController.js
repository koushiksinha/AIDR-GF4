Ext.define('AIDRFM.collection-create.controller.CollectionCreateController', {
    extend: 'Ext.app.Controller',

    views: [
        'CollectionCreatePanel'
    ],

    init: function () {
        this.control({

            'collection-create': {
                afterrender: this.afterRenderCollectionCreateView
            },

            "#collectionNameInfo": {
                render: function (infoPanel, eOpts) {
                    var tip = Ext.create('Ext.tip.ToolTip', {
                        trackMouse: true,
                        html: 'Give a name to your collection. For example, Hurricane Sandy, Earthquake Japan.',
                        target: infoPanel.el,
                        dismissDelay: 0
                    });
                }
            },

            "#collectionCodeInfo": {
                render: function (infoPanel, eOpts) {
                    var tip = Ext.create('Ext.tip.ToolTip', {
                        trackMouse: true,
                        html: ' Collection code consists of alpha-numeric short code name to a collection. ' +
                            'Spaces are not allowed in the code name. For example, Sandy2012, EQJapan2013 are valid code names',
                        target: infoPanel.el,
                        dismissDelay: 0
                    });
                }
            },

            "#collectionkeywordsInfo": {
                render: function (infoPanel, eOpts) {
                    var tip = Ext.create('Ext.tip.ToolTip', {
                        trackMouse: true,
                        html: 'This field represents comma separated keywords to filter the Twitter stream.<br>' +
                            'General rules:<br>' +
                            '- Not case-sensitive ("bridge" matches "Bridge").<br>' +
                            '- Whole words match ("bridge" does not match "damagedbridge").<br><br>' +
                            'Multi-word queries<br>' +
                            '- If you include two or more words on a query, all of them must be present in the tweet ("Brooklin bridge" does not match a tweet that does not contain "Brooklin" or does not contain "bridge")<br>' +
                            '- The words does not need to be consecutive or in that order ("Brooklin bridge" will match "the bridge to Brooklin")<br><br>' +
                            'Queries with or without hashtags:<br>' +
                            '- If you don\'t include \'#\', you also match hashtags ("bridge" matches "#bridge")<br>' +
                            '- If you do include \'#\', you only match hashtags ("#bridge" does not match "bridge")<br>',
                        target: infoPanel.el,
                        dismissDelay: 0
                    });
                }
            },

            "#collectionGeoInfo": {
                render: function (infoPanel, eOpts) {
                    var tip = Ext.create('Ext.tip.ToolTip', {
                        trackMouse: true,
                        html: 'This field represents a comma-separated pairs of longitude and latitude. A valid geo location represents a bounding box with southwest corner of the box coming first. Note that if you specify a geographical region, all messages posted from within that region will be collected, independently of whether they contain the keywords or not.',
                        target: infoPanel.el,
                        dismissDelay: 0
                    });
                }
            },

            "#collectionFollowInfo": {
                render: function (infoPanel, eOpts) {
                    var tip = Ext.create('Ext.tip.ToolTip', {
                        trackMouse: true,
                        html: "Follow represents a comma-separated list of twitter users IDs to be followed. A valid twitter user id must be in the numeric format.",
                        target: infoPanel.el,
                        dismissDelay: 0
                    });
                }
            },

            "#collectionLangInfo": {
                render: function (infoPanel, eOpts) {
                    var tip = Ext.create('Ext.tip.ToolTip', {
                        trackMouse: true,
                        html: "This field is used to set a comma separated list of language codes to filter results only to the specified languages. The language codes must be a valid BCP 47 language identifier. Language filter is not a mandatory field, but it is strongly recommended if you intend to use the automatic tagger.",
                        target: infoPanel.el,
                        dismissDelay: 0
                    });
                }
            },

            "#collectionCancelCreate": {
                click: function (btn, e, eOpts) {
                    document.location.href = BASE_URL + '/protected/home';
                }
            },

            "#collectionCreate": {
                click: function (btn, e, eOpts) {
                    CollectionCreateController.isExist();
                }
            },

            "#nameTextField": {
                blur: function (field, eOpts) {
                    CollectionCreateController.generateCollectionCode(field.getValue());
                }
            }

        });
    },

    afterRenderCollectionCreateView: function (component, eOpts) {
        AIDRFMFunctions.initMessageContainer();
        this.CollectionCreateComponent = component;
        CollectionCreateController = this;
    },

    saveCollection: function () {
        var me = this;

        var mask = AIDRFMFunctions.getMask(true, 'Saving collection ...');
        mask.show();

        if (AIDRFMFunctions.mandatoryFieldsEntered()) {
            var form = Ext.getCmp('collectionForm').getForm();
            Ext.Ajax.request({
                url: 'collection/save.action',
                method: 'POST',
                params: {
                    name: Ext.String.trim( form.findField('name').getValue() ),
                    code: Ext.String.trim( form.findField('code').getValue() ),
                    track: Ext.String.trim( form.findField('track').getValue() ),
                    follow: Ext.String.trim( form.findField('follow').getValue() ),
                    geo: Ext.String.trim( form.findField('geo').getValue() ),
                    langFilters: form.findField('langFilters').getValue()
                },
                headers: {
                    'Accept': 'application/json'
                },
                success: function (response) {
                    AIDRFMFunctions.setAlert("Collection Created", ["Collection created successfully.", "You will be redirected to Home screen."]);
                    mask.hide();

                    var maskRedirect = AIDRFMFunctions.getMask(true, 'Redirecting ...');
                    maskRedirect.show();

//                    wait for 3 sec to let user read information box
                    var isFirstRun = true;
                    Ext.TaskManager.start({
                        run: function () {
                            if (!isFirstRun) {
                                document.location.href = BASE_URL + '/protected/home';
                            }
                            isFirstRun = false;
                        },
                        interval: 3 * 1000
                    });
                }
            });
        } else {
            mask.hide();
        }
    },

    isExist: function () {
        var me = this;

        var form = Ext.getCmp('collectionForm').getForm();
        var code = form.findField('code');
        Ext.Ajax.request({
            url: 'collection/exist.action',
            method: 'GET',
            params: {
                code: code.getValue()
            },
            headers: {
                'Accept': 'application/json'
            },
            success: function (response) {
                var response = Ext.decode(response.responseText);
                if (response.data) {
                    AIDRFMFunctions.setAlert('Error', 'Collection Code already exist. Please select another code');
                    code.markInvalid("Collection Code already exist. Please select another code");
                } else {
                    me.saveCollection();
                }
            }
        });
    },

    generateCollectionCode: function(value) {
        var me = this;

        var currentCode = me.CollectionCreateComponent.codeE.getValue();
        if (currentCode != ''){
            return false;
        }

        var v = Ext.util.Format.trim(value);
        v = v.replace(/ /g, '_');
        v = Ext.util.Format.lowercase(v);

        var date = Ext.Date.format(new Date(), "Y-m-");
        date = Ext.util.Format.lowercase(date);

        var length = value.length;
        if (length > 56){
            length = 56;
        }

        var result = date + Ext.util.Format.substr(v, 0, length);
        me.isExistForGenerated(result);
    },

    isExistForGenerated: function (code, attempt) {
        var me = this;

        Ext.Ajax.request({
            url: 'collection/exist.action',
            method: 'GET',
            params: {
                code: code
            },
            headers: {
                'Accept': 'application/json'
            },
            success: function (response) {
                var response = Ext.decode(response.responseText);
                if (response.data) {
                    if (attempt) {
                        me.modifyGeneratedCode(code, attempt);
                    } else {
                        me.modifyGeneratedCode(code, 0);
                    }
                } else {
                    me.CollectionCreateComponent.codeE.setValue(code);
                }
            }
        });
    },

    modifyGeneratedCode: function(oldCode, attempt) {
        var me = this;

        var date = Ext.util.Format.substr(oldCode, oldCode.length - 7, oldCode.length),
            code = Ext.util.Format.substr(oldCode, 0, oldCode.length - 9);

        var result = code + '_' + attempt + date;
        me.isExistForGenerated(result, attempt + 1);
    }

});