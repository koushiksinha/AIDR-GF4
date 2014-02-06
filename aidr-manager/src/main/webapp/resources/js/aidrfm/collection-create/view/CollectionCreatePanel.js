Ext.require([
    'AIDRFM.common.AIDRFMFunctions',
    'AIDRFM.common.StandardLayout',
    'AIDRFM.common.Header',
    'AIDRFM.common.Footer'
]);

Ext.define('AIDRFM.collection-create.view.CollectionCreatePanel', {
    extend: 'AIDRFM.common.StandardLayout',
    alias: 'widget.collection-create',

    fieldDefaults: {
        labelAlign: 'left',
        msgTarget: 'side'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    initComponent: function () {
        var me = this;

        this.breadcrumbs = Ext.create('Ext.container.Container', {
            html: '<div class="bread-crumbs"><a href="home">Home</a></div>',
            margin: 0,
            padding: 0
        });

        this.descriptionL = Ext.create('Ext.form.Label', {
            flex: 1,
            text: 'Create New Collection',
            padding: '0 0 0 0',
            cls: 'header-h1 bold-text'
        });

        this.codeE = Ext.create('Ext.form.field.Text', {
            fieldLabel: 'Code',
            name: 'code',
            allowBlank: false,
            width: 370,
            emptyText: 'e.g., Sandy2012 or EQJapan2011',
            maxLength: 64,
            maxLengthText: 'The maximum length for this field is 64',
            maskRe: /[^ ]/
        });

        this.nameE = Ext.create('Ext.form.field.Text', {
            width: 370,
            fieldLabel: 'Name',
            id: 'nameTextField',
            name: 'name',
            allowBlank: false,
            emptyText: 'e.g., Hurricane Sandy'
        });

        this.keywordsE = Ext.create('Ext.form.field.TextArea', {
            fieldLabel: 'Keywords',
            name: 'track',
            allowBlank: true,
            maxLength: 24000,
            maxLengthText: 'The maximum length for this field is 400',
            flex: 1,
            rows: 4,
            emptyText: 'e.g., #sandy, #newyork,#joplin (max 400)'
        });

        this.geoE = Ext.create('Ext.form.field.Text', {
            fieldLabel: 'Geographical regions',
            labelWidth: 130,
            name: 'geo',
            flex: 1,
            maxLength: 25,
            maxLengthText: 'The maximum length for this field is 25',
            emptyText: 'e.g., 43.43, 22.44, 89.32, 56.43 (max 25)'
        });

        this.geoDescription = Ext.create('Ext.form.Label', {
            flex: 1,
            html: '<span class="redInfo">*</span> <a href="http://boundingbox.klokantech.com/" target="_blank">boundingbox.klokantech.com</a> ("Copy/paste CSV format of a boundingbox")',
            padding: '2 0 2 135'
        });

        this.followE = Ext.create('Ext.form.field.Text', {
            fieldLabel: 'Follow specific users',
            labelWidth: 130,
            name: 'follow',
            flex: 1,
            emptyText: 'e.g., 47423744, 53324456 (max 5000)'
        });

        this.langComboStore = Ext.create('Ext.data.ArrayStore', {
            autoDestroy: true,
            storeId: 'langComboStore',
            idIndex: 0,
            fields: [
                'name',
                'code'
            ],
            data: LANG
        });

        this.langCombo = Ext.create('Ext.form.ComboBox', {
            store: this.langComboStore,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'code',
            multiSelect: true,
            editable: false,
            fieldLabel: 'Language(s)',
            labelWidth: 100,
            name: 'langFilters',
            flex: 1,
            value: 'en',
            emptyText: 'e.g., en, ar, ja',
            tpl: '<tpl for=".">' +
                '<li role="option" class="x-boundlist-item">{name}</li>' +
                '<tpl if="xindex == 9"><hr/></tpl>' +
                '<tpl if="xindex == 56"><hr/>Other ...<hr/></tpl>' +
                '</tpl>'
        });

        this.configurationsL = Ext.create('Ext.form.Label', {
            flex: 1,
            text: 'Optional configuration',
            padding: '15 0 0 0',
            cls: 'header-h2'
        });

        this.saveButton = Ext.create('Ext.Button', {
            text: 'Save',
            margin: '0 10 0 10',
            cls: 'btn btn-green',
            id: 'collectionCreate'
        });

        this.cancelButton = Ext.create('Ext.Button', {
            text: 'Cancel',
            cls: 'btn btn-red',
            id: 'collectionCancelCreate'
        });

        this.editForm = Ext.create('Ext.form.Panel', {
            id: 'collectionForm',
            bodyCls: 'no-border',
            items: [
                {
                    xtype: 'container',
                    defaults: {
                        width: 350,
                        labelWidth: 120
                    },
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            margin: '5 0',
                            items: [
                                this.nameE,
                                {
                                    border: false,
                                    bodyStyle: 'background:none',
                                    html: '<img src="/AIDRFetchManager/resources/img/info.png"/>',
                                    height: 22,
                                    width: 22,
                                    id: 'collectionNameInfo'
                                }
                            ]
                        },
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            margin: '5 0',
                            items: [
                                this.codeE,
                                {
                                    border: false,
                                    bodyStyle: 'background:none',
                                    html: '<img src="/AIDRFetchManager/resources/img/info.png"/>',
                                    height: 22,
                                    width: 22,
                                    id: 'collectionCodeInfo'
                                }
                            ]
                        },
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            margin: '5 0',
                            items: [
                                this.keywordsE,
                                {
                                    border: false,
                                    bodyStyle: 'background:none',
                                    html: '<img src="/AIDRFetchManager/resources/img/info.png"/>',
                                    height: 22,
                                    width: 22,
                                    id: 'collectionkeywordsInfo'
                                }
                            ]
                        },
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            margin: '5 0 0 0',
                            padding: '0 0 20 0',
                            cls: 'bordered-bottom',
                            items: [
                                this.langCombo,
                                {
                                    border: false,
                                    bodyStyle: 'background:none',
                                    html: '<img src="/AIDRFetchManager/resources/img/info.png"/>',
                                    height: 22,
                                    width: 22,
                                    id: 'collectionLangInfo'
                                }
                            ]
                        },
                        this.configurationsL,
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            margin: '0 0 5 0',
                            items: [
                                this.geoE,
                                {
                                    border: false,
                                    bodyStyle: 'background:none',
                                    html: '<img src="/AIDRFetchManager/resources/img/info.png"/>',
                                    height: 22,
                                    width: 22,
                                    id: 'collectionGeoInfo'

                                }
                            ]
                        },
                        this.geoDescription,
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            margin: '5 0',
                            items: [
                                this.followE,
                                {
                                    border: false,
                                    bodyStyle: 'background:none',
                                    html: '<img src="/AIDRFetchManager/resources/img/info.png"/>',
                                    height: 22,
                                    width: 22,
                                    id: 'collectionFollowInfo'
                                }
                            ]
                        },
                        {
                            xtype: 'container',
                            layout: 'hbox',
                            padding: '15 0 0 0',
                            items: [
                                this.saveButton,
                                this.cancelButton
                            ]
                        }
                    ]
                }
            ]
        });

        this.items = [
            this.breadcrumbs,
            {
                xtype: 'container',
                margin: '5 0 0 0',
                html: '<div class="horizontalLine"></div>'
            },
            this.descriptionL,
            {
                xtype: 'container',
                margin: '5 0 15 0',
                html: '<div class="horizontalLine"></div>'
            },
            this.editForm
        ];

        this.callParent(arguments);
    }

});