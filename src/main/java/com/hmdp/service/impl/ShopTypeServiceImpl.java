package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getTypeList() {
        //1. 查询 redis 库
        String shop_type_JSON = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        //2. 如果存在，直接返回
        if (StrUtil.isNotBlank(shop_type_JSON)) {
            JSONArray jsonArray = JSONUtil.parseArray(shop_type_JSON);
            List<ShopType> list = jsonArray.toList(ShopType.class);
            return Result.ok(list);
        }
        //3. 不存在，查询数据库
        log.debug("缓存未命中，查询数据库...");
        List<ShopType> typeList = query().orderByAsc("sort").list();

        //4. 数据库不存在，返回错误
        if (typeList == null) {
            return Result.fail("店铺类型不存在");
        }
        //5. 存在，写入 redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(typeList));
        //6. 返回
        return Result.ok(typeList);
    }
}
