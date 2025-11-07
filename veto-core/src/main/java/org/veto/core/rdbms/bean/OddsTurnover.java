package org.veto.core.rdbms.bean;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;
/**
 * 成交额度表
 */
@Setter
@Getter
@Entity
@Table(name = "odds_turnover", indexes = {
        @Index(columnList = "contest_id"),
        @Index(columnList = "odds_id", unique = true),
        @Index(columnList = "turnover"),
})
public class OddsTurnover implements VetoBean{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contestId;
    
    private Long oddsId;
    
    // 成交额度
    @Column(precision = 18, scale = 2)
    private BigDecimal turnover;

    private Date createdAt;

    private Date updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
    
}
