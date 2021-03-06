package com.letv.mas.caller.iptv.tvproxy.common.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.letv.mas.caller.iptv.tvproxy.common.model.dao.db.table.ChannelDataMysqlTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestService extends BaseService {

    //@Transactional
    //@HystrixCommand(threadPoolKey = "vipThreadPool", fallbackMethod = "testFallback")
    public PageInfo<ChannelDataMysqlTable> test(Integer chId, int pageNum, int pageSize) {
        Page<ChannelDataMysqlTable> page = PageHelper.startPage(pageNum, pageSize,true);
        List<ChannelDataMysqlTable> list = this.facadeMysqlDao.getChannelDataMysqlDao().getList(chId);
        PageInfo<ChannelDataMysqlTable> pageInfo = new PageInfo<ChannelDataMysqlTable>(list);
        System.out.println("page.size():"+page.size());
        System.out.print(page);
        System.out.print("-----------------------------");
        System.out.print(pageInfo);
        if (list != null && !list.isEmpty()) {
            return pageInfo;
        }

        /*RowBounds rowBounds = new RowBounds(pageNum, pageSize);
        List<ChannelDataMysqlTable> list = this.facadeMysqlDao.getChannelDataMysqlDao().getList(chId,rowBounds);
        */

        return null;
    }
    public PageInfo<ChannelDataMysqlTable> testFallback(Integer chId,int pageNum,int pageSize) {
        System.out.print(" ---------testFallback -----------");
        return null;
    }
}
