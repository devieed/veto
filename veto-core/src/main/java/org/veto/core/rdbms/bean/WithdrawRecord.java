package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "widthraw_records", indexes = {
        @Index(columnList = "operation"),
        @Index(columnList = "amount"),
        @Index(columnList = "status"),
        @Index(columnList = "created_at"),
        @Index(columnList = "address"),
        @Index(columnList = "fee")
})
/**
 * 提币记录
 */
@Setter
@Getter
public class WithdrawRecord implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operationId;

    private OPERATION operation;

    private Boolean status;

    private Date createdAt;

    private String address;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private Boolean isBlockchain;

    @Column(precision = 18, scale = 2)
    private BigDecimal fee;

    @PrePersist
    public void prePersist(){
        this.createdAt = new Date();
    }

    public enum OPERATION{
        SYSTEM,
        USER,
        ADMIN
    }
}
