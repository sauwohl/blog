-- 1.参数列表
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

-- 2.数据key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 3.脚本业务
-- 判断库存
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足
    return 1
end
-- 判断用户是否下单 SISMEMBER命令
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 重复下单
    return 2
end

-- 可以下单，扣库存，保存用户
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
-- 发送消息到队列中
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',orderId)
return 0