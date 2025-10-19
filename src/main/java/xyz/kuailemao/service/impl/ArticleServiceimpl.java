package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.lettuce.core.Limit;
import jakarta.servlet.http.HttpServletRequest;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.constants.SQLConst;
import xyz.kuailemao.domain.dto.ArticleDTO;
import xyz.kuailemao.domain.dto.SearchArticleDTO;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.*;
import xyz.kuailemao.enums.*;
import xyz.kuailemao.exceptions.FileUploadException;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.*;
import xyz.kuailemao.utils.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class ArticleServiceimpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleMapper articleMapper;


    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;


    @Autowired
    private RedisCache redisCache;

    /**
     * 前端获取所有文章列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVO<List<ArticleVO>> listAllArticle(Integer pageNum, Integer pageSize) {
        boolean hasKey = redisCache.isHasKey(RedisConst.ARTICLE_COMMENT_COUNT) && redisCache.isHasKey(RedisConst.ARTICLE_FAVORITE_COUNT) && redisCache.isHasKey(RedisConst.ARTICLE_LIKE_COUNT);
        // 文章
        Page<Article> page = new Page<>(pageNum, pageSize);
        this.page(page, new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE).orderByDesc(Article::getCreateTime));
        List<Article> list = page.getRecords();
        // 文章分类
        // 1. 优化：使用 Map 存储分类和标签信息，避免 N+1 问题
        // 如果 list 为空，直接返回空分页结果（无需后续查询）
        if (list.isEmpty()) {
            return new PageVO<>(Collections.emptyList(), 0L); // 空列表 + 总条数0
        }
        Map<Long, String> categoryMap = categoryMapper.selectBatchIds(list.stream().map(Article::getCategoryId).toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        List<ArticleTag> articleTags = articleTagMapper.selectBatchIds(list.stream().map(Article::getId).toList());
        Map<Long, String> tagMap = tagMapper.selectBatchIds(articleTags.stream().map(ArticleTag::getTagId).toList())
                .stream().collect(Collectors.toMap(Tag::getId, Tag::getTagName));

        List<ArticleVO> articleVOS = list.stream().map(article -> {
            ArticleVO articleVO = article.asViewObject(ArticleVO.class);
            // 2. 优化：使用 Map 获取分类和标签信息
            articleVO.setCategoryName(categoryMap.get(article.getCategoryId()));
            articleVO.setTags(articleTags.stream()
                    .filter(at -> Objects.equals(at.getArticleId(), article.getId()))
                    .map(at -> tagMap.get(at.getTagId()))
                    .toList());
            return articleVO;
        }).toList();

        if (hasKey) {
            articleVOS = articleVOS.stream().peek(articleVO -> {
                setArticleCount(articleVO, RedisConst.ARTICLE_FAVORITE_COUNT, CountTypeEnum.FAVORITE);
                setArticleCount(articleVO, RedisConst.ARTICLE_LIKE_COUNT, CountTypeEnum.LIKE);
                setArticleCount(articleVO, RedisConst.ARTICLE_COMMENT_COUNT, CountTypeEnum.COMMENT);
            }).toList();
        }

        return new PageVO<>(articleVOS, page.getTotal());
    }


    /**
     * 获取推荐的文章信息（前端展示）
     * @return
     */
    @Override
    public List<RecommendArticleVO> listRecommendArticle() {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsTop, SQLConst.RECOMMEND_ARTICLE).and(i -> i.eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        List<Article> articles = articleMapper.selectList(wrapper);
        return articles.stream().map(article -> article.asViewObject(RecommendArticleVO.class)).toList();
    }

    /**
     * 获取随机的文章信息
     * @return
     */
    @Override
    public List<RandomArticleVO> listRandomArticle() {
        List<Article> randomArticles = articleMapper.selectRandomArticles(SQLConst.PUBLIC_ARTICLE, SQLConst.RANDOM_ARTICLE_COUNT);
        return randomArticles.stream()
                .map(article -> article.asViewObject(RandomArticleVO.class))
                .toList();
    }

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FavoriteService favoriteService;


    /**
     * 获取文章详情
     * @param id 文章id
     * @return
     */
    @Override
    public ArticleDetailVO getArticleDetail(Integer id) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE).and(i -> i.eq(Article::getId, id)));
        if (StringUtils.isNull(article)) return null;
        // 文章分类
        Category category = categoryMapper.selectById(article.getCategoryId());
        // 文章关系
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId()));
        // 标签
        List<Tag> tags = tagMapper.selectBatchIds(articleTags.stream().map(ArticleTag::getTagId).toList());
        // 当前文章的上一篇文章与下一篇文章,大于当前文章的最小文章与小于当前文章的最大文章
        LambdaQueryWrapper<Article> preAndNextWrapper = new LambdaQueryWrapper<>();
        preAndNextWrapper.lt(Article::getId, id);
        Article preArticle = articleMapper.selectOne(preAndNextWrapper.orderByDesc(Article::getId).last(SQLConst.LIMIT_ONE_SQL));
        preAndNextWrapper.clear();
        preAndNextWrapper.gt(Article::getId, id);
        Article nextArticle = articleMapper.selectOne(preAndNextWrapper.orderByAsc(Article::getId).last(SQLConst.LIMIT_ONE_SQL));

        return article.asViewObject(ArticleDetailVO.class, vo -> {
            vo.setCategoryName(category.getCategoryName());
            vo.setCategoryId(category.getId());
            vo.setTags(tags.stream().map(tag -> tag.asViewObject(TagVO.class)).toList());
            vo.setCommentCount(commentService.count(new LambdaQueryWrapper<Comment>().eq(Comment::getTypeId, article.getId()).eq(Comment::getType, CommentEnum.COMMENT_TYPE_ARTICLE.getType())));
            vo.setLikeCount(likeService.count(new LambdaQueryWrapper<Like>().eq(Like::getTypeId, article.getId()).eq(Like::getType, LikeEnum.LIKE_TYPE_ARTICLE.getType())));
            vo.setFavoriteCount(favoriteService.count(new LambdaQueryWrapper<Favorite>().eq(Favorite::getTypeId, article.getId()).eq(Favorite::getType, FavoriteEnum.FAVORITE_TYPE_ARTICLE.getType())));
            vo.setPreArticleId(preArticle == null ? 0 : preArticle.getId());
            vo.setPreArticleTitle(preArticle == null ? "" : preArticle.getArticleTitle());
            vo.setNextArticleId(nextArticle == null ? 0 : nextArticle.getId());
            vo.setNextArticleTitle(nextArticle == null ? "" : nextArticle.getArticleTitle());
        });
    }


    /**
     * 相关文章信息
     * @param categoryId 文章分类id
     * @param articleId 文章id
     * @return
     */
    @Override
    public List<RelatedArticleVO> relatedArticleList(Integer categoryId, Integer articleId) {
        List<Article> articleList = this.baseMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE)
        .eq(Article::getCategoryId, categoryId).ne(Article::getId,articleId)
                .last("Limit 5"));
        if(articleList == null || articleList.isEmpty()) return Collections.emptyList();
        List<RelatedArticleVO> relatedArticleVOList  = articleList.stream().map(article -> article.asViewObject(RelatedArticleVO.class)).toList();
        return relatedArticleVOList;
    }

    /**
     * 获取时间轴信息
     * @return
     */
    @Override
    public List<TimeLineVO> listTimeLine() {
        List<Article> articleList = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        List<TimeLineVO> timeLineVOList = articleList.stream().map(article -> article.asViewObject(TimeLineVO.class)).toList();
        if(timeLineVOList == null || timeLineVOList.isEmpty()) return Collections.emptyList();
        return timeLineVOList;
    }

    /**
     * 获取分类与标签下的文章
     * @param type   类型
     * @param typeId 类型id
     * @return
     */
    @Override
    public List<CategoryArticleVO> listCategoryArticle(Integer type, Long typeId) {
        List<Article> articles = null;
        //如果是分类，根据id获取分类对象（分类名称需要）
        if(type == 0){
            articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE)
                    .eq(Article::getCategoryId, typeId));
            String categoryName = categoryMapper.selectById(typeId).getCategoryName();
        }
        //如果是标签，根据标签id获取文章id的集合
        else if(type == 1){
            List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId,typeId));
            List<Long> articleIds = articleTags.stream().map(ArticleTag::getArticleId).collect(Collectors.toList());
            //获取文章
            articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().in(Article::getId,articleIds));
            //获取分类
            List<Category>  categoryList = categoryMapper.selectList(new LambdaQueryWrapper<Category>().in(Category::getId, articles.stream().map(Article::getCategoryId).collect(Collectors.toList())));
            Map<Long,String>  categoryMap = categoryList.stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));
        }
        //获取标签
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId,articles.stream().map(Article::getId).collect(Collectors.toList())));
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, articleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toList())));
        List<TagVO> tagVOs = tags.stream().map(tag -> tag.asViewObject(TagVO.class)).toList();
        tagVOs.forEach(tagVO -> {
            tagVO.setArticleCount(articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagVO.getId())));
        });
        Map<Long,TagVO> tagMap = tagVOs.stream().collect(Collectors.toMap(TagVO::getId,tagVO -> tagVO));
        Map<Long,List<TagVO>> map = articleTags.stream().collect(Collectors.groupingBy(ArticleTag::getArticleId,Collectors.mapping(articleTag ->tagMap.get(articleTag.getTagId()),Collectors.toList())));

        List<CategoryArticleVO> resultList = articles.stream().map(article ->{
            CategoryArticleVO categoryArticleVO = BeanUtil.copyProperties(article, CategoryArticleVO.class);
            categoryArticleVO.setTags(map.get(article.getId()));
            return categoryArticleVO;
        }).toList();

        return resultList;
    }


    /**
     * 文章访问量+1
     * @param id 文章id
     */
    @Override
    public void addVisitCount(Long id) {
        // 访问量去重
        HttpServletRequest request = SecurityUtils.getCurrentHttpRequest();
        // key + id + ip + time(秒)
        String KEY = RedisConst.ARTICLE_VISIT_COUNT_LIMIT + id + ":" + IpUtils.getIpAddr(request);
        if (redisCache.getCacheObject(KEY) == null) {
            // 设置间隔时间
            redisCache.setCacheObject(KEY, 1, RedisConst.ARTICLE_VISIT_COUNT_INTERVAL, TimeUnit.SECONDS);

            if (redisCache.isHasKey(RedisConst.ARTICLE_VISIT_COUNT + id))
                redisCache.increment(RedisConst.ARTICLE_VISIT_COUNT + id, 1L);
            else redisCache.setCacheObject(RedisConst.ARTICLE_VISIT_COUNT + id, 0);
        }

    }

    @Autowired
    private FileUploadUtils fileUploadUtils;

    /**
     * 上传文章封面
     * @param articleCover 文章封面
     * @return
     */
    @Override
    public ResponseResult<String> uploadArticleCover(MultipartFile articleCover) {
        try {
            String articleCoverUrl = null;
            try {
                articleCoverUrl = fileUploadUtils.upload(UploadEnum.ARTICLE_COVER, articleCover);
            } catch (FileUploadException e) {
                return ResponseResult.failure(e.getMessage());
            }
            if (StringUtils.isNotNull(articleCoverUrl))
                return ResponseResult.success(articleCoverUrl);
            else
                return ResponseResult.failure("上传格式错误");
        } catch (Exception e) {
            log.error("文章封面上传失败", e);
            return ResponseResult.failure();
        }
    }

    @Autowired
    private ArticleTagService articleTagService;

    @Override
    public ResponseResult<Void> publish(ArticleDTO articleDTO) {
        Article article = articleDTO.asViewObject(Article.class, v -> v.setUserId(SecurityUtils.getUserId()));
        if (this.saveOrUpdate(article)) {
            // 清除标签关系
            articleTagMapper.deleteById(article.getId());
            // 新增标签关系
            List<ArticleTag> articleTags = articleDTO.getTagId().stream().map(articleTag -> ArticleTag.builder().articleId(article.getId()).tagId(articleTag).build()).toList();
            articleTagService.saveBatch(articleTags);
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    /**
     * 删除文章封面
     * @param articleCoverUrl 文章封面
     * @return
     */

    @Override
    public ResponseResult<Void> deleteArticleCover(String articleCoverUrl) {
        try {
            // 提取图片名称
            String articleCoverName = articleCoverUrl.substring(articleCoverUrl.indexOf(articleCoverUrl) + articleCoverUrl.length());
            fileUploadUtils.deleteFiles(List.of(articleCoverName));
            return ResponseResult.success();
        } catch (Exception e) {
            log.error("删除文章封面失败", e);
            return ResponseResult.failure();
        }
    }

    /**
     * 上传文章图片
     * @param articleImage 文章图片
     * @return
     */
    @Override
    public ResponseResult<String> uploadArticleImage(MultipartFile articleImage) {
        try {
            String url = fileUploadUtils.upload(UploadEnum.ARTICLE_IMAGE, articleImage);
            if (StringUtils.isNotNull(url))
                return ResponseResult.success(url);
            else
                return ResponseResult.failure("上传格式错误");
        } catch (Exception e) {
            log.error("文章图片上传失败", e);
        }
        return null;
    }


    @Autowired
    private UserMapper userMapper;

    /**
     * 后台获取所有的文章列表
     * @return
     */
    @Override
    public List<ArticleListVO> listArticle() {
        List<Article> articleList = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        if(articleList == null || articleList.isEmpty()) return Collections.emptyList();
        //分类名称和标签名称
        List<Long> categoryIdList = articleList.stream().map(Article::getCategoryId).toList();
        List<Category> categoryList = categoryMapper.selectList(new LambdaQueryWrapper<Category>().in(Category::getId, categoryIdList));
        Map<Long,String> categoryMap = categoryList.stream().collect(Collectors.toMap(Category::getId,Category::getCategoryName));

        List<Long> articleIdList = articleList.stream().map(Article::getId).collect(Collectors.toList());
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getTagId, articleIdList));
        Map<Long, String> tagMap = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, articleTags.stream().map(tag -> tag.getTagId()).toList())).stream().collect(Collectors.toMap(Tag::getId, Tag::getTagName));
        Map<Long, List<String>> collect = articleTags.stream()
                .collect(Collectors.groupingBy(
                        // 第1个参数：按什么分组？→ 按文章ID分组
                        ArticleTag::getArticleId,

                        // 第2个参数：每组数据如何转换？→ 把每组关联关系转为标签名列表
                        Collectors.mapping(
                                // 先把 ArticleTag 转换为标签名（用 tagNameMap 查）
                                rel -> tagMap.get(rel.getTagId()),
                                // 再把多个标签名收集成 List
                                Collectors.toList()
                        )
                ));
        List<ArticleListVO> resultList = articleList.stream().map(article -> {
            ArticleListVO articleListVO = BeanUtil.copyProperties(article, ArticleListVO.class);
            articleListVO.setCategoryName(categoryMap.get(article.getCategoryId()));
            articleListVO.setUserName(userMapper.selectById(articleListVO.getUserId()).getUsername());
            articleListVO.setTagsName(collect.get(article.getId()));
            return articleListVO;
        }).toList();
        return resultList;
    }

    /**
     *
     * @param searchArticleDTO 条件搜索文章
     * @return
     */
    @Override
    public List<ArticleListVO> searchArticle(SearchArticleDTO searchArticleDTO) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus,searchArticleDTO.getStatus())
                .eq(Article::getIsTop,searchArticleDTO.getIsTop())
                .like(Article::getArticleTitle, searchArticleDTO.getArticleTitle())
                .eq(Article::getCategoryId, searchArticleDTO.getCategoryId());
        List<Article> articleList = articleMapper.selectList(queryWrapper);
        if(articleList == null || articleList.isEmpty()) return Collections.emptyList();
        //标签名称和分类名称和作者名称
        List<Long> categoryIdList = articleList.stream().map(Article::getCategoryId).collect(Collectors.toList());
        List<Category> categoryList = categoryMapper.selectList(new LambdaQueryWrapper<Category>().in(Category::getId, categoryIdList));
        Map<Long,String> categoryMap = categoryList.stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        List<Long> articleIdList = articleList.stream().map(Article::getId).collect(Collectors.toList());
        List<ArticleTag> articleTags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIdList));
        Map<Long,String> tags  = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, articleTags.stream().map(tag -> tag.getTagId()).toList())).stream().collect(Collectors.toMap(Tag::getId, Tag::getTagName));
        Map<Long,List<String>>  tagMap = articleTags.stream().collect(Collectors.groupingBy(ArticleTag::getArticleId,Collectors.mapping(item->tags.get(item.getTagId()),Collectors.toList())));

        List<ArticleListVO> articleVOList = articleList.stream().map(article -> {
            ArticleListVO articleVO = BeanUtil.copyProperties(article, ArticleListVO.class);
            articleVO.setCategoryName(categoryMap.get(article.getCategoryId()));
            articleVO.setTagsName(tagMap.get(article.getId()));
            articleVO.setUserName(userMapper.selectById(article.getUserId()).getUsername());
            return articleVO;
        }).toList();
        return articleVOList;

    }

    /**
     * 修改文章状态
     * @param id 文章id
     * @param status 状态
     * @return
     */
    @Override
    public ResponseResult<Void> updateStatus(Long id, Integer status) {
        Article article = Article.builder().id(id).status(status).build();
        if(articleMapper.updateById(article)>0) return ResponseResult.success();
        return ResponseResult.failure();
    }

    /**
     * 修改文章是否置顶
     */
    @Override
    public ResponseResult<Void> updateIsTop(Long id, Integer isTop) {
        Article article = Article.builder().id(id).isTop(isTop).build();
        if(articleMapper.updateById(article)>0) return ResponseResult.success();
        return  ResponseResult.failure();
    }

    /**
     * 回显文章数据
     * @param id 文章id
     * @return
     */
    @Override
    public ArticleDTO getArticleDTO(Long id) {
        ArticleDTO articleDTO = articleMapper.selectById(id).asViewObject(ArticleDTO.class);
        if (StringUtils.isNotNull(articleDTO)) {
            // 查询文章标签
            List<Long> tagIds = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleDTO.getId())).stream().map(ArticleTag::getTagId).toList();
            articleDTO.setTagId(tagMapper.selectBatchIds(tagIds).stream().map(Tag::getId).toList());
            return articleDTO;
        }
        return null;
    }

    @Autowired
    private  LikeMapper likeMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 删除文章
     * @param id 文章id
     * @return
     */
    @Override
    public ResponseResult<Void> deleteArticle(List<Long> id) {
        //删除文章表和文章-标签中间表数据
        if(articleMapper.deleteBatchIds(id)>0&&articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId,id))>0) {
            // 删除点赞、收藏、评论
            likeMapper.delete(new LambdaQueryWrapper<Like>().eq(Like::getType, LikeEnum.LIKE_TYPE_ARTICLE.getType()).and(a -> a.in(Like::getTypeId, id)));
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getType, FavoriteEnum.FAVORITE_TYPE_ARTICLE.getType()).and(a -> a.in(Favorite::getTypeId, id)));
            commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getType, CommentEnum.COMMENT_TYPE_ARTICLE.getType()).and(a -> a.in(Comment::getTypeId, id)));
        }
        return ResponseResult.failure();
    }

    private void setArticleCount(ArticleVO articleVO, String redisKey, CountTypeEnum articleFieldName) {

        String articleId = articleVO.getId().toString();
        Object countObj = redisCache.getCacheMap(redisKey).get(articleId);
        long count = 0L;
        if (countObj != null) {
            count = Long.parseLong(countObj.toString());
        } else {
            // 缓存发布新文章时数量缓存不存在
            redisCache.setCacheMap(redisKey, Map.of(articleId, 0));
        }

        if (articleFieldName.equals(CountTypeEnum.FAVORITE)) {
            articleVO.setFavoriteCount(count);
        } else if (articleFieldName.equals(CountTypeEnum.LIKE)) {
            articleVO.setLikeCount(count);
        } else if (articleFieldName.equals(CountTypeEnum.COMMENT)) {
            articleVO.setCommentCount(count);
        }
    }




    /**
     * 根据标题搜索文章
     * @return
     */
    @Override
    public List<InitSearchTitleVO> initSearchByTitle() {
        List<Article> articles = this.list(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));
        if (articles.isEmpty()) {
            return List.of();
        }
        return articles.stream().map(article -> article.asViewObject(InitSearchTitleVO.class, item -> item.setCategoryName(categoryMap.get(article.getCategoryId())))).toList();
    }

    /**
     * 前端获取热门推荐文章
     * @return
     */
    @Override
    public List<HotArticleVO> listHotArticle() {
        List<Article> articles = this.articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE).last("limit 5"));
        if(!articles.isEmpty()){
            List<HotArticleVO> hotArticleVOs = articles.stream().map(article -> BeanUtil.copyProperties(article,HotArticleVO.class)).toList();
            return hotArticleVOs;
        }
        return List.of();
    }

    /**
     * 根据内容搜索文章
     * @param keyword 文章内容
     * @return
     */
    @Override
    public List<SearchArticleByContentVO> searchArticleByContent(String keyword) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().like(Article::getArticleContent, keyword).eq(Article::getStatus, SQLConst.PUBLIC_ARTICLE));
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));
        if (!articles.isEmpty()) {
            List<SearchArticleByContentVO> listVos = articles.stream().map(article -> article.asViewObject(SearchArticleByContentVO.class, vo -> {
                vo.setCategoryName(categoryMap.get(article.getCategoryId()));
            })).toList();
            int index = -1;
            for (SearchArticleByContentVO articleVo : listVos) {
                String content = articleVo.getArticleContent();
                index = content.toLowerCase().indexOf(keyword.toLowerCase());
                if (index != -1) {
                    int end = Math.min(content.length(), index + keyword.length() + 20);
                    String highlighted = keyword + content.substring(index + keyword.length(), end);
                    articleVo.setArticleContent(stripMarkdown(highlighted));
                }
            }
            if (index != -1) {
                return listVos;
            }
        }
        return List.of();
    }


    /**
     * 去掉markdown格式
     *
     * @param markdown markdown
     * @return txt
     */
    private String stripMarkdown(String markdown) {
        return markdown.replaceAll("(?m)^\\s*#.*$", "") // 去掉标题
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // 去掉加粗
                .replaceAll("\\*(.*?)\\*", "$1") // 去掉斜体
                .replaceAll("`([^`]*)`", "$1") // 去掉行内代码
                .replaceAll("~~(.*?)~~", "$1") // 去掉删除线
                .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1") // 去掉链接
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "") // 去掉图片
                .replaceAll(">\\s?", "") // 去掉引用
                .replaceAll("(?m)^\\s*[-*+]\\s+", "") // 去掉无序列表
                .replaceAll("(?m)^\\s*\\d+\\.\\s+", "") // 去掉有序列表
                .replaceAll("\\n", " "); // 去掉换行符
    }

}
