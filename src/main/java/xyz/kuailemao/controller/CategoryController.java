package xyz.kuailemao.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.CategoryDTO;
import xyz.kuailemao.domain.dto.SearchCategoryDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.CategoryVO;
import xyz.kuailemao.service.CategoryService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类——文章列表
     * @param categoryDTO
     * @return
     */
    @PutMapping()
    public ResponseResult<Void> addCategory(@RequestBody @Valid CategoryDTO categoryDTO) {
        return categoryService.addCategory(categoryDTO);
    }


    /**
     * 新增分类-分类列表
     * @param categoryDTO
     * @return
     */
    @PutMapping("/back/add")
    public ResponseResult<Void> addOrUpdateCategory(@RequestBody @Valid CategoryDTO categoryDTO) {
        return categoryService.addCategory(categoryDTO);
    }



    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PostMapping("/back/update")
    public ResponseResult<Void> updateCategory(@RequestBody @Valid CategoryDTO categoryDTO) {
        return categoryService.addOrUpdateCategory(categoryDTO);
    }




    /**
     * 搜索分类列表
     * @param searchCategoryDTO
     * @return
     */
    @PostMapping("/back/search")
    public ResponseResult<List<CategoryVO>> searchCategory(@RequestBody SearchCategoryDTO searchCategoryDTO) {
        return ControllerUtils.messageHandler(() -> categoryService.searchCategory(searchCategoryDTO));
    }

    /**
     * 获取所有分类
     * @return
     */
    @GetMapping("/list")
    public ResponseResult<List<CategoryVO>> listAllCategory() {
        return ControllerUtils.messageHandler((categoryService::listAllCategory));
    }


    /**
     * 获取分类列表
     * @return
     */
    @GetMapping("/back/list")
    public ResponseResult<List<CategoryVO>> listArticleCategory() {
        return ControllerUtils.messageHandler((categoryService::listAllCategory));
    }

    /**
     * 根据id查询分类
     * @param id
     * @return
     */
    @GetMapping("/back/get/{id}")
    public ResponseResult<CategoryVO> getCategoryById(@PathVariable(value = "id") Long id) {
        return ControllerUtils.messageHandler(() -> categoryService.getCategoryById(id));
    }


    /**
     * 删除分类
     * @param ids
     * @return
     */
    @DeleteMapping("/back/delete")
    public ResponseResult<Void> deleteCategory(@RequestBody List<Long> ids) {
        return categoryService.deleteCategoryByIds(ids);
    }



}
