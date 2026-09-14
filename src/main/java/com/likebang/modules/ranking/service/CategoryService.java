package com.likebang.modules.ranking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.likebang.common.dto.PageParam;
import com.likebang.modules.ranking.dto.request.CategoryCreateRequest;
import com.likebang.modules.ranking.dto.request.CategoryUpdateRequest;
import com.likebang.modules.ranking.dto.response.CategoryResponse;

import java.util.List;

/**
 * 排名分类 业务接口
 */
public interface CategoryService {

    /**
     * 分页查询分类列表
     */
    IPage<CategoryResponse> page(PageParam pageParam);

    /**
     * 查询所有启用的分类（按sort排序，用于前端下拉/导航）
     */
    List<CategoryResponse> listAllEnabled();

    /**
     * 根据ID查详情
     */
    CategoryResponse getById(Long id);

    /**
     * 新增分类
     */
    void create(CategoryCreateRequest request);

    /**
     * 修改分类
     */
    void update(Long id, CategoryUpdateRequest request);

    /**
     * 删除分类
     */
    void delete(Long id);
}
