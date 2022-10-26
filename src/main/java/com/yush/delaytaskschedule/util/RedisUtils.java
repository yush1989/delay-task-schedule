package com.yush.delaytaskschedule.util;

import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisUtils {

    private RedisTemplate<String,Object> redisTemplate;

    public RedisUtils(RedisTemplate<String,Object> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入缓存
     * @param key
     * @param value
     * @return
     */
    public boolean set(final String key, Object value) {
        boolean result = false;
        try {
            ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 写入缓存设置时效时间
     * @param key
     * @param value
     * @return
     */
    public boolean set(final String key, Object value, Long expireTime) {
        boolean result = false;
        try {
            //脚本执行成功之后返回true,否则false
            String script = "redis.call('SET',KEYS[1],ARGV[1]);redis.call('EXPIRE',KEYS[1],ARGV[2]);return 1;";
            DefaultRedisScript<Boolean> defaultRedisScript = new DefaultRedisScript(script,Boolean.class);
            List<String> keys = new ArrayList<>();
            keys.add(key);
            result = redisTemplate.execute(defaultRedisScript,keys,value,expireTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 批量删除对应的value
     *
     * @param keys
     */
    public void remove(final String... keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    /**
     * 删除对应的value
     *
     * @param key
     */
    public void remove(final String key) {
        if (exists(key)) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 判断缓存中是否有对应的value
     *
     * @param key
     * @return
     */
    public boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 读取缓存
     *
     * @param key
     * @return
     */
    public Object get(final String key) {
        Object result = null;
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        result = operations.get(key);
        return result;
    }

    /**
     * 哈希 添加
     *
     * @param key
     * @param hashKey
     * @param value
     */
    public void hmSet(String key, Object hashKey, Object value) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        hash.put(key, hashKey, value);
    }

    /**
     * 哈希获取数据
     *
     * @param key
     * @param hashKey
     * @return
     */
    public Object hmGet(String key, Object hashKey) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        return hash.get(key, hashKey);
    }

    /**
     * 列表添加
     *
     * @param k
     * @param v
     */
    public void lPush(String k, Object v) {
        ListOperations<String, Object> list = redisTemplate.opsForList();
        list.rightPush(k, v);
    }

    /**
     * 列表获取
     *
     * @param k
     * @param l
     * @param l1
     * @return
     */
    public List<Object> lRange(String k, long l, long l1) {
        ListOperations<String, Object> list = redisTemplate.opsForList();
        return list.range(k, l, l1);
    }

    /**
     * 集合添加
     *
     * @param key
     * @param value
     */
    public void add(String key, Object value) {
        SetOperations<String, Object> set = redisTemplate.opsForSet();
        set.add(key, value);
    }

    /**
     * 集合获取
     * @param key
     * @return
     */
    public Set<Object> setMembers(String key) {
        SetOperations<String, Object> set = redisTemplate.opsForSet();
        return set.members(key);
    }

    /**
     * 有序集合添加
     * @param key
     * @param value
     * @param scoure
     */
    public void zAdd(String key, Object value, double scoure) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.add(key, value, scoure);
    }

    /**
     * 有序集合删除单个值
     * @param key
     * @param value
     */
    public void zRemove(String key,Object value){
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.remove(key,value);
    }

    /**
     * 有序集合删除多个值
     * @param key
     * @param values
     */
    public void zRemove(String key,Object... values){
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.remove(key,values);
    }

    /**
     * 有序集合获取
     * @param key
     * @param scoure
     * @param scoure1
     * @return
     */
    public Set<Object> rangeByScore(String key, double scoure, double scoure1) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        redisTemplate.opsForValue();
        return zset.rangeByScore(key, scoure, scoure1);
    }

    /**
     * 有序集合获取排名
     * @param key 集合名称
     * @param value 值
     */
    public Long zRank(String key, Object value) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.rank(key,value);
    }

    /**
     * 有序集合获取排名
     * @param key
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRankWithScore(String key, long start,long end) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> ret = zset.rangeWithScores(key,start,end);
        return ret;
    }

    /**
     * 有序集合添加
     * @param key
     * @param value
     */
    public Double  zSetScore(String key, Object value) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.score(key,value);
    }

    /**
     * 有序集合添加分数
     * @param key
     * @param value
     * @param scoure
     */
    public void incrementScore(String key, Object value, double scoure) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.incrementScore(key, value, scoure);
    }

    /**
     * 有序集合获取排名
     * @param key
     */
    public Set<ZSetOperations.TypedTuple<Object>> reverseZRankWithScore(String key, long start,long end) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> ret = zset.reverseRangeByScoreWithScores(key,start,end);
        return ret;
    }

    /**
     * 有序集合获取排名
     * @param key
     */
    public Set<ZSetOperations.TypedTuple<Object>> reverseZRankWithRank(String key, long start, long end) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> ret = zset.reverseRangeWithScores(key, start, end);
        return ret;
    }

    /**
     * 设置过期时间,有时间单位
     * @param key
     * @param value
     * @param expireTime
     * @param timeUnit
     * @return
     */
    public Boolean setNx(String key, Object value, long expireTime, TimeUnit timeUnit){
        return redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, timeUnit);
    }

    /**
     * 设置过期时间，没有时间单位，默认是秒
     * @param key
     * @param value
     * @param expireTime
     * @return
     */
    public Boolean setNx(String key, Object value, long expireTime){
        return setNx(key,value,expireTime,TimeUnit.SECONDS);
    }

    /**
     * 设置值，不设置过期时间
     * @param key
     * @param value
     * @return
     */
    public Boolean setNx(String key, Object value){
        return redisTemplate.opsForValue().setIfAbsent(key,value);
    }

    /**
     * 递增
     * @param key
     * @return
     */
    public long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 指定key的过期时间
     * @param key
     * @param time 单位为秒
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取key剩余过期时间，-1 未设置过期时间，-2 不存在对应key
     * @return
     */
    public Long getExpire(String key){
        return redisTemplate.getExpire(key,TimeUnit.SECONDS);
    }

    /**
     * 模糊匹配是否存在key
     * @param pattern
     * @return
     */
    public Boolean hasKeyPattern(String pattern){
        Set<String> set = redisTemplate.keys(pattern);
        return !CollectionUtils.isEmpty(set);
    }

    /**
     * 查看是否包含指定key
     * @param key
     * @return
     */
    public Boolean hasKey(String key){
        return redisTemplate.hasKey(key);
    }

    /**
     * 返回匹配到的所有key集合
     * @param pattern
     * @return
     */
    public Set<String> keys(String pattern){
        return redisTemplate.keys(pattern);
    }
}
