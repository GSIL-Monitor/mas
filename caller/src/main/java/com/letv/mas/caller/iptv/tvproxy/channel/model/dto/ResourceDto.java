package com.letv.mas.caller.iptv.tvproxy.channel.model.dto;

import com.letv.mas.caller.iptv.tvproxy.apicommon.model.bean.bo.BaseData;
import com.letv.mas.caller.iptv.tvproxy.common.model.dao.db.table.AlbumMysqlTable;
import com.letv.mas.caller.iptv.tvproxy.common.model.dao.db.table.VideoMysqlTable;
import com.letv.mas.caller.iptv.tvproxy.common.model.dao.tp.static1.HotSearchRecommendDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@ApiModel(value = "ResourceDto", description = "CMS配置资源信息")
public class ResourceDto extends BaseData {

    /**
     *
     */
    private static final long serialVersionUID = -1900545957997744784L;

    /**
     * 内容id，比如，专题id
     */
    @ApiModelProperty(value = "内容id，比如，专题id")
    private String id;

    /**
     * 数据表示专题资源时，包含6--专辑专题, 7--视频专题，8--直播专题
     */
    @ApiModelProperty(value = "数据表示专题资源时，包含6--专辑专题, 7--视频专题，8--直播专题")
    private String type;

    @ApiModelProperty(value = "929x466图床地址")
    private String img929x466;
    @ApiModelProperty(value = "400x300图床地址")
    private String img400x300;
    @ApiModelProperty(value = "615x225图床地址")
    private String img615x225;

