package com.likebang.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likebang.modules.user.dto.request.UserCreateRequest;
import com.likebang.modules.user.dto.request.UserUpdateRequest;
import com.likebang.modules.user.dto.response.UserResponse;

public interface UserService {

    IPage<UserResponse> page(Page<UserResponse> page, String keyword);

    UserResponse getById(Long id);

    void create(UserCreateRequest request);

    void update(Long id, UserUpdateRequest request);

    void delete(Long id);
}
