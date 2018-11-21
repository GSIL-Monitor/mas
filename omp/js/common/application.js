var url = GlobalUrl;
var token = $.cookie("OMP_LOGIN_TOKEN");
var curRow = {};
$(function () {
    initTable();
});

function homePage() {
    $('#tb_application').bootstrapTable('destroy');
    initTable();
}

function applyApplication() {
    document.getElementById("id").value = "";
    document.getElementById("user_id").value = jQuery.base64.decode(token);
    document.getElementById("user_id").disabled = "disabled";
    document.getElementById("client_name").value = "";
    document.getElementById("client_name").disabled = "";
    document.getElementById("en_name").value = "";
    document.getElementById("en_name").disabled = "";
    document.getElementById("client_secret").value = "";
    document.getElementById("client_secret").disabled = "";
    document.getElementById("status").innerHTML = '';
    document.getElementById("saveBtn").style.display = 'block';
    document.getElementById('addWindow').style.display = 'block';
}

function findDate() {
    window.setTimeout(function () {
        var table = $('#tb_application').bootstrapTable('getSelections');
        document.getElementById("id").value = table[0].id;
        document.getElementById("user_id").value = jQuery.base64.decode(token);
        document.getElementById("user_id").disabled = "disabled";
        document.getElementById("client_name").value = table[0].client_name;
        document.getElementById("client_name").disabled = "disabled";
        document.getElementById("en_name").value = table[0].en_name;
        document.getElementById("en_name").disabled = "disabled";
        document.getElementById("client_secret").value = table[0].client_secret;
        document.getElementById("client_secret").disabled = "disabled";
        document.getElementById("status").innerHTML = '&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;审核成功';
        document.getElementById("saveBtn").style.display = 'none';
        document.getElementById('addWindow').style.display = 'block';
    }, 100);
}

function updateDate() {
    window.setTimeout(function () {
        var table = $('#tb_application').bootstrapTable('getSelections');
        document.getElementById("id").value = table[0].id;
        document.getElementById("user_id").value = jQuery.base64.decode(token);
        document.getElementById("user_id").disabled = "disabled";
        document.getElementById("client_name").value = table[0].client_name;
        document.getElementById("client_name").disabled = "";
        document.getElementById("en_name").value = table[0].en_name;
        document.getElementById("en_name").disabled = "";
        document.getElementById("client_secret").value = table[0].client_secret;
        document.getElementById("client_secret").disabled = "";
        if (table[0].status == 2) {
            document.getElementById("status").innerHTML = '&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;审核失败';
        } else {
            document.getElementById("status").innerHTML = '&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;送审中';
        }
        document.getElementById("saveBtn").style.display = 'block';
        document.getElementById('addWindow').style.display = 'block';
    }, 100);
}

function saveDate() {
    var id = document.getElementById("id").value;
    // var user_id = document.getElementById("user_id").value;
    var client_name = document.getElementById("client_name").value;
    var client_secret = document.getElementById("client_secret").value;
    if (client_name == "") {
        alert("应用名称不能为空");
    } else {
        var en_name = document.getElementById("en_name").value;
        if (en_name == "") {
            alert("英文简称不能为空");
        } else {
            $.ajax({
                url: url + "/app/updateApplications",
                type: "get",
                dataType: 'jsonp',
                jsonp: "callback",
                data: {
                    id: id,
                    user_id: token,
                    client_name: client_name,
                    en_name: en_name,
                    client_secret: client_secret
                },
                success: function (res) {
                    // console.log(res);
                    if (res.status == 1) {
                        homePage();
                        document.getElementById('addWindow').style.display = 'none';
                        alert("修改成功");
                    } else {
                        alert(res.msg);
                    }
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                }
            })
        }
    }
}

function initTable() {
    $("#tb_application").bootstrapTable({
        ajax: function (request) {
            $.ajax({
                url: url + "/app/userApplications",
                type: "get",
                dataType: 'jsonp',
                jsonp: "callback",
                data: {
                    pageNum: 1,
                    pageSize: 20,
                    token: token
                },
                success: function (res) {
                    // console.log(res);
                    request.success({
                        row: res
                    });
                    $('#tb_application').bootstrapTable('load', res['data']);
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                }
            })
        },
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
                field: "client_name",
                title: "应用名称"
            }, {
                field: "en_name",
                title: "英文简称"
            }, {
                field: "client_secret",
                title: "应用秘钥"
            }, {
                title: '操作',
                field: 'operate',
                formatter: function (value, row, index) {
                    if (row.flag == 2) {
                        return "<p style='color: #ff0d03;'>设备已停用</p>";
                    } else if (row.flag == 1) {
                        return "<p style='color: #ff0d03;'>设备已删除</p>";
                    } else {
                        if (row.status == 3) {
                            return "<button onclick='findDate()' class='btn btn-xs red'><i class='glyphicon glyphicon-search'></i> 查看</button>";
                        } else {
                            return "<button onclick='updateDate()' class='btn btn-xs blue'><i class='glyphicon glyphicon-pencil'></i> 修改</button>";
                        }
                    }
                }
            }],
        onClickRow: function (row, $element) {
            curRow = $element.data('index');
        }
    });
}