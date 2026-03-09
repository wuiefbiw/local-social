package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //空值解决缓存穿透
        //Shop shop = querywithPassThrough(id);

        //互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 缓存空值解决缓存穿透
     * @param id
     * @return
     */
    private Shop querywithPassThrough(Long id) {
        //1.从 redis 查询商铺缓存
        String shopjson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopjson)) {
            //3.存在，直接返回
            Shop shop = JSONUtil.toBean(shopjson, Shop.class);
            return shop;
        }
        //判断命中的是否为空值
        if (shopjson != null) {
            //返回错误信息
            return null;
        }
        //4.不存在，根据id查询数据库
        log.debug("缓存未命中，查询数据库...");
        Shop shop = getById(id);
        //5.db也不存在，返回错误
        if (shop == null) {
            //将空值写入 redis 防止缓存穿透
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回
        return shop;
    }

    /**
     * 互斥锁解决缓存击穿
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id) {
        //1.从 redis 查询商铺缓存
        String shopjson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopjson)) {
            //3.存在，直接返回
            Shop shop = JSONUtil.toBean(shopjson, Shop.class);
            return shop;
        }
        //判断命中的是否为空值
        if (shopjson != null) {
            //返回错误信息
            return null;
        }
        //4.实现缓存重建
        //4.1 获取互斥锁
        Shop shop = null;
        String key = LOCK_SHOP_KEY + id;
        try {
            boolean isLock = getLock(key);
            //4.2 判断是否获取成功
            if (!isLock) {
                //4.3 失败，休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4 成功，根据id查询数据库
            log.debug("缓存未命中，查询数据库...");
            shop = getById(id);
            //5.db也不存在，返回错误
            if (shop == null) {
                //将空值写入 redis 防止缓存穿透
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //6.存在，写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放锁
            unlock(key);
        }
        //8.返回
        return shop;
    }
    private boolean getLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除 redis 中的缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
