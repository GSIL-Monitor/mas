var url = GlobalUrl;
var token = $.cookie("OMP_LOGIN_TOKEN");
var curRow = {};
$(function () {
    initTable();
});

function homePage() {
    $('#tb_application_manager').bootstrapTable('destroy');
    initTable();
}

function searchApp() {
    var user_id = document.getElementById("ss").value;
    var value = document.getElementById("cc").value;
    if (user_id != "" || value != "") {
        $.ajax({
            url: url + "/app/searchApp",
            type: "get",
            dataType: 'jsonp',
            jsonp: "callback",
            data: {
                user_id: user_id,
                value: value
            },
            success: function (res) {
                if (res.status == 0) {
                    alert("没有符合条件的结果");
                } else {
                    $('#tb_application_manager').bootstrapTable('load', res);
                }
            },
            error: function (xhr) {
                console.log(xhr.responseText);
            }
        })
    }else {
        homePage();
    }

}

function startUse() {
    window.setTimeout(function () {
        var table = $('#tb_application_manager').bootstrapTable('getSelections');
        $.ajax({
            url: url + "/app/startUseApplications",
            type: "get",
            dataType: 'jsonp',
            jsonp: "callback",
            data: {
                id: table[0].id,
                user_id: table[0].user_id,
                client_id: table[0].client_id
            },
            success: function (res) {
                homePage();
            },
            error: function (xhr) {
                console.log(xhr.responseText);
            }
        })
    }, 100);
}

function stopUse() {
    window.setTimeout(function () {
        var table = $('#tb_application_manager').bootstrapTable('getSelections');
        $.ajax({
            url: url + "/app/stopUseApplications",
            type: "get",
            dataType: 'jsonp',
            jsonp: "callback",
            data: {
                id: table[0].id,
                user_id: table[0].user_id,
                client_id: table[0].client_id
            },
            success: function (res) {
                homePage();
            },
            error: function (xhr) {
                console.log(xhr.responseText);
            }
        })
    }, 100);
}

function passDate() {
    window.setTimeout(function () {
        var table = $('#tb_application_manager').bootstrapTable('getSelections');
        $.ajax({
            url: url + "/app/passApplications",
            type: "get",
            dataType: 'jsonp',
            jsonp: "callback",
            data: {
                id: table[0].id,
                client_id: table[0].client_id,
                en_name: table[0].en_name,
                user_id: table[0].user_id
            },
            success: function (res) {
                homePage();
            },
            error: function (xhr) {
                console.log(xhr.responseText);
            }
        })
    }, 100);
}

function findDate() {
    window.setTimeout(function () {
        var table = $('#tb_application_manager').bootstrapTable('getSelections');
        document.getElementById("id").value = table[0].id;
        document.getElementById("user_id").value = table[0].user_id;
        document.getElementById("user_id").disabled = "disabled";
        document.getElementById("status").innerHTML = '&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;审核成功';
        document.getElementById("client_name").value = table[0].client_name;
        document.getElementById("client_name").disabled = "disabled";
        document.getElementById("en_name").value = table[0].en_name;
        document.getElementById("en_name").disabled = "disabled";
        document.getElementById("client_secret").value = table[0].client_secret;
        document.getElementById("client_secret").disabled = "disabled";
        var create_time = timestampToTime(table[0].create_time / 1000);
        document.getElementById("create_time").value = create_time;
        document.getElementById("create_time").disabled = "disabled";
        var end_time = timestampToTime(table[0].end_time / 1000);
        document.getElementById("end_time").value = end_time;
        document.getElementById("end_time").disabled = "disabled";
        document.getElementById("saveBtn").style.display = 'none';
        document.getElementById('editWindow').style.display = 'block';
    }, 100);
}

function updateDate() {
    window.setTimeout(function () {
        var table = $('#tb_application_manager').bootstrapTable('getSelections');
        document.getElementById("id").value = table[0].id;
        document.getElementById("user_id").value = table[0].user_id;
        document.getElementById("user_id").disabled = "disabled";
        document.getElementById("status").innerHTML = '&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;送审中';
        document.getElementById("client_name").value = table[0].client_name;
        document.getElementById("client_name").disabled = "";
        document.getElementById("en_name").value = table[0].en_name;
        document.getElementById("en_name").disabled = "";
        document.getElementById("client_secret").value = table[0].client_secret;
        document.getElementById("client_secret").disabled = "";
        var create_time = timestampToTime(table[0].create_time / 1000);
        document.getElementById("create_time").value = create_time;
        document.getElementById("create_time").disabled = "disabled";
        var end_time = timestampToTime(table[0].end_time / 1000);
        document.getElementById("end_time").value = end_time;
        document.getElementById("end_time").disabled = "disabled";
        document.getElementById("saveBtn").style.display = 'block';
        document.getElementById('editWindow').style.display = 'block';
    }, 100);
}

