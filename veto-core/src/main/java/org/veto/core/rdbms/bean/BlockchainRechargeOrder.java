package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "blockchain_recharge_orders", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "address"),
        @Index(columnList = "txid", unique = true),
        @Index(columnList = "amount"),
        @Index(columnList = "status")
})
public class BlockchainRechargeOrder implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String address;

    private String txid;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private Date createdAt;

    private Boolean status;

    @Version
    private Long version;
}
