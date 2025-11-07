package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 记录用户首充奖励
 */
@Entity
@Table(name = "users_first_recharge", indexes = {
        @Index(columnList = "user_id", unique = true),
        @Index(columnList = "recharge_total"),
        @Index(columnList = "created_at")
})
@Setter
@Getter
public class UserFirstRecharge implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "recharge_total", precision = 18, scale = 2)
    private BigDecimal rechargeTotal;

    private Date createdAt;

    @PrePersist
    public void init(){
        this.createdAt = new Date();
    }
}
