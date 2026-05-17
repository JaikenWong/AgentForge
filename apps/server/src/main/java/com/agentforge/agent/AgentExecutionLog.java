package com.agentforge.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_execution_logs")
public class AgentExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("agent_id")
    private Long agentId;

    @TableField("user_id")
    private Long userId;

    @TableField("tenant_id")
    private Long tenantId;

    private String channel;

    @TableField("input_text")
    private String inputText;

    @TableField("output_text")
    private String outputText;

    private String status;

    @TableField("latency_ms")
    private Integer latencyMs;

    @TableField("error_message")
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
