package dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @Author:peishunwu
 * @Description:
 * @Date:Created  2018/6/11
 *
 * 注意：往Readis中放入的对象一定要序列化之后再放入，
 * 序列化的目的是将一个实现Serializable接口的对象转换成一个字节序列，可以。把该字节序列保存起来
 * （例如：保存在一个文件里）以后随时将该字节序列恢复原来的对象。
 *序列化的对象占原来空间的十分之一，压缩速度可以达到两个数量级，同时节省了CPU
 *
 * Readis缓存对象时需要将对象序列化，而何为序列化，实际上就是将对象以字节存储，这样不管对象的属性是字符串、整形还是图片、视频等二进制类型，
 * 都可以将其保存在字节数组中。对象序列化后便可以持久化保存或者网络传输。需要还原对象时，只需要将字节数组再反序列化即可。
 *
 * 因为要在项目中用到，所以要添加@Service， 把这个做成一个服务
 * 因为要初始化连接池JedisPool，所以要implements  InitializingBean并调用默认的
 * afterPropertiesSet()方法
 *
 */
@Service
public class RedisDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JedisPool jedisPool;

    public RedisDao(String ip, int port) {
        //一个简单的配置
        jedisPool = new JedisPool(ip,port);
    }

    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public Seckill getSeckill(long seckillId){
        //缓存Redis操作逻辑，而不应该放在Service下，因为这是数据访问层的逻辑
        try{
            Jedis jedis = jedisPool.getResource();
            try{
                String key = "seckill:"+seckillId;
                //并没有实现内部序列化操作
                //get->byte[]->反序列化-》Object(Seckill)
                //采用自定义序列化方式
                //采用自定义的序列化，在pom.xml文件中引入两个依赖protostuff：pojo
                byte[] bytes = jedis.get(key.getBytes());
                //重新获取缓存
                if(bytes != null){
                    Seckill seckill = schema.newMessage();
                    //将bytes按照从Seckill类创建的模式架构scheam反序列化赋值给对象seckill
                    ProtostuffIOUtil.mergeFrom(bytes,seckill,schema);
                    return seckill;
                }
            }finally {
                jedis.close();
            }
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return null;
    }


    public String putSeckill(Seckill seckill){
        //set Object (Seckill)---》序列化----》bytes[]
        try{
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:"+seckill.getSeckillId();
                //protostuff工具
                //将seckill对象序列化成字节数组
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill,schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                //缓存时间+key标记+对象序列化结果==》放入缓存jedis缓存池，返回结果result（OK/NO）
                int timeout = 60*60;
                String result = jedis.setex(key.getBytes(),timeout,bytes);
                return result;
            }finally {
                jedis.close();
            }
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return null;
    }
}
