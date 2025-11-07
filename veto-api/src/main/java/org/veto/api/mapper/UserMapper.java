package org.veto.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.veto.api.dto.UserLoginDTO;
import org.veto.api.dto.UserRegisterDTO;
import org.veto.api.dto.UserUpdateDTO;
import org.veto.api.vo.UserSelfVO;
import org.veto.api.vo.UserVO;
import org.veto.core.command.UserLoginCommand;
import org.veto.core.command.UserRegisterCommand;
import org.veto.core.command.UserUpdateCommand;
import org.veto.core.rdbms.bean.User;
import org.veto.shared.Util;

import java.text.SimpleDateFormat;
import java.util.Date;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserLoginCommand toUserLoginCommand(UserLoginDTO userLoginDTO);

    @Mapping(target = "nickname", expression = "java(defaultStringValue(userRegisterDTO.getUsername(),userRegisterDTO.getUsername()))")
    UserRegisterCommand toUserRegisterCommand(UserRegisterDTO userRegisterDTO);

    UserUpdateCommand toUserUpdateCommand(UserUpdateDTO userUpdateDTO);

    @Mapping(target = "createdAt", expression = "java(formatDate(user.getCreatedAt()))")
    @Mapping(target = "lastLoginAt", expression = "java(formatDate(user.getLastLoginAt()))")
    @Mapping(target = "avatar", expression = "java(user.getAvatar())")
    UserSelfVO toUserSelfVO(User user);

    @Mapping(target = "createdAt", expression = "java(formatDate(user.getCreatedAt()))")
    @Mapping(target = "avatar", expression = "java(user.getAvatar())")
    UserVO toUserVO(User user);

    default Page<UserVO> toUserVOPage(Page<User> users){
        return users.map(this::toUserVO);
    }

    default String formatDate(Date date){
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    default String defaultStringValue(String val, String defaultVal){
        return Util.isBlank(val) ? defaultVal : val;
    }
}
