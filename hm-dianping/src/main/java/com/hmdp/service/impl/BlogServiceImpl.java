package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.querBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 查询blog
        Blog blog = getById(id);
        if(blog == null){
            return Result.fail("笔记不存在");
        }
        // 查询blog用户信息
        querBlogUser(blog);
        // 查询blog是否点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        // 获取并判断当前登录用户是否点赞
        UserDTO user = UserHolder.getUser();
        if(user==null){
            // 用户未登录
            return;
        }
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        // 获取并判断当前登录用户是否点赞
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score == null){
            // 未点赞:修改db;保存到redis的zset集合
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if(isSuccess){
                // ZSet即sortedSet 参数为:key value score
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }else{
            // 已点赞:修改db;从redis移除
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogByLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 查询top5点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        // 解析用户id并查询用户(按时间戳排序)
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id",ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("笔记发布失败！");
        }
        // 查询粉丝
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 推送笔记给粉丝(每个粉丝一个收件箱)
        for(Follow follow : follows){
            Long userId = follow.getUserId();
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 查询当前用户收件箱 ZREVERANGEBYSCORE
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
        // 解析数据: blogId, minTime(时间戳,即score), offset(与上次最小值相同的个数)
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1; // offset
        for( ZSetOperations.TypedTuple<String> tuple : typedTuples){
            ids.add(Long.valueOf(tuple.getValue()));
            // 滚动分页，下次从上次读到的最小值(即最后一个元素)继续
            long time = tuple.getScore().longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }
        // 查blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 查询blog用户信息
            querBlogUser(blog);
            // 查询blog是否点赞
            isBlogLiked(blog);
        }
        
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);
        return Result.ok(r);
    }

    private void querBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}
