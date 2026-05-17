package com.agentforge.runtime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_channels")
public class AgentChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("agent_id")
    private Long agentId;

    @TableField("tenant_id")
    private Long tenantId;

    private String channel;

    @TableField("chat_id")
    private String chatId;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
