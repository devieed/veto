package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户的收益
 */
@Entity
@Table(name = "user_revenues", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "`type`"),
        @Index(columnList = "user_id,type,coin_type", unique = true),
        @Index(columnList = "updated_at"),
        @Index(columnList = "total"),
        @Index(columnList = "coin_type")
})
@Setter
@Getter
public class UserRevenue implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private TYPE type;

    @Enumerated(EnumType.ORDINAL)
    private COIN_TYPE coinType;

    private Date updatedAt;

    @Column(precision =  18, scale = 2)
    private BigDecimal total;

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    public enum TYPE {
        BET, // 投注
        RECOMMEND,
        INTEREST, // 每天的利息
        FIRST_RECHARGE_BONUS, // 首次充值
    }
}
