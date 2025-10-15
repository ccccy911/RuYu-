package xyz.kuailemao.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.kuailemao.domain.dto.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.UserAccountVO;
import xyz.kuailemao.domain.vo.UserDetailsVO;
import xyz.kuailemao.domain.vo.UserListVO;
import xyz.kuailemao.mapper.UserMapper;
import xyz.kuailemao.service.UserService;
import xyz.kuailemao.utils.ControllerUtils;
import xyz.kuailemao.utils.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("/register")
    public ResponseResult<Void> register(@RequestBody  UserRegisterDTO userRegisterDTO) {
        return userService.userRegister(userRegisterDTO);
    }

    /**
     * 更新用户状态
     * @param updateRoleStatusDTO
     * @return
     */
    @PostMapping("/update/status")
    public ResponseResult<Void> updateStatus(@RequestBody UpdateRoleStatusDTO  updateRoleStatusDTO) {
        return userService.updateRoleStatus(updateRoleStatusDTO);

    }

    /**
     * 搜索用户列表
     * @param userSearchDTO
     * @return
     */
    @PostMapping("/search")
    public ResponseResult<List<UserListVO>>  search(@RequestBody UserSearchDTO userSearchDTO) {
        return userService.searchUser(userSearchDTO);
    }


    /**
     * 重置密码
     * @param userResetPasswordDTO
     * @return
     */
    @PostMapping("/reset-password")
    public ResponseResult<Void> resetPassword(@RequestBody UserResetPasswordDTO userResetPasswordDTO){
        return userService.resetPassword(userResetPasswordDTO);
    }


    /**
     * 用户头像上传
     * @param avatarFile
     * @return
     * @throws Exception
     */
    @PostMapping("/auth/upload/avatar")
    public ResponseResult<String> uploadAvatar(@RequestParam("avatarFile") MultipartFile avatarFile) throws Exception {
        return userService.uploadAvatar(avatarFile);
    }

    /**
     * 修改用户信息
     * @param userUpdateDTO
     * @return
     */
    @PostMapping("/auth/update")
    public ResponseResult<Void> updateUser(@RequestBody  UserUpdateDTO userUpdateDTO) {
        return userService.updateUser(userUpdateDTO);
    }


    /**
     * 修改用户绑定的邮箱
     * @param updateEmailDTO
     * @return
     */
    @PostMapping("/auth/update/email")
    public ResponseResult<Void> updateEmail(@RequestBody  UpdateEmailDTO updateEmailDTO) {
        return userService.updateEmailAndVerify(updateEmailDTO);
    }

    /**
     * 获取用户列表
     * @return
     */
    @GetMapping("/list")
    public ResponseResult<List<UserListVO>> updateEmail() {
        return userService.searchUser(null);
    }


    /**
     * 获取用户详细信息
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    public ResponseResult<UserDetailsVO> getUserDetails(@PathVariable Long id) {
        return userService.getUserInfo(id);
    }


    /**
     * 获取当前登录的用户信息
     * @return
     */
    @GetMapping("/auth/info")
    public ResponseResult<UserAccountVO> getInfo() {
        return  userService.findAccountById(SecurityUtils.getUserId());
    }

    /**
     * 删除用户信息
     * @param userDeleteDTO
     * @return
     */
    @DeleteMapping("/user/delete")
    public ResponseResult<Void> deleteUser(@RequestBody UserDeleteDTO  userDeleteDTO) {
        return userService.deleteUser(userDeleteDTO);
    }






}