function saveDate() {
    var id = document.getElementById("id").value;
    var user_id = document.getElementById("user_id").value;
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
                    user_id: jQuery.base64.encode(user_id),
                    client_name: client_name,
                    en_name: en_name,
                    client_secret: client_secret
                },
                success: function (res) {
                    // console.log(res);
                    if (res.status == 1) {
                        homePage();
                        document.getElementById('editWindow').style.display = 'none';
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

function deleteDate() {
    window.setTimeout(function () {
        var table = $('#tb_application_manager').bootstrapTable('getSelections');
        if (confirm("确定要删这条数据吗?")) {
            $.ajax({
                url: url + "/app/deleteApplications",
                type: "get",
                dataType: 'jsonp',
                jsonp: "callback",
                data: {
                    user_id: table[0].user_id,
                    client_id: table[0].client_id
                },
                success: function (res) {
                    homePage();
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                }
            })
        }
    }, 100);
}

function initTable() {
    $("#tb_application_manager").bootstrapTable({
        ajax: function (request) {
            $.ajax({
                url: url + "/app/allApplications",
                type: "get",
                dataType: 'jsonp',
                jsonp: "callback",
                data: {
                    pageNum: 1,
                    pageSize: 100,
                    token: token
                },
                success: function (res) {
                    // console.log(res);
                    request.success({
                        row: res
                    });
                    $('#tb_application_manager').bootstrapTable('load', res['data']);
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                }
            })
        },
        dataType: 'jsonp',
        idField: "id",
        pagination: true,
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
                field: 'client_secret',
                title: '应用秘钥'
            }, {
                field: "user",
                title: "所属用户"
            }, {
                field: "client_name",
                title: "应用名称"
            }, {
                title: '操作',
                field: 'operate',
                formatter: function (value, row, index) {
                    if (row.status == 3) {
                        if (row.flag == 0) {
                            return "<button onclick='stopUse()' class='btn btn-xs red'><i class='glyphicon glyphicon-ban-circle'></i> 停用</button>" +
                                "&nbsp;&nbsp;<button onclick='findDate()' class='btn btn-xs red'><i class='glyphicon glyphicon-search'></i> 查看</button>" +
                                "&nbsp;&nbsp;<button onclick='deleteDate()' class='btn btn-xs red'><i class='fa fa-trash-o'></i> 删除</button>";
                        } else {
                            return "<button onclick='startUse()' class='btn btn-xs red'><i class='glyphicon glyphicon-ok-sign'></i> 启用</button>" +
                                "&nbsp;&nbsp;<button onclick='findDate()' class='btn btn-xs red'><i class='glyphicon glyphicon-search'></i> 查看</button>" +
                                "&nbsp;&nbsp;<button onclick='deleteDate()' class='btn btn-xs red'><i class='fa fa-trash-o'></i> 删除</button>";
                        }
                    } else {
                        return "<button onclick='passDate()' class='btn btn-xs red'><i class='glyphicon glyphicon-play'></i> 通过</button>" +
                            "&nbsp;&nbsp;<button onclick='updateDate()' class='btn btn-xs red'><i class='glyphicon glyphicon-pencil'></i> 修改</button>" +
                            "&nbsp;&nbsp;<button onclick='deleteDate()' class='btn btn-xs red'><i class='fa fa-trash-o'></i> 删除</button>";
                    }
                }
            }],
        onClickRow: function (row, $element) {
            curRow = $element.data('index');
        }
    });
}

function timestampToTime(timestamp) {
    var date = new Date(timestamp * 1000);//时间戳为10位需*1000，时间戳为13位的话不需乘1000
    Y = date.getFullYear() + '-';
    M = (date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1) + '-';
    D = date.getDate() + ' ';
    h = date.getHours() + ':';
    m = date.getMinutes() + ':';
    s = date.getSeconds();
    return Y + M + D + h + m + s;
}