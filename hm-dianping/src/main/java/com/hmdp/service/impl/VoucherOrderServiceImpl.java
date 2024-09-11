package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while(true){
                try {
                    // 获取消息队列中订单信息 XGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 消息获取失败，即没有消息，继续下一次循环
                    if(list == null || list.isEmpty()){
                        continue;
                    }
                    // 解析订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    // 消息获取成功，创建订单
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // ACK确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("订单异常",e);
                    // pendingList里是那些获取但没ACK的
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            // 处理异常消息
            while(true){
                try {
                    // 获取pending-list中订单信息 XGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    // 消息获取失败，即pending-list没有消息，结束循环
                    if(list == null || list.isEmpty()){
                        break;
                    }
                    // 解析订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    // 消息获取成功，创建订单
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // ACK确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("Pending-list异常",e);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
//    private class VoucherOrderHandler implements Runnable{
//
//        @Override
//        public void run() {
//            while(true){
//                try {
//                    // 获取阻塞队列中订单信息
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    // 创建订单
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("订单异常",e);
//                }
//            }
//        }
//    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 创建锁对象（分布式锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock(); // 可选参数：等待时间，TTL，时间单位
        if(!isLock){
            log.error("不能重复下单");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    private  IVoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        // 执行lua
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        // 包装类用toString,基本类型用的String.valueOf
        // 无法购买
        int r = result.intValue();
        if(r != 0){
            return Result.fail(r == 1 ? "库存不足！" : "请勿重复下单！");
        }
        // 获取代理对象
        proxy = (IVoucherOrderService)AopContext.currentProxy();

        // 返回订单id
        return Result.ok(voucherId);
    }
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        Long userId = UserHolder.getUser().getId();
//        // 执行lua
//        Long result = stringRedisTemplate.execute(
//                SECKILL_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(), userId.toString()
//        );
//        // 无法购买
//        int r = result.intValue();
//        if(r != 0){
//            return Result.fail(r == 1 ? "库存不足！" : "请勿重复下单！");
//        }
//        // 可以购买，下单信息保存到阻塞队列
//        VoucherOrder voucherOrder = new VoucherOrder();
//
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//
//        voucherOrder.setUserId(userId);
//
//        voucherOrder.setVoucherId(voucherId);
//
//        orderTasks.add(voucherOrder);
//        // 获取代理对象
//        proxy = (IVoucherOrderService)AopContext.currentProxy();
//
//        // 返回订单id
//        return Result.ok(voucherId);
//    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 判断是否在秒杀时间内
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始！");
//        }
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已结束！");
//        }
//        // 库存是否充足
//        if(voucher.getStock() < 1){
//            return Result.fail("优惠券库存不足！");
//        }
//
//        // 一人一单(悲观锁)
//        Long userId = UserHolder.getUser().getId();
//        // 分布式锁
//        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock(); // 可选参数：等待时间，TTL，时间单位
//        if(!isLock){
//            return Result.fail("不允许重复下单");
//        }
//        //synchronized (userId.toString().intern()){
//            /** 1.加锁的对象应该是userId的值，转成string.intern来保证锁加在值而不是实例上(而不是直接加锁在方法/userId上)
//             * 2.锁 要加在 事务 的外面(事务提交后再释放锁)
//             * 3.使用代理对象调用事务方法
//             */
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//        //}
//    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if(count > 0){
            // 用户购买过
            log.error("不能重复购买");
            return;
        }

        // 扣减库存 (改进的乐观锁解决超卖问题)
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock",0).update();
        // set stock = stock - 1 where id = xx and stock > 0
        // 前一个update是初始化updateWrapper，后一个是提交update操作
        if(!success){
            log.error("库存不足");
            return;
        }

        // 创建订单
        save(voucherOrder);
    }
}
