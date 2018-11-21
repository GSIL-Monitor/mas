var url = GlobalUrl;
var curRow = {};
$(function () {
    initTable();
});

function saveDate() {
    var treeObj = $.fn.zTree.getZTreeObj("menuTree");
    var nodes = treeObj.getCheckedNodes(true);
    var str = '';
    if (nodes != null && nodes.length != 0) {
        for (var i = 0; i < nodes.length; i++) {
            if (i == (nodes.length - 1)) {
                str += nodes[i].id;
            } else {
                str += nodes[i].id + ",";
            }
        }
    }
    var id = document.getElementById("id").value;
    var tableId = document.getElementById("tableId").value;
    var type = document.getElementsByName("userType");
    if (type[0].checked == true) {
        type = type[0].value;
    } else {
        type = type[1].value;
    }
    $('#tb_user').bootstrapTable('updateRow', {
        index: tableId,
        row: {
            code: str
        }
    });
    $.ajax({
        url: url + "/acl/updateUserAcl",
        type: "post",
        dataType: 'jsonp',
        jsonp: "jsoncallback",
        jsonpCallback: 'callback',
        data: {
            id: id,
            type: type,
            code: str
        },
        success: function (res) {
            if (res == 1) {
                document.getElementById('addWindow').style.display = 'none';
                alert("修改成功");
            } else {
                alert("修改失败")
            }
            homePage();
        },
        error: function (xhr) {
            console.log(xhr);
            homePage();
        }
    });
}

function updateDate() {
    var setting = {
        data: {
            key: {
                title: "权限列表"
            },
            simpleData: {
                enable: true//使用简单格式json数据
            }
        },
        check: {
            enable: true//启用复选框效果
        }
    };
    window.setTimeout(function () {
        var table = $('#tb_user').bootstrapTable('getSelections');
        $.ajax({
            url: url + '/acl/allAclsAsTree',
            type: "get",
            dataType: 'jsonp',
            jsonp: "jsoncallback",
            jsonpCallback: 'callback',
            data: {
                name: table[0].mail
            },
            success: function (data) {
                document.getElementById("id").value = table[0].id;
                document.getElementById("tableId").value = curRow;
                document.getElementById("mail").value = table[0].mail;
                if (table[0].type == 2) {
                    document.getElementById('radio2').checked = true;
                } else {
                    document.getElementById('radio1').checked = true;
                }
                var zNodes = eval(data);
                $.fn.zTree.init($("#menuTree"), setting, zNodes);
            },
            error: function (xhr) {
                console.log(xhr);
            }
        });
        document.getElementById('addWindow').style.display = 'block';
    }, 100);
}

function homePage() {
    $('#tb_user').bootstrapTable('destroy');
    initTable();
}

function searchUser() {
    var ss = document.getElementById("ss").value;
    $.ajax({
        url: url + "/acl/findUsersByMail",
        type: "get",
        dataType: 'jsonp',
        jsonp: "jsoncallback",
        jsonpCallback: 'callback',
        data: {
            mail: ss
        },
        success: function (data) {
            $('#tb_user').bootstrapTable('load', data);
        },
        error: function (xhr) {
            console.log(xhr.responseText);
        }
    })
}

function initTable() {
    $("#tb_user").bootstrapTable({
        ajax: function (request) {
            $.ajax({
                url: url + "/acl/allUsers",
                type: "get",
                dataType: 'jsonp',
                jsonp: "jsoncallback",
                jsonpCallback: 'callback',
                data: {
                    pageNum: 1,
                    pageSize: 100
                },
                success: function (res) {
                    request.success({
                        row: res
                    });
                    $('#tb_user').bootstrapTable('load', res['rows']);
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                }
            })
        },
        dataType: 'jsonp',
        idField: "id",
        pagination: true,
        rownumbers: true,
        pageSize: 10,
        pageList: [10, 20, 30, 40, 50],
        singleSelect: true,
        clickToSelect: true,
        queryParams: function (param) {
            return {
                pageNum: 1,
                pageSize: 10
            };
        },
        columns: [{checkbox: true},
            {
                //field: 'Number',//可不加
                title: 'Number',//标题  可不加
                formatter: function (value, row, index) {
                    return index + 1;
                }
            }, {
                field: "mail",
                title: "Mail"
            }, {
                field: "type",
                title: "Type",
                formatter: function (value, row, index) {
                    if (value == 1) {
                        return "用户";
                    } else {
                        return "管理员";
                    }
                }
            }, {
                field: "code",
                title: "Code"
            }, {
                title: '操作',
                field: 'operate',
                formatter: function (value, row, index) {
                    return "<button onclick='updateDate()' class='btn btn-xs red'><i class='glyphicon glyphicon-pencil'></i> 修改</button>";
                }
            }],
        onClickRow: function (row, $element) {
            curRow = $element.data('index');
        }
    });
}