    @ApiModelProperty(value = "默认图片地址")
    private String img;// 图片地址

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "副标题")
    private String subTitle;

    @ApiModelProperty(value = "简介")
    private String shortDesc;

    /**
     * 我的页面会员相关板块的位置，
     * focus--大焦点图,1、激活会员服务，2--开通会员，3(目前没有我要续费)--我要续费，4--会员兑换，5--播放记录，6--收藏与追剧
     */
    @ApiModelProperty(value = "页面会员相关板块的位置：focus--大焦点图,1、激活会员服务，2--开通会员，3(目前没有我要续费)--我要续费，4--会员兑换，5--播放记录，6--收藏与追剧")
    private String pos;

    @ApiModelProperty(value = "分类id")
    private String categoryId;

    @ApiModelProperty(value = "频道id")
    private String cid;// channelId

    // private String cCode;

    @ApiModelProperty(value = "专辑id")
    private String albumId;

    @ApiModelProperty(value = "第几集")
    private String episode;

    @ApiModelProperty(value = "专辑中排序")
    private int porder;

    /**
     * 一般从CMS板块的“URL相关”取数据
     */
    @ApiModelProperty(value = "CMS板块的“URL相关”取数据")
    private String url;

    /**
     * 打开浏览器类型，2015-08-17为兼容CMS H5跳转支持收银台逻辑扩展，不合理的设计；
     * 2015-09-09，修改设计，该值修改为与com.letv.itv.v2.api.datao.
     * Browser.browserType一致的业务意义
     */
    @ApiModelProperty(value = "［历史版本］打开浏览器类型")
    private Integer browserType;

    /**
     * 跳转或打开页面的方式，0--不做任何跳转和响应，1--跳收银台，2015-08-17为兼容CMS H5跳转支持收银台逻辑扩展，不合理的设计;
     * 2015-09-09，修改设计，当dataType=9表示跳H5时，该值修改为与com.letv.itv.v2.api.datao.
     * Browser.penTypey一致的业务意义
     */
    @ApiModelProperty(value = "［历史版本］跳转或打开页面的方式")
    private Integer openType;

    /**
     * 2015-09-09，修改设计，url映射，该属性参考com.letv.itv.v2.api.datao.Browser.urlMap
     */
    @ApiModelProperty(value = "［历史版本］url映射")
    private Map<String, String> urlMap;

    /**
     * 超前院线id，与subjectId值相同；
     * 2015-09-09，修改设计，该属性参考com.letv.itv.v2.api.module.subject.SubjectPreLive.
     * cinemaId;
     */
    @ApiModelProperty(value = "［历史版本］超前院线id，与subjectId值相同")
    private String cinemaId;

    /**
     * 板块id，该属性参考com.letv.itv.v2.api.data.BaseBlock.blockId
     */
    @ApiModelProperty(value = "板块id")
    private String blockId;

    /**
     * 子标签id，该属性参考com.letv.itv.v2.api.data.BaseBlock.labelIdToPlay
     */
    @ApiModelProperty(value = "子标签id")
    private String labelIdToPlay;

    /**
     * 2015-09-09，修改设计，数据类型，该属性参考com.letv.itv.v2.api.data.BaseData的设计，值参考com.
     * letv.itv.v2.api.constants.DataConstant.DATA_TYPE_*
     */
    @ApiModelProperty(value = "数据类型")
    private int dataType = 0;

    public ResourceDto() {

    }

    public ResourceDto(VideoMysqlTable iptvVideoInfo) {
        if (iptvVideoInfo != null) {
            this.id = String.valueOf(iptvVideoInfo.getId());
            this.title = iptvVideoInfo.getName_cn();
            this.subTitle = iptvVideoInfo.getSub_title().equalsIgnoreCase("null") ? "" : iptvVideoInfo.getSub_title();
            this.shortDesc = iptvVideoInfo.getShort_desc();

            this.img400x300 = iptvVideoInfo.getPic(400, 300).toLowerCase();
            this.categoryId = iptvVideoInfo.getCategory().toString();
            this.episode = iptvVideoInfo.getEpisode();
            this.porder = iptvVideoInfo.getPorder();
        }
    }

    public ResourceDto(AlbumMysqlTable iptvAlbumInfo) {
        if (iptvAlbumInfo != null) {
            this.id = String.valueOf(iptvAlbumInfo.getId());
            this.title = iptvAlbumInfo.getName_cn();
            this.subTitle = iptvAlbumInfo.getSub_title().equalsIgnoreCase("null") ? "" : iptvAlbumInfo.getSub_title();
            this.shortDesc = iptvAlbumInfo.getShort_desc();

            this.img400x300 = iptvAlbumInfo.getPic(400, 300).toLowerCase();
            this.categoryId = iptvAlbumInfo.getCategory().toString();
        }

    }

    public ResourceDto(HotSearchRecommendDTO hotSearchRecommend) {
        if (hotSearchRecommend != null) {
            this.id = hotSearchRecommend.getAid().toString();
            this.title = hotSearchRecommend.getName();
            this.subTitle = hotSearchRecommend.getSubTitle().equalsIgnoreCase("null") ? "" : hotSearchRecommend
                    .getSubTitle();
            this.img615x225 = hotSearchRecommend.getPoster20();
            this.img400x300 = hotSearchRecommend.getImg400x300();
        }
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImg929x466() {
        return this.img929x466;
    }

    public void setImg929x466(String img929x466) {
        this.img929x466 = img929x466;
    }

    public String getImg400x300() {
        return this.img400x300;
    }

    public void setImg400x300(String img400x300) {
        this.img400x300 = img400x300;
    }

    public String getImg615x225() {
        return this.img615x225;
    }

    public void setImg615x225(String img615x225) {
        this.img615x225 = img615x225;
    }

    public String getImg() {
        return this.img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return this.subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getShortDesc() {
        return this.shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getPos() {
        return this.pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCid() {
        return this.cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getAlbumId() {
        return this.albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getEpisode() {
        return this.episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public int getPorder() {
        return this.porder;
    }

    public void setPorder(int porder) {
        this.porder = porder;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getBrowserType() {
        return this.browserType;
    }

    public void setBrowserType(Integer browserType) {
        this.browserType = browserType;
    }

    public Integer getOpenType() {
        return this.openType;
    }

    @Override
    public int getDataType() {
        return this.dataType;
    }

    @Override
    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setOpenType(Integer openType) {
        this.openType = openType;
    }

    public String getCinemaId() {
        return this.cinemaId;
    }

    public void setCinemaId(String cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getBlockId() {
        return this.blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getLabelIdToPlay() {
        return this.labelIdToPlay;
    }

    public void setLabelIdToPlay(String labelIdToPlay) {
        this.labelIdToPlay = labelIdToPlay;
    }

    public Map<String, String> getUrlMap() {
        return this.urlMap;
    }

    public void setUrlMap(Map<String, String> urlMap) {
        this.urlMap = urlMap;
    }

}
