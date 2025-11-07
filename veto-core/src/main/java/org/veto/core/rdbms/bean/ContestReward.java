package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 记录某场比赛现在的状态，记录是否已发放奖励
 */
@Setter
@Getter
@Table(name = "contest_rewards", indexes = {
        @Index(columnList = "contest_id", unique = true),
        @Index(columnList = "status"),
        @Index(columnList = "updated_at"),
        @Index(columnList = "reward_total")
})
@Entity
public class ContestReward implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id")
    private Long contestId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", referencedColumnName = "id", updatable = false, insertable = false)
    private Contest contest;
    // 奖励发放总数
    @Column(name = "reward_total")
    private BigDecimal rewardTotal;

    @Enumerated(EnumType.ORDINAL)
    private STATUS status;

    private Date updatedAt;
    // 如果
    @Column(name = "failed_msg")
    private String failedMsg;

    @PrePersist
    @PreUpdate
    public void prePersistAndPreUpdate() {
        this.updatedAt = new Date();
    }

    public enum STATUS {
        NOT_STARTED,// 未开始
        DISTRIBUTING, //正在发放
        SUCCESS, // 发放成功
        FAILED // 发放失败
    }
}
