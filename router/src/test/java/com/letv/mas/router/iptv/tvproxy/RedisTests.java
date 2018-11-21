package com.letv.mas.router.iptv.tvproxy;

import com.letv.mas.router.iptv.tvproxy.model.LetvTypeReference;
import com.letv.mas.router.iptv.tvproxy.model.dao.cache.RedisJsonDao;
import com.letv.mas.router.iptv.tvproxy.model.dao.db.mysql.UserDao;
import com.letv.mas.router.iptv.tvproxy.model.xdo.UserDo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leeco on 18/11/14.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = com.letv.mas.router.iptv.tvproxy.ServiceZuulApplication.class,
        properties = {
                "spring.profiles.active:tvproxy", "SERVER_PORT:9001", "SERVER_IP:10.58.92.94",
                "REDIS_M_NODES:10.124.132.130:6379", "REDIS_M_PASSWORD:aN3eg2Ak@",
                "REDIS_S_NODES:10.124.132.156:6380", "REDIS_S_PASSWORD:aN3eg2Ak@",
                "REDIS_M_NODES:10.124.132.130:6379",
                "REDIS_M_PASSWORD:aN3eg2Ak@",
//                "REDIS_NODES:10.124.132.130:6379#master1,10.124.132.156:6380"
        },
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class RedisTests {
    private static final Logger log = LoggerFactory.getLogger(RedisTests.class);
    private static final String KEY_TEST_PREFIX = "MAS_REDIS_TEST_";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    RedisJsonDao redisJsonDao;

    @Autowired
    private UserDao userDao;

    @LocalServerPort
    private int basePort;
    private URL baseUrl;
    private MockMvc mockMvc;

    @Before
    public void bootstrap() throws Exception {
        String url = String.format("http://localhost:%d/", this.basePort);
        log.info("port is {}", this.basePort);
        this.baseUrl = new URL(url);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    @Test
    public void testRedisOpts() throws Exception {
        UserDo wUserDo = userDao.getUserById("1060106551164772352");
        UserDo rUserDo = null;
        int duration = 0, index = 0;
        // ========================= single (B) ========================= //
        // -w
        redisJsonDao.set(KEY_TEST_PREFIX + 1, wUserDo);
        // -r
        rUserDo = redisJsonDao.get(KEY_TEST_PREFIX + 1, UserDo.class);
        Assert.assertEquals(wUserDo, rUserDo);
        // -d
        redisJsonDao.delete(KEY_TEST_PREFIX + 1);
        rUserDo = redisJsonDao.get(KEY_TEST_PREFIX + 1, UserDo.class);
        Assert.assertEquals(null, rUserDo);
        // -w(t)
        duration = 1;
        redisJsonDao.set(KEY_TEST_PREFIX + 1, wUserDo, duration);
        Thread.sleep(500);
        rUserDo = redisJsonDao.get(KEY_TEST_PREFIX + 1, UserDo.class);
        Assert.assertEquals(wUserDo, rUserDo);
        Thread.sleep(500);
        rUserDo = redisJsonDao.get(KEY_TEST_PREFIX + 1, UserDo.class);
        Assert.assertEquals(null, rUserDo);
        // ========================= single (E) ========================= //

        // ========================= batch (B) ========================= //
        Map<String, UserDo> wUserDos = new HashMap<>();
        Map<String, UserDo> rUserDos = null;
        List<String> keys = new ArrayList<String>();
        while (++index <= 50) {
            wUserDos.put(KEY_TEST_PREFIX + index, wUserDo);
            keys.add(KEY_TEST_PREFIX + index);
        }
        // -w
        redisJsonDao.mset(wUserDos);
        rUserDos = redisJsonDao.mget(keys, UserDo.class);
        Assert.assertTrue(null != rUserDos && rUserDos.size() > 0);
        for (Map.Entry<String, UserDo> entry : rUserDos.entrySet()) {
            Assert.assertEquals(wUserDos.get(entry.getKey()), entry.getValue());
        }
        rUserDos = redisJsonDao.mget(keys, UserDo.class, 20);
        Assert.assertTrue(null != rUserDos && rUserDos.size() > 0);
        for (Map.Entry<String, UserDo> entry : rUserDos.entrySet()) {
            Assert.assertEquals(wUserDos.get(entry.getKey()), entry.getValue());
        }
        // ========================= batch (E) ========================= //
    }
}
