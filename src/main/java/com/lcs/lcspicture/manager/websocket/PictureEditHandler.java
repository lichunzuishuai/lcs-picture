package com.lcs.lcspicture.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lcs.lcspicture.manager.websocket.disruptor.PictureEditEventProducer;
import com.lcs.lcspicture.manager.websocket.model.PictureEditActionEnum;
import com.lcs.lcspicture.manager.websocket.model.PictureEditMessageTypeEnum;
import com.lcs.lcspicture.manager.websocket.model.PictureEditRequestMessage;
import com.lcs.lcspicture.manager.websocket.model.PictureEditResponseMessage;
import com.lcs.lcspicture.model.entity.User;
import com.lcs.lcspicture.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑 WebSocket 处理器
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;
    @Resource
    PictureEditEventProducer pictureEditEventProducer;
    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 连接建立成功
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        //保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 不能覆盖已有会话集合，否则会导致只有“最后一个连接”能收到广播
        pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // 如果当前图片已有人在编辑，将编辑状态同步给新加入者（避免提示不一致）
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null) {
            User editingUser = userService.getById(editingUserId);
            if (editingUser != null) {
                PictureEditResponseMessage enterEditMessage = new PictureEditResponseMessage();
                enterEditMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
                enterEditMessage.setMessage(String.format(" %s 正在编辑图片", editingUser.getUserName()));
                enterEditMessage.setUser(userService.getUserVO(editingUser));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(enterEditMessage)));
            }
        }
        //这样写如果已经又人在编辑图片了那么后面进来的人不知道当前是谁在编辑图片
        /*pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);*/
        //构造响应
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        responseMessage.setMessage(String.format(" %s 加入编辑", user.getUserName()));
        responseMessage.setUser(userService.getUserVO(user));
        //广播给同一张图片的用户,除了自己
        broadcastToPicture(pictureId, responseMessage);
    }

    /**
     * 接收前端发送的消息，根据消息类别处理消息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        //根据消息类型处理消息（生产消息到 Disruptor 环形队列中）
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
        // 调用对应的消息处理方法
        /*switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }*/
    }

    /**
     * 处理进入编辑的消息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //没有用户正在编辑才可以进入编辑
        //  使用原子操作
        Long previousUser = pictureEditingUsers.putIfAbsent(pictureId, user.getId());
        if (previousUser == null) {
            // 成功进入编辑，广播给所有人
            //构造响应
            PictureEditResponseMessage successMessage = new PictureEditResponseMessage();
            successMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            successMessage.setMessage(String.format(" %s 开始编辑图片", user.getUserName()));
            successMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, successMessage);
        } else {
            //  已被抢占，只告诉自己
            PictureEditResponseMessage errorMessage = new PictureEditResponseMessage();
            errorMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            errorMessage.setMessage("已有其他用户正在编辑，请稍后再试");
            errorMessage.setUser(userService.getUserVO(user));
            //  只发送给当前用户
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(errorMessage)));
        }
    }

    /**
     * 处理编辑动作的消息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        //确认当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setMessage(String.format(" %s 执行了 %s", user.getUserName(), actionEnum.getText()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setEditAction(editAction);
            //广播给图片的所有用户除了自己
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理退出编辑的消息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            pictureEditingUsers.remove(pictureId);
            //构造响应
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format(" %s 退出了编辑", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 断开连接
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        //移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        //删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        //响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("%s 退出了编辑", user.getUserName()));
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }


    /**
     * 广播给图片的所有用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            //创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            //配置序列化：将Long类型转为String，解决精度丢失问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);//支持long基本类型
            objectMapper.registerModule(module);
            //序列化为 JSON
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                if (excludeSession != null && session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播给图片的所有用户(支持排除掉某个Session)
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

}
