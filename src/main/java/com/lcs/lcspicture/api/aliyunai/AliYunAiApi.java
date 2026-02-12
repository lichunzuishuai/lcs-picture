package com.lcs.lcspicture.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.lcs.lcspicture.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.lcs.lcspicture.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lcs.lcspicture.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.lcs.lcspicture.exception.BusinessException;
import com.lcs.lcspicture.exception.ErrorCode;
import com.lcs.lcspicture.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    //创建任务获取任务ID
    private static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    //获取任务结果
    private static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    //创建扩图任务
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        //参数校验
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);
        //创建任务
        try (HttpResponse execute = HttpUtil.createPost(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                //允许异步
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest))
                .execute()) {
            if (!execute.isOk()) {
                log.error("创建任务失败");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务失败");
            }
            //解析结果
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(execute.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("Ai扩图失败：errorCode{},errorMessage{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图接口响应异常");
            }
            return response;
        }

    }

    //查询任务状态
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        try (HttpResponse execute = HttpUtil.createGet(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .execute();) {
            //判断响应状态
            if (!execute.isOk()) {
                log.error("查询任务状态失败");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询任务状态失败");
            }
            return JSONUtil.toBean(execute.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
