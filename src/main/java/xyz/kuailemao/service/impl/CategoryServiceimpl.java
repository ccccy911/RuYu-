package xyz.kuailemao.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import xyz.kuailemao.domain.dto.CategoryDTO;
import xyz.kuailemao.domain.dto.SearchCategoryDTO;
import xyz.kuailemao.domain.entity.Article;
import xyz.kuailemao.domain.entity.Category;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.CategoryArticleVO;
import xyz.kuailemao.domain.vo.CategoryVO;
import xyz.kuailemao.mapper.ArticleMapper;
import xyz.kuailemao.mapper.CategoryMapper;
import xyz.kuailemao.service.CategoryService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceimpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {

    private final ArticleMapper articleMapper;

    public CategoryServiceimpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /**
     * 新增分类——文章列表
     * @param categoryDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addCategory(CategoryDTO categoryDTO) {
        categoryDTO.setId(null);
        if(this.save(BeanUtil.copyProperties(categoryDTO,Category.class))){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addOrUpdateCategory(CategoryDTO categoryDTO) {
        if(this.updateById(BeanUtil.copyProperties(categoryDTO,Category.class))){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 搜索分类列表
     * @param searchCategoryDTO
     * @return
     */
    @Override
    public List<CategoryVO> searchCategory(SearchCategoryDTO searchCategoryDTO) {
        LambdaQueryWrapper<Category> queryWrapper = Wrappers.<Category>lambdaQuery();
        queryWrapper.like(Category::getCategoryName,searchCategoryDTO.getCategoryName());
        if(searchCategoryDTO.getStartTime() != null && searchCategoryDTO.getEndTime() != null){
            queryWrapper.between(Category::getCreateTime, searchCategoryDTO.getStartTime(), searchCategoryDTO.getEndTime());
        }
        List<Category> categoryList = this.list(queryWrapper);
        List<CategoryVO> categoryVOList = categoryList.stream().map(category -> {
            CategoryVO categoryVO = BeanUtil.copyProperties(category,CategoryVO.class);
            categoryVO.setArticleCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getCategoryId,category.getId())));
            return categoryVO;
        }).toList();
        return categoryVOList;
    }


    /**
     * 获取所有分类
     * @return
     */
    @Override
    public List<CategoryVO> listAllCategory() {
        List<Category> categorylist = this.list();
        return categorylist.stream().map(category -> {
            CategoryVO categoryVO = BeanUtil.copyProperties(category,CategoryVO.class);
            categoryVO.setArticleCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getCategoryId,category.getId())));
            return categoryVO;
        }).toList();
    }

    /**
     * 根据id查询分类
     * @param id
     * @return
     */
    @Override
    public CategoryVO getCategoryById(Long id) {
        Category category = this.getById(id);
        CategoryVO categoryVO = BeanUtil.copyProperties(category,CategoryVO.class);
        categoryVO.setArticleCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getCategoryId,id)));
        return categoryVO;
    }


    /**
     * 删除分类
     * @param ids
     * @return
     */
    @Override
    public ResponseResult<Void> deleteCategoryByIds(List<Long> ids) {
        if(ids != null && ids.size() > 0){
            this.removeByIds(ids);
            articleMapper.delete(new LambdaQueryWrapper<Article>().in(Article::getCategoryId,ids));
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


}
