package com.likebang.modules.ranking.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.common.dto.PageParam;
import com.likebang.common.exception.BusinessException;
import com.likebang.common.result.ResultCode;
import com.likebang.common.utils.DateTimeUtils;
import com.likebang.modules.ranking.dto.request.CategoryCreateRequest;
import com.likebang.modules.ranking.dto.request.CategoryUpdateRequest;
import com.likebang.modules.ranking.dto.response.CategoryResponse;
import com.likebang.modules.ranking.entity.RankingCategory;
import com.likebang.modules.ranking.mapper.RankingCategoryMapper;
import com.likebang.modules.ranking.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 排名分类 业务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final RankingCategoryMapper categoryMapper;

    @Override
    public IPage<CategoryResponse> page(PageParam pageParam) {
        LambdaQueryWrapper<RankingCategory> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageParam.getKeyword())) {
            wrapper.like(RankingCategory::getName, pageParam.getKeyword())
                    .or().like(RankingCategory::getDescription, pageParam.getKeyword());
        }
        wrapper.orderByAsc(RankingCategory::getSort)
                .orderByDesc(RankingCategory::getCreatedAt);

        // 统一经 PageParam.toPage() 构建：非法页码兜底 + size 上限钳制
        Page<RankingCategory> page = categoryMapper.selectPage(pageParam.toPage(), wrapper);

        return page.convert(this::convert);
    }

    @Override
    public List<CategoryResponse> listAllEnabled() {
        LambdaQueryWrapper<RankingCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RankingCategory::getStatus, 1)
                .orderByAsc(RankingCategory::getSort);
        List<RankingCategory> list = categoryMapper.selectList(wrapper);
        return list.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getById(Long id) {
        RankingCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        return convert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CategoryCreateRequest request) {
        LambdaQueryWrapper<RankingCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RankingCategory::getName, request.getName());
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }

        RankingCategory category = new RankingCategory();
        BeanUtils.copyProperties(request, category);
        category.setCreatedAt(DateTimeUtils.now());
        category.setUpdatedAt(DateTimeUtils.now());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            // 应用层查重挡不住并发（check-then-act），uk_name 唯一键是最终防线
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryUpdateRequest request) {
        RankingCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }

        // 局部更新：只写实际变更的列，避免整档回写覆盖他人并发修改（丢失更新）
        RankingCategory update = new RankingCategory();
        update.setId(id);

        // 改名需与他档查重（自身同值不改），并发改名由 uk_name 唯一键兜底
        boolean renaming = StrUtil.isNotBlank(request.getName())
                && !Objects.equals(request.getName(), category.getName());
        if (renaming) {
            Long count = categoryMapper.selectCount(Wrappers.<RankingCategory>lambdaQuery()
                    .eq(RankingCategory::getName, request.getName())
                    .ne(RankingCategory::getId, id));
            if (count != null && count > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称已存在");
            }
            update.setName(request.getName());
        }
        if (request.getDescription() != null) {
            update.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            update.setIconUrl(request.getIconUrl());
        }
        if (request.getSort() != null) {
            update.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }

        // 仅传名称且与库中一致时，无任何字段需要更新
        if (update.getName() == null && update.getDescription() == null && update.getIconUrl() == null
                && update.getSort() == null && update.getStatus() == null) {
            return;
        }

        update.setUpdatedAt(DateTimeUtils.now());
        try {
            categoryMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        RankingCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        categoryMapper.deleteById(id);
    }

    private CategoryResponse convert(RankingCategory source) {
        CategoryResponse target = new CategoryResponse();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
