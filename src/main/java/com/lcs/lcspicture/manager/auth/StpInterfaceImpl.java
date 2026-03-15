package com.lcs.lcspicture.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.lcs.lcspicture.constant.UserConstant;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.manager.auth.model.SpaceUserPermissionConstant;
import com.lcs.lcspicture.model.entity.Picture;
import com.lcs.lcspicture.model.entity.Space;
import com.lcs.lcspicture.model.entity.SpaceUser;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.model.enums.SpaceRoleEnum;
import com.lcs.lcspicture.model.enums.SpaceTypeEnum;
import com.lcs.lcspicture.service.PictureService;
import com.lcs.lcspicture.service.SpaceService;
import com.lcs.lcspicture.service.SpaceUserService;
import com.lcs.lcspicture.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    //默认是/api
    @Value("${server.servlet.context-path}")
    private String contextPath;


    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //只针对空间用户
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        //管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        //获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        if (isAllFiledsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        //校验登录状态
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        //从上下文中获取SpaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        //通过spaceUserId获取空间用户信息
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间用户不存在");
            }
            //取出当前登录用户性对应的SpaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        //通过spaceId或pictureId获取空间或图片信息
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片不存在");
            }
            spaceId = picture.getSpaceId();
            //公共图库，仅本人和管理员才可以操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) && userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    //不是自己的 图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间不存在");
        }
        //根据spaceType 判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            //私有空间，仅本人和管理员才可以操作
            if (space.getUserId().equals(userId) && userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        }
        //团队空间
        spaceUser = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();
        if (spaceUser == null) {
            return new ArrayList<>();
        }
        return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
    }

    private boolean isAllFiledsNull(Object obj) {
        if (obj == null) {
            return true;//对象为null
        }
        return Arrays.stream(ReflectUtil.getFields(obj.getClass()))
                //获取字段值
                .map(filed -> ReflectUtil.getFieldValue(obj, filed))
                //判断所有字段是否为空
                .allMatch(ObjectUtil::isEmpty);
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {

        return new ArrayList<>();
    }

    /**
     * 根据登录id获取用户信息
     *
     * @return
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext bean;
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            bean = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            bean = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        //根据请求路径区分ID字段的含义
        Long id = bean.getId();

        if (ObjUtil.isNotEmpty(id)) {
            //获取请求路径的业务前缀:/api/picture/aaa?a=1
            String requestURI = request.getRequestURI();
            // 去除业务前缀picture/aaa?a=1
            String replace = requestURI.replace(contextPath + "/", "");
            // 获取业务前缀picture
            String businessPrefix = StrUtil.subBefore(replace, "/", false);
            switch (businessPrefix) {
                case "picture":
                    bean.setPictureId(id);
                    break;
                case "space":
                    bean.setSpaceId(id);
                    break;
                case "spaceUser":
                    bean.setSpaceUserId(id);
                    break;
                default:
                    break;
            }
        }
        return bean;
    }

}
