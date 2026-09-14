package com.likebang.modules.ranking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.common.result.Result;
import com.likebang.modules.ranking.dto.request.CategoryCreateRequest;
import com.likebang.modules.ranking.dto.request.CategoryUpdateRequest;
import com.likebang.modules.ranking.dto.response.CategoryResponse;
import com.likebang.modules.ranking.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排名分类 接口
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分页查询分类列表
     */
    @GetMapping("/page")
    public Result<IPage<CategoryResponse>> page(@ModelAttribute PageParam pageParam) {
        return Result.success(categoryService.page(pageParam));
    }

    /**
     * 查询所有启用分类（导航用）
     */
    @GetMapping
    public Result<List<CategoryResponse>> list() {
        return Result.success(categoryService.listAllEnabled());
    }

    /**
     * 分类详情
     */
    @GetMapping("/{id}")
    public Result<CategoryResponse> detail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /**
     * 新增分类
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryCreateRequest request) {
        categoryService.create(request);
        return Result.success();
    }

    /**
     * 修改分类
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.update(id, request);
        return Result.success();
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
