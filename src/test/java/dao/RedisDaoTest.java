package dao;

import dao.cache.RedisDao;
import entity.Seckill;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @Author:peishunwu
 * @Description:
 * @Date:Created  2018/6/11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class RedisDaoTest {

    private long seckillId = 1001;
    @Autowired
    private RedisDao redisDao;

    @Autowired
    private SeckillDao seckillDao;
    @Test
    public void testKill()throws Exception{
        //get  and  put

        //从缓存中获取
        Seckill seckill = redisDao.getSeckill(seckillId);
        if(seckill == null){//缓存中没有就从数据库查询
            seckill = seckillDao.queryById(seckillId);
            if(seckill !=null){
                String result = redisDao.putSeckill(seckill);//缓存序列化对象
                System.out.println("放入缓存结果："+result);
                seckill = redisDao.getSeckill(seckillId);
                System.out.println(seckill);
            }
        }else {
            System.out.println("从缓存中获取成功："+seckill);
        }

    }
}
