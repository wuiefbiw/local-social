package com.hmdp.config;

import com.hmdp.utils.Logininterceptor;
import com.hmdp.utils.RefreshTokeninterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new Logininterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type",
                        "/upload/**",
                        "/blog/hot",
                        //"/user/me",
                        "/user/code",    //发送验证码
                        "/user/login"    //登录

                ).order(1);
        registry.addInterceptor(new RefreshTokeninterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
