var url = GlobalUrl;
var curRow = {};
$(function () {
    initTable();
});

function homePage() {
    $('#tb_acl').bootstrapTable('destroy');
    initTable();
}

function newlyAdd() {
    document.getElementById('addWindow').style.display = 'block';
    document.getElementById("id").value = "";
    document.getElementById("name").value = "";
    document.getElementById("path").value = "";
    document.getElementById("_parentId").value = "";
}

function updateDate() {
    window.setTimeout(function () {
        var table = $('#tb_acl').bootstrapTable('getSelections');
        document.getElementById('addWindow').style.display = 'block';
        document.getElementById("id").value = table[0].id;
        document.getElementById("name").value = table[0].name;
        document.getElementById("path").value = table[0].path;
        document.getElementById("_parentId").value = table[0]._parentId;
    }, 100);
}

function saveDate() {
    var id = document.getElementById("id").value;
    var name = document.getElementById("name").value;
    var path = document.getElementById("path").value;
    var _parentId = document.getElementById("_parentId").value;
    console.log(id + name + path + _parentId);
    $.ajax({
        async: false,
        url: url + "/acl/updateAcl",
        type: "post",
        dataType: 'jsonp',
        jsonp: "jsoncallback",
        jsonpCallback: 'callback',
        data: {
            id: id,
            name: name,
            path: path,
            _parentId: _parentId
        },
        success: function (res) {
            if (res == 1) {
                document.getElementById('addWindow').style.display = 'none';
                alert('保存成功');
            } else {
                document.getElementById('addWindow').style.display = 'none';
                alert('保存失败');
            }
            homePage();
        },
        error: function (xhr) {
            console.log(xhr);
            homePage();
        }
    })
}

function deleteDate() {
    if (confirm("确定删除该权限?")) {
        //点击确定后操作
        window.setTimeout(function () {
            var table = $('#tb_acl').bootstrapTable('getSelections');
            $.ajax({
                async: false,
                url: url + "/acl/deleteAcl",
                type: "post",
                dataType: 'jsonp',
                jsonp: "jsoncallback",
                jsonpCallback: 'callback',
                data: {
                    id: table[0].id
                },
                success: function (res) {
                    if (res == 1) {
                        alert('删除成功');
                    } else {
                        alert('删除失败');
                    }
                    $('#tb_acl').bootstrapTable('destroy');
                    initTable();
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                    $('#tb_acl').bootstrapTable('destroy');
                    initTable();
                }
            })
        }, 100);
    }
}

function initTable() {
    $("#tb_acl").bootstrapTable({
        ajax: function (request) {
            $.ajax({
                url: url + "/acl/allAcls",
                type: "get",
                dataType: 'jsonp',
                jsonp: "jsoncallback",
                jsonpCallback: 'callback',
                data: {
                    pageNum: 1,
                    pageSize: 50
                },
                success: function (res) {
                    request.success({
                        row: res
                    });
                    $('#tb_acl').bootstrapTable('load', res['rows']);
                },
                error: function (xhr) {
                    console.log(xhr.responseText);
                }
            })
        },
        dataType: 'jsonp',
        // toolbar: "#toolbar",
        idField: "id",
        pagination: true,
        pageSize: 10,
        pageList: [10, 20, 30, 40, 50],
        singleSelect: true,
        // showRefresh: true,
        // search: true,
        clickToSelect: true,
        queryParams: function (param) {
            return {
                pageNum: 1,
                pageSize: 10
            };
        },
        columns: [{
            checkbox: true
        }, {
            field: "id",
            title: "ID"
            // formatter: function (value, row, index) {
            //     return "<a href=\"#\" name=\"UserName\" data-type=\"text\" data-pk=\"" + row.id + "\" data-title=\"用户名\">" + value + "</a>";
            // }
        }, {
            field: "name",
            title: "Name"
        }, {
            field: "path",
            title: "Path"
        }, {
            field: "_parentId",
            title: "ParentId"
        }, {
            title: '操作',
            field: 'operate',
            //row.id+\",\"+row.name+\",\"+row.path+\",\"+row._parentId
            formatter: function (value, row, index) {
                return "<button onclick='updateDate()' class='btn btn-xs blue'><i class='glyphicon glyphicon-pencil'></i> 修改</button>"
                    + "&nbsp;&nbsp;&nbsp;&nbsp;<button onclick='deleteDate()' class='btn btn-xs red'><i class='fa fa-trash-o'></i> 删除</button>";
            }
        }],
        onClickRow: function (row, $element) {
            curRow = $element.data('index');
        }
        // ,
        // onLoadSuccess: function (aa, bb, cc) {
        //     $("#tb_user a").editable({
        //         url: function (params) {
        //             var sName = $(this).attr("name");
        //             curRow[sName] = params.value;
        //             $.ajax({
        //                 type: 'POST',
        //                 url: "/Editable/Edit",
        //
        //                 data: curRow,
        //                 dataType: 'JSON',
        //                 success: function (data, textStatus, jqXHR) {
        //                     alert('保存成功！');
        //                 },
        //                 error: function () {
        //                     alert("error");
        //                 }
        //             });
        //         },
        //         type: 'text'
        //     });
        // }
    });
}
