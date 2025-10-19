package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;
import xyz.kuailemao.domain.dto.*;
import xyz.kuailemao.domain.entity.User;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.UserAccountVO;
import xyz.kuailemao.domain.vo.UserDetailsVO;
import xyz.kuailemao.domain.vo.UserListVO;

import java.util.List;

public interface UserService extends IService<User>, UserDetailsService {

    ResponseResult<Void> userRegister(UserRegisterDTO userRegisterDTO);

    ResponseResult<Void> updateRoleStatus(UpdateRoleStatusDTO updateRoleStatusDTO);
    ResponseResult<List<UserListVO>> searchUser(UserSearchDTO userSearchDTO);

    ResponseResult<Void> resetPassword(UserResetPasswordDTO userResetPasswordDTO);

    ResponseResult<String> uploadAvatar(MultipartFile avatarFile) throws Exception;

    ResponseResult<Void> updateUser(UserUpdateDTO userUpdateDTO);

    ResponseResult<Void> updateEmailAndVerify(UpdateEmailDTO updateEmailDTO);

    ResponseResult<UserDetailsVO> getUserInfo(Long id);

    ResponseResult<UserAccountVO> findAccountById(Long userId);

    ResponseResult<Void> deleteUser(UserDeleteDTO userDeleteDTO);


    /**
     * 用户登录状态
     * @param id 用户id
     * @param type 登录类型
     */
    void userLoginStatus(Long id, Integer type);
}
