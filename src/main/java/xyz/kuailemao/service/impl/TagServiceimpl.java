package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import xyz.kuailemao.domain.dto.SearchTagDTO;
import xyz.kuailemao.domain.dto.TagDTO;
import xyz.kuailemao.domain.entity.Article;
import xyz.kuailemao.domain.entity.ArticleTag;
import xyz.kuailemao.domain.entity.Tag;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TagVO;
import xyz.kuailemao.mapper.ArticleMapper;
import xyz.kuailemao.mapper.ArticleTagMapper;
import xyz.kuailemao.mapper.TagMapper;
import xyz.kuailemao.service.TagService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class TagServiceimpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    /**
     * 获取标签列表（客户端）
     * @return
     */
    @Override
    public List<TagVO> listAllTag() {
        List<Tag> tagList = this.list(null);
        List<TagVO> tagVOList = tagList.stream().map(tag -> {
            TagVO tagVO = BeanUtil.copyProperties(tag, TagVO.class);
            //统计每个标签下的文章总数
            tagVO.setArticleCount(articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId,tag.getId())));
            return tagVO;
        }).toList();
        return tagVOList;
    }

    /**
     * 新增标签-文章列表
     * @param tagDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addTag(TagDTO tagDTO) {
        if(this.save(BeanUtil.copyProperties(tagDTO,Tag.class)))
            return ResponseResult.success();
        return ResponseResult.failure();
    }


    /**
     * 修改标签
     * @param tagDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addOrUpdateTag(TagDTO tagDTO) {
        if(this.updateById(BeanUtil.copyProperties(tagDTO,Tag.class))){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    /**
     * 删除标签
     * @param ids
     * @return
     */
    @Override
    public ResponseResult<Void> deleteTagByIds(List<Long> ids) {
        //删除标签表
        //删除标签-文章中间表
        if(this.removeByIds(ids) && articleTagMapper.deleteBatchIds(ids)>0){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    /**
     * 搜索标签列表
     * @param searchTagDTO
     * @return
     */
    @Override
    public List<TagVO> searchTag(SearchTagDTO searchTagDTO) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Tag::getTagName,searchTagDTO.getTagName());
        if(searchTagDTO.getStartTime() != null && searchTagDTO.getEndTime() != null){
            queryWrapper.between(Tag::getCreateTime,searchTagDTO.getStartTime(),searchTagDTO.getEndTime());
        }
        List<Tag> tagList = this.list(queryWrapper);
        List<TagVO> tagVOList = tagList.stream().map(tag -> {
            TagVO tagVO = BeanUtil.copyProperties(tag, TagVO.class);
            tagVO.setArticleCount(articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId,tag.getId())));
            return tagVO;
        }).toList();

        return tagVOList;
    }


    /**
     * 根据id查询标签
     * @param id
     * @return
     */
    @Override
    public TagVO getTagById(Long id) {
        return tagMapper.selectById(id).asViewObject(TagVO.class);
    }


}
