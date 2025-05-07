package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 判断是否在秒杀时间内
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始！");
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束！");
        }
        // 库存是否充足
        if(voucher.getStock() < 1){
            return Result.fail("优惠券库存不足！");
        }

        // 一人一单(悲观锁)
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()){
            // 1.加锁的对象应该是userId的值，转成string.intern来保证锁加在值而不是实例上(而不是直接加锁在方法/userId上)
            // 2.锁 要加在 事务 的外面(事务提交后再释放锁)
            // 3.使用代理对象调用事务方法
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }

    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count > 0){
            // 用户购买过
            return Result.fail("用户已经购买过该优惠券!");
        }

        // 扣减库存 (改进的乐观锁解决超卖问题)
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock",0).update();
        // set stock = stock - 1 where id = xx and stock > 0
        // 前一个update是初始化updateWrapper，后一个是提交update操作
        if(!success){
            return Result.fail("优惠券库存不足！");
        }

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();

        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);

        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        return Result.ok(userId);
    }
}
