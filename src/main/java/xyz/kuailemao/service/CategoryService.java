package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import xyz.kuailemao.domain.dto.CategoryDTO;
import xyz.kuailemao.domain.dto.SearchCategoryDTO;
import xyz.kuailemao.domain.entity.Category;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.CategoryVO;

import java.util.List;

public interface CategoryService  extends IService<Category> {

    ResponseResult<Void> addCategory(@Valid CategoryDTO categoryDTO);

    ResponseResult<Void> addOrUpdateCategory(@Valid CategoryDTO categoryDTO);

    List<CategoryVO> searchCategory(SearchCategoryDTO searchCategoryDTO);

    List<CategoryVO> listAllCategory();

    CategoryVO getCategoryById(Long id);

    ResponseResult<Void> deleteCategoryByIds(List<Long> ids);
}
