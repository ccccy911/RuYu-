package xyz.kuailemao.service.impl;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.vo.RoleVO;
import xyz.kuailemao.domain.vo.UserAccountVO;
import xyz.kuailemao.service.IpService;
import xyz.kuailemao.utils.*;
import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import xyz.kuailemao.constants.*;
import xyz.kuailemao.domain.dto.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.UserDetailsVO;
import xyz.kuailemao.domain.vo.UserListVO;
import xyz.kuailemao.enums.*;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.UserRoleService;
import xyz.kuailemao.service.UserService;
import xyz.kuailemao.utils.HttpUtils;
import xyz.kuailemao.utils.IpUtils;
import xyz.kuailemao.utils.RedisCache;
import xyz.kuailemao.utils.SecurityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.beans.BeanUtils.copyProperties;
@Service
public class UserServiceimpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private RedisCache redisCache;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private TreeHoleMapper treeHoleMapper;

    @Resource
    private LeaveWordMapper leaveWordMapper;

    @Resource
    private ChatGptMapper chatGptMapper;

    @Resource
    private LinkMapper linkMapper;

    @Resource
    private IpService ipService;




    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        HttpServletRequest request = SecurityUtils.getCurrentHttpRequest();
        String equipmentHeader = null;
        String typeHeader = null;
        String accessToken = null;
        if (request != null) {
            equipmentHeader = request.getHeader(Const.TYPE_HEADER);
            typeHeader = request.getHeader(Const.FRONTEND_LOGIN_TYPE);
            accessToken = request.getHeader(Const.FRONTEND_THIRD_LOGIN_TOKEN);
        }
        User user = null;
        // 判断是否第三方登录
        if (typeHeader != null) {
            // getee
            if (typeHeader.equals(RegisterOrLoginTypeEnum.GITEE.getStrategy())) {
                String result = HttpUtils.sendGet(UrlEnum.GITEE_USER_INFO.getUrl(), "access_token=" + accessToken);
                JSONObject jsonObject = JSON.parseObject(result);
                Integer uuid = (Integer) jsonObject.get(SQLConst.ID);
                user = userMapper.selectById(uuid);
            }
            // github
            if (typeHeader.equals(RegisterOrLoginTypeEnum.GITHUB.getStrategy())) {
                OkHttpClient client = new OkHttpClient();
                Headers headers = new Headers.Builder()
                        .add(RequestHeaderEnum.GITHUB_USER_INFO.getHeader(), RequestHeaderEnum.GITHUB_USER_INFO.getContent())
                        .add(RespConst.TOKEN_HEADER, RespConst.TOKEN_PREFIX + accessToken)
                        .build();
                Request getRequest = new Request.Builder()
                        .url(UrlEnum.GITHUB_USER_INFO.getUrl())
                        .method(UrlEnum.GITHUB_USER_INFO.getMethod(), null)
                        .headers(headers)
                        .build();
                try (Response response = client.newCall(getRequest).execute()) {
                    JSONObject jsonObject;
                    if (response.body() != null) {
                        jsonObject = JSON.parseObject(response.body().string());
                        Integer uuid = (Integer) jsonObject.get(SQLConst.ID);
                        user = userMapper.selectById(uuid);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        } else {
            user = findAccountByNameOrEmail(username);
        }
        // 2. 判断用户是否存在
        if (ObjectUtils.isEmpty(user)) {
            // 不存在，抛出异常
            throw new UsernameNotFoundException(RespConst.USERNAME_OR_PASSWORD_ERROR_MSG);
        }
        return handlerLogin(user, equipmentHeader);
    }


    /**
     * 判断验证码是否正确
     * @param email
     * @param code
     * @param type
     * @return
     */
    private ResponseResult<Void> verifyCode(String email, String code, String type) {
        String redisCode = redisCache.getCacheObject(RedisConst.VERIFY_CODE + type + RedisConst.SEPARATOR + email);
        //redis存储的验证码过期或者失效
        if (redisCode == null)
            return ResponseResult.failure(RespEnum.VERIFY_CODE_ERROR.getCode(), RespConst.VERIFY_CODE_NULL_MSG);
        //验证码错误
        if (!redisCode.equals(code))
            return ResponseResult.failure(RespEnum.VERIFY_CODE_ERROR.getCode(), RespEnum.VERIFY_CODE_ERROR.getMsg());
        return null;
    }

    /**
     * 判断用户名和邮箱是否存在
     * @param username
     * @param email
     * @return
     */
    private boolean userIsExist(String username, String email) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username).or().eq(User::getEmail, email);
        return userMapper.selectOne(queryWrapper) != null;
    }

    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @Override
    public ResponseResult<Void> userRegister(UserRegisterDTO userRegisterDTO) {
        // 1.判断验证码是否正确
        ResponseResult<Void> verifyCode = verifyCode(userRegisterDTO.getEmail(), userRegisterDTO.getCode(), RedisConst.REGISTER);
        if (verifyCode != null) return verifyCode;

        // 2.判断用户名或邮箱是否已存在
        if (userIsExist(userRegisterDTO.getUsername(), userRegisterDTO.getEmail())) {
            return ResponseResult.failure(RespEnum.USERNAME_OR_EMAIL_EXIST.getCode(), RespEnum.USERNAME_OR_EMAIL_EXIST.getMsg());
        }
        // 3.密码加密
        String enPassword = passwordEncoder.encode(userRegisterDTO.getPassword());
        Date date = new Date();

        // 获取注册ip地址
        String ipAddr = IpUtils.getIpAddr(SecurityUtils.getCurrentHttpRequest());
        if (IpUtils.isUnknown(ipAddr)) {
            ipAddr = IpUtils.getHostIp();
        }
        // 4.保存用户信息
        User user = User.builder()
                .id(null)
                .nickname(userRegisterDTO.getUsername())
                .username(userRegisterDTO.getUsername())
                .password(enPassword)
                .registerType(RegisterOrLoginTypeEnum.EMAIL.getRegisterType())
                .registerIp(ipAddr)
                .gender(UserConst.DEFAULT_GENDER)
                .avatar(UserConst.DEFAULT_AVATAR)
                .intro(UserConst.DEFAULT_INTRODUCTION)
                .registerType(RegisterOrLoginTypeEnum.EMAIL.getRegisterType())
                .isDeleted(UserConst.DEFAULT_STATUS)
                .email(userRegisterDTO.getEmail())
                .loginTime(date).build();
        if (this.save(user)) {
            // 删除验证码
            ipService.refreshIpDetailAsyncByUidAndRegister(user.getId());
            redisCache.deleteObject(RedisConst.VERIFY_CODE + RedisConst.REGISTER + RedisConst.SEPARATOR + userRegisterDTO.getEmail());
            return ResponseResult.success();
        } else {
            return ResponseResult.failure();
        }
    }

    /**
     * 更新用户状态
     * @param updateRoleStatusDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateRoleStatus(UpdateRoleStatusDTO updateRoleStatusDTO) {
        User user = BeanUtil.copyProperties(updateRoleStatusDTO, User.class);
        if(this.baseMapper.updateById(user) > 0){
            return ResponseResult.success();
        }
        return ResponseResult.failure();


    }

    @Override
    public ResponseResult<List<UserListVO>> searchUser(UserSearchDTO userSearchDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if(userSearchDTO != null) {
            queryWrapper.like(Objects.nonNull(userSearchDTO.getUsername()), User::getUsername, userSearchDTO.getUsername())
                    .like(Objects.nonNull(userSearchDTO.getEmail()), User::getEmail, userSearchDTO.getEmail())
                    .eq(Objects.nonNull(userSearchDTO.getIsDisable()), User::getIsDisable, userSearchDTO.getIsDisable());
            if (userSearchDTO.getCreateTimeStart() != null) {
                queryWrapper.ge(User::getCreateTime, userSearchDTO.getCreateTimeStart());
            }
            if (userSearchDTO.getCreateTimeEnd() != null) {
                queryWrapper.le(User::getCreateTime, userSearchDTO.getCreateTimeEnd());
            }
        }
        List<User> userList = this.baseMapper.selectList(queryWrapper);
        List<UserListVO> userListVOList = userList.stream().map(user -> BeanUtil.copyProperties(user, UserListVO.class)).collect(Collectors.toList());
        return ResponseResult.success(userListVOList);
    }


    /**
     * 重置密码
     * @param userResetPasswordDTO
     * @return
     */
    @Override
    public ResponseResult<Void> resetPassword(UserResetPasswordDTO userResetPasswordDTO) {
        //判断验证码是否正确
        ResponseResult<Void> verifyCode = verifyCode(userResetPasswordDTO.getEmail(), userResetPasswordDTO.getCode(), RedisConst.RESET);
        if (verifyCode != null) return verifyCode;

        String password = passwordEncoder.encode(userResetPasswordDTO.getPassword());
        User user = User.builder().password(password).build();

        if(this.update(user,new LambdaQueryWrapper<User>().eq(User::getEmail,userResetPasswordDTO.getEmail()))){
            return  ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Autowired
    private FileUploadUtils fileUploadUtils;

    /**
     * 用户头像上传
     * @param avatarFile
     * @return
     */
    @Override
    public ResponseResult<String> uploadAvatar(MultipartFile avatarFile) throws Exception {
        //返回文件上传后的路径
        String upload = fileUploadUtils.upload(UploadEnum.USER_AVATAR, avatarFile);
        return ResponseResult.success(upload);
    }

    /**
     * 修改用户信息
     * @param userUpdateDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateUser(UserUpdateDTO userUpdateDTO) {
        Long userId = SecurityUtils.getUserId();
        User user = BeanUtil.copyProperties(userUpdateDTO, User.class);
        user.setId(userId);
        User isUser = this.baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getNickname, user.getNickname()));
        if(isUser != null){
            return ResponseResult.failure("用户名不能重复");
        }
        if(this.updateById(user)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();

    }

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;


    /**
     * 修改用户绑定的邮箱
     * @param updateEmailDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateEmailAndVerify(UpdateEmailDTO updateEmailDTO) {
        //验证邮箱是否已经注册
        User isUser = this.baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, updateEmailDTO.getEmail()));
        if(isUser != null){
            return ResponseResult.failure("该邮箱已经被注册");
        }
        //验证密码是否正确
        User user = this.userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId,SecurityUtils.getUserId()));
        if (bCryptPasswordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())){
            return ResponseResult.failure("密码错误");
        }
        //验证验证码是否正确
        ResponseResult<Void> veryCode = verifyCode(updateEmailDTO.getEmail(), updateEmailDTO.getCode(),  RedisConst.RESET_EMAIL);
        if (veryCode != null) return veryCode;
        //修改邮箱
        user.setEmail(updateEmailDTO.getEmail());
        if(this.updateById(user)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 获取用户详细信息
     * @param id
     * @return
     */
    @Override
    public ResponseResult<UserDetailsVO> getUserInfo(Long id) {
        User user = this.baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, id));
        if(user!=null){
            UserDetailsVO userDetailsVO = BeanUtil.copyProperties(user, UserDetailsVO.class);
            //获取用户的角色
            List<UserRole> userRoleList = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId,id));
            List<Long> roleIds = userRoleList.stream().map(userRole -> userRole.getRoleId()).collect(Collectors.toList());
            List<Role> roleList = roleMapper.selectList(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds));
            userDetailsVO.setRoles(roleList.stream().map(role -> role.getRoleName()).collect(Collectors.toList()));
            return ResponseResult.success(userDetailsVO);
        }
        return null;



    }


    /**
     * 获取当前登录的用户信息
     * @param userId
     * @return
     */
    @Override
    public ResponseResult<UserAccountVO> findAccountById(Long userId) {
        User user = this.baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
        UserAccountVO userAccountVO = BeanUtil.copyProperties(user, UserAccountVO.class);
        //获取用户的角色及权限列表
        List<String> userRoles = SecurityUtils.getUserRoles();
        // 角色
        List<String> roles = new ArrayList<>();
        // 权限
        List<String> permissions = new ArrayList<>();
        userRoles.forEach(role -> {
            if (role.startsWith(SecurityConst.ROLE_PREFIX)) {
                // 去掉前缀，添加
                roles.add(role.substring(SecurityConst.ROLE_PREFIX.length()));
            } else {
                permissions.add(role);
            }
        });
        userAccountVO.setRoles(roles);
        userAccountVO.setPermissions(permissions);
        return ResponseResult.success(userAccountVO);
    }


    /**
     * 删除用户信息
     * @param userDeleteDTO
     * @return
     */
    @Override
    public ResponseResult<Void> deleteUser(UserDeleteDTO userDeleteDTO) {
        // 删除用户
        List<Long> ids = userDeleteDTO.getIds();
        if (removeBatchByIds(ids)) {
            // 删除用户角色关系
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, ids));
            // 删除用户评论、点赞、收藏
            commentMapper.delete(new LambdaQueryWrapper<Comment>().in(Comment::getCommentUserId, ids).or(a -> a.in(Comment::getReplyUserId, ids)));
            likeMapper.delete(new LambdaQueryWrapper<Like>().in(Like::getUserId, ids));
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().in(Favorite::getUserId, ids));
            // 删除用户文章
            articleMapper.delete(new LambdaQueryWrapper<Article>().in(Article::getUserId, ids));
            // 删除用户树洞
            treeHoleMapper.delete(new LambdaQueryWrapper<TreeHole>().in(TreeHole::getUserId, ids));
            // 删除用户留言
            leaveWordMapper.delete(new LambdaQueryWrapper<LeaveWord>().in(LeaveWord::getUserId, ids));
            // 删除用户聊天记录
            chatGptMapper.delete(new LambdaQueryWrapper<ChatGpt>().in(ChatGpt::getUserId, ids));
            // 删除用户友链
            linkMapper.delete(new LambdaQueryWrapper<Link>().in(Link::getUserId, ids));
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    public LoginUser handlerLogin(User user, String equipmentHeader) {
        HttpServletRequest request = SecurityUtils.getCurrentHttpRequest();
        String header = null;
        if (request != null) {
            header = request.getHeader(Const.TYPE_HEADER);
        }
        // 查询用户角色
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<Role> roles = userRoles.stream().map(role -> roleMapper.selectById(role.getRoleId())).filter(role -> Objects.equals(role.getStatus(), RoleEnum.Role_STATUS_ARTICLE.getStatus())).toList();
        // 用户是否被禁用
        if (user.getIsDisable() == 1) {
            throw new BadCredentialsException(RespConst.ACCOUNT_DISABLED_MSG);
        }
        // 是否测试账号前台
        if (header == null || (roles.stream().anyMatch(role -> role.getRoleKey().equals(SecurityConst.ROLE_TESTER)) && !header.equals(Const.BACKEND_REQUEST))) {
            throw new BadCredentialsException(RespConst.TEST_ACCOUNT_MSG);
        }

        // 判断用户是否具备任何权限,
        if ((equipmentHeader != null && equipmentHeader.equals(Const.BACKEND_REQUEST) && ObjectUtils.isEmpty(roles))) {
            throw new BadCredentialsException(RespConst.NO_PERMISSION_MSG);
        }
        if (!roles.isEmpty()) {
            // 查询权限关系表
            List<RolePermission> rolePermissions = rolePermissionMapper.selectBatchIds(roles.stream().map(Role::getId).toList());
            // 查询角色权限
            List<Long> pIds = rolePermissions.stream().map(RolePermission::getPermissionId).toList();
            List<Permission> permissions = permissionMapper.selectBatchIds(pIds);
            // 组合角色，权限
            List<String> list = permissions.stream().map(Permission::getPermissionKey).collect(Collectors.toList());
            roles.forEach(role -> list.add(SecurityConst.ROLE_PREFIX + role.getRoleKey()));
            return new LoginUser(user, list);
        }
        return new LoginUser(user, List.of());
    }


    @Override
    public void userLoginStatus(Long id, Integer type) {
        // ip地址
        String ipAddr = IpUtils.getIpAddr(SecurityUtils.getCurrentHttpRequest());
        if (IpUtils.isUnknown(ipAddr)) {
            ipAddr = IpUtils.getHostIp();
        }
        User user = User.builder()
                .id(id)
                .loginTime(new Date())
                .loginType(type)
                .loginIp(ipAddr)
                .build();
        if (updateById(user)) {
            ipService.refreshIpDetailAsyncByUidAndLogin(user.getId());
        }

    }



    public User findAccountByNameOrEmail(String text) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, text).or().eq(User::getEmail, text).eq(User::getRegisterType, RegisterOrLoginTypeEnum.EMAIL.getRegisterType());
        return userMapper.selectOne(wrapper);
    }











}
