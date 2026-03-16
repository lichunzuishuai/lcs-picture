package com.lcs.lcspicture.manager.websocket.disruptor;

import com.lcs.lcspicture.manager.websocket.model.PictureEditRequestMessage;
import com.lcs.lcspicture.model.entity.User;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 消息生产者
 */
@Component
public class PictureEditEventProducer {
    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        // 获取ringBuffer
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        //找到下一个可用的sequence
        long sequence = ringBuffer.next();
        PictureEditEvent pictureEditEvent = ringBuffer.get(sequence);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        //发布事件
        ringBuffer.publish(sequence);
    }

    //优雅停机
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}
